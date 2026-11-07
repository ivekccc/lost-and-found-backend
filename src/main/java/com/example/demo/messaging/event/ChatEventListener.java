package com.example.demo.messaging.event;

import com.example.demo.messaging.dto.ChatEventDto;
import com.example.demo.messaging.dto.MessageDto;
import com.example.demo.messaging.model.ChatEventType;
import com.example.demo.messaging.model.MessageType;
import com.example.demo.messaging.service.ChatPresenceRegistry;
import com.example.demo.notification.NotificationFacade;
import com.example.demo.notification.model.NotificationType;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatEventListener {

    public static final String USER_QUEUE = "/queue/chat";
    private static final int PUSH_PREVIEW_LENGTH = 100;

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatPresenceRegistry presenceRegistry;
    private final NotificationFacade notificationFacade;

    @TransactionalEventListener
    public void onMessageSent(MessageSentEvent event) {
        ChatEventDto frame = new ChatEventDto(
                ChatEventType.MESSAGE, event.getMessage().getConversationId(), event.getMessage(), null, null);
        messagingTemplate.convertAndSendToUser(event.getRecipientEmail(), USER_QUEUE, frame);
        messagingTemplate.convertAndSendToUser(event.getSenderEmail(), USER_QUEUE, frame);
    }

    @Async
    @TransactionalEventListener
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void notifyRecipient(MessageSentEvent event) {
        MessageDto message = event.getMessage();
        String dataJson = "{\"conversationId\":" + message.getConversationId() + "}";
        if (message.getType() == MessageType.THANKS) {
            notifyThanks(event, dataJson);
            return;
        }
        if (presenceRegistry.isOnline(event.getRecipientEmail())) {
            return;
        }
        switch (message.getType()) {
            case TEXT -> notificationFacade.pushOnly(event.getRecipientId(), NotificationType.CHAT_MESSAGE,
                    "notification.chatMessage.title", "notification.chatMessage.body", dataJson,
                    event.getSenderName(), preview(message.getBody()));
            case MEETING_POINT -> notificationFacade.pushOnly(event.getRecipientId(), NotificationType.CHAT_MESSAGE,
                    "notification.chatMessage.title", "notification.chatMeetingPoint.body", dataJson,
                    event.getSenderName(), message.getPlaceLabel());
            case CONTACT -> notificationFacade.pushOnly(event.getRecipientId(), NotificationType.CHAT_MESSAGE,
                    "notification.chatMessage.title", "notification.chatContact.body", dataJson,
                    event.getSenderName());
        }
    }

    private void notifyThanks(MessageSentEvent event, String dataJson) {
        String note = event.getMessage().getBody();
        if (note == null) {
            notificationFacade.notify(event.getRecipientId(), NotificationType.THANKS_RECEIVED,
                    "notification.thanksReceived.title", "notification.thanksReceived.body", dataJson,
                    event.getSenderName());
        } else {
            notificationFacade.notify(event.getRecipientId(), NotificationType.THANKS_RECEIVED,
                    "notification.thanksReceived.title", "notification.thanksReceivedWithNote.body", dataJson,
                    event.getSenderName(), preview(note));
        }
    }

    @TransactionalEventListener
    public void onConversationRead(ConversationReadEvent event) {
        messagingTemplate.convertAndSendToUser(event.getOtherEmail(), USER_QUEUE, new ChatEventDto(
                ChatEventType.READ, event.getConversationId(), null, event.getReaderId(), event.getReadAt()));
    }

    private String preview(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > PUSH_PREVIEW_LENGTH ? body.substring(0, PUSH_PREVIEW_LENGTH) + "…" : body;
    }
}
