package com.example.demo.messaging.service;

import com.example.demo.location.LocationFacade;
import com.example.demo.messaging.MessagingFacade;
import com.example.demo.messaging.dto.ConversationDetailsDto;
import com.example.demo.messaging.dto.ConversationDto;
import com.example.demo.messaging.dto.ConversationUnreadCountDto;
import com.example.demo.messaging.dto.MessageDto;
import com.example.demo.messaging.dto.MessagePageDto;
import com.example.demo.messaging.dto.SendMessageRequestDto;
import com.example.demo.messaging.event.ConversationReadEvent;
import com.example.demo.messaging.event.MessageSentEvent;
import com.example.demo.messaging.model.Conversation;
import com.example.demo.messaging.model.Message;
import com.example.demo.messaging.model.MessageType;
import com.example.demo.messaging.repository.ConversationReportView;
import com.example.demo.messaging.repository.ConversationRepository;
import com.example.demo.messaging.repository.MessageRepository;
import com.example.demo.report.model.ReportType;
import com.example.demo.shared.exception.InvalidChatMessageException;
import com.example.demo.shared.exception.RateLimitExceededException;
import com.example.demo.shared.exception.ResourceNotFoundException;
import com.example.demo.user.UserFacade;
import com.example.demo.user.model.User;
import com.example.demo.user.model.UserStatus;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessagingService {

    private static final int DEFAULT_PAGE_SIZE = 30;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_MESSAGES_PER_MINUTE = 20;
    private static final long RATE_LIMIT_WINDOW_SECONDS = 60;
    private static final int EXCERPT_LENGTH = 120;
    private static final int COORDINATE_SCALE = 8;

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final UserFacade userFacade;
    private final LocationFacade locationFacade;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void openForClaim(Long claimId, Long reportId, Long holderId, Long claimantId) {
        if (conversationRepository.existsByClaimId(claimId)) {
            return;
        }
        Conversation conversation = new Conversation();
        conversation.setClaimId(claimId);
        conversation.setReportId(reportId);
        conversation.setHolder(userFacade.requireById(holderId));
        conversation.setClaimant(userFacade.requireById(claimantId));
        conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public Optional<Long> findConversationIdForClaim(Long claimId) {
        return conversationRepository.findByClaimId(claimId).map(Conversation::getId);
    }

    @Transactional(readOnly = true)
    public List<ConversationDto> getConversations(String userEmail) {
        User viewer = userFacade.requireByEmail(userEmail);
        List<Conversation> conversations = conversationRepository.findAllOfUser(viewer.getId());
        if (conversations.isEmpty()) {
            return List.of();
        }
        Map<Long, Message> lastMessages = messageRepository
                .findLastOfConversations(conversations.stream().map(Conversation::getId).toList())
                .stream()
                .collect(Collectors.toMap(message -> message.getConversation().getId(), Function.identity()));

        return conversations.stream()
                .map(conversation -> toDto(conversation, viewer, lastMessages.get(conversation.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationUnreadCountDto getUnreadCount(String userEmail) {
        User viewer = userFacade.requireByEmail(userEmail);
        long count = conversationRepository.findAllOfUser(viewer.getId()).stream()
                .filter(conversation -> unreadCount(conversation, viewer.getId()) > 0)
                .count();
        return new ConversationUnreadCountDto(count);
    }

    @Transactional(readOnly = true)
    public ConversationDetailsDto getConversation(Long conversationId, String userEmail) {
        User viewer = userFacade.requireByEmail(userEmail);
        Conversation conversation = requireOwn(conversationId, viewer);
        User other = conversation.otherParticipant(viewer.getId());
        ConversationReportView report = reportViewOf(conversation);
        return new ConversationDetailsDto(
                conversation.getId(),
                other.getId(),
                displayName(other),
                other.getAvatarUrl(),
                conversation.getReportId(),
                report.getTitle(),
                ReportType.valueOf(report.getType()),
                conversation.roleOf(viewer.getId()),
                conversation.readAtOf(other.getId()),
                conversation.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public MessagePageDto getMessages(Long conversationId, String userEmail, Long beforeId, Integer size) {
        User viewer = userFacade.requireByEmail(userEmail);
        Conversation conversation = requireOwn(conversationId, viewer);
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest page = PageRequest.of(0, pageSize + 1);
        List<Message> messages = beforeId == null
                ? messageRepository.findLatest(conversation.getId(), page)
                : messageRepository.findBefore(conversation.getId(), beforeId, page);
        boolean hasMore = messages.size() > pageSize;
        List<MessageDto> dtos = messages.stream()
                .limit(pageSize)
                .map(message -> toDto(message, null))
                .toList();
        return new MessagePageDto(dtos, hasMore);
    }

    @Transactional
    public MessageDto sendMessage(Long conversationId, String userEmail, SendMessageRequestDto request) {
        User sender = requireSender(userEmail);
        Conversation conversation = requireOwn(conversationId, sender);

        Message message = newMessage(conversation, sender, request.getType());
        if (request.getType() == MessageType.TEXT) {
            String body = request.getBody() == null ? "" : request.getBody().trim();
            if (body.isEmpty()) {
                throw new InvalidChatMessageException("chat.emptyMessage");
            }
            message.setBody(body);
        } else if (request.getType() == MessageType.MEETING_POINT) {
            if (request.getLatitude() == null || request.getLongitude() == null) {
                throw new InvalidChatMessageException("chat.meetingPointRequired");
            }
            String label = locationFacade.describeMeetingPoint(
                            request.getLatitude(), request.getLongitude(), reportViewOf(conversation).getCityId())
                    .orElseThrow(() -> new InvalidChatMessageException("chat.meetingPointOutsideCity"));
            message.setLatitude(request.getLatitude().setScale(COORDINATE_SCALE, RoundingMode.HALF_UP));
            message.setLongitude(request.getLongitude().setScale(COORDINATE_SCALE, RoundingMode.HALF_UP));
            message.setPlaceLabel(label);
        } else {
            throw new InvalidChatMessageException("chat.useContactEndpoint");
        }

        return deliver(conversation, sender, message, request.getClientId());
    }

    @Transactional
    public MessageDto shareContact(Long conversationId, String userEmail) {
        User sender = requireSender(userEmail);
        Conversation conversation = requireOwn(conversationId, sender);
        ConversationReportView report = reportViewOf(conversation);

        Message message = newMessage(conversation, sender, MessageType.CONTACT);
        message.setContactName(displayName(sender));
        boolean ownsReport = sender.getId().equals(report.getOwnerId());
        if (ownsReport && (hasText(report.getContactEmail()) || hasText(report.getContactPhone()))) {
            message.setContactEmail(hasText(report.getContactEmail()) ? report.getContactEmail() : sender.getEmail());
            message.setContactPhone(report.getContactPhone());
        } else {
            message.setContactEmail(sender.getEmail());
            message.setContactPhone(sender.getPhoneNumber());
        }

        return deliver(conversation, sender, message, null);
    }

    @Transactional
    public void markRead(Long conversationId, String userEmail) {
        User reader = userFacade.requireByEmail(userEmail);
        Conversation conversation = requireOwn(conversationId, reader);
        LocalDateTime now = LocalDateTime.now();
        conversation.markReadBy(reader.getId(), now);
        conversationRepository.save(conversation);
        eventPublisher.publishEvent(new ConversationReadEvent(
                this, conversation.getId(), reader.getId(), now,
                conversation.otherParticipant(reader.getId()).getEmail()));
    }

    @Transactional(readOnly = true)
    public Optional<TypingTarget> typingTarget(Long conversationId, String userEmail) {
        User typist = userFacade.requireByEmail(userEmail);
        return conversationRepository.findOfParticipant(conversationId, typist.getId())
                .map(conversation -> new TypingTarget(
                        typist.getId(), conversation.otherParticipant(typist.getId()).getEmail()));
    }

    @Transactional
    public void deleteConversationsOfUser(Long userId) {
        conversationRepository.deleteAllOfUser(userId);
    }

    @Transactional(readOnly = true)
    public Optional<MessagingFacade.ReportableMessage> findReportableMessage(Long messageId, Long reporterId) {
        return messageRepository.findWithConversation(messageId)
                .filter(message -> message.getConversation().hasParticipant(reporterId))
                .filter(message -> !message.getSender().getId().equals(reporterId))
                .map(this::toReportable);
    }

    @Transactional(readOnly = true)
    public Optional<MessagingFacade.ReportableMessage> describeMessage(Long messageId) {
        return messageRepository.findWithConversation(messageId).map(this::toReportable);
    }

    @Transactional(readOnly = true)
    public List<MessageDto> getAllMessagesForModeration(Long conversationId) {
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("chat.notFound"));
        return messageRepository.findAllOfConversation(conversation.getId()).stream()
                .map(message -> toDto(message, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public Conversation requireForModeration(Long conversationId) {
        return conversationRepository.findWithParticipants(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("chat.notFound"));
    }

    @Transactional(readOnly = true)
    public String reportTitleOf(Conversation conversation) {
        return reportViewOf(conversation).getTitle();
    }

    public String displayName(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String combined = (firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName);
        String trimmed = combined.trim();
        return trimmed.isEmpty() ? user.getUsername() : trimmed;
    }

    private MessageDto deliver(Conversation conversation, User sender, Message message, String clientId) {
        Message saved = messageRepository.save(message);
        conversation.setLastMessageAt(saved.getCreatedAt());
        conversation.markReadBy(sender.getId(), saved.getCreatedAt());
        conversationRepository.save(conversation);

        MessageDto dto = toDto(saved, clientId);
        User recipient = conversation.otherParticipant(sender.getId());
        eventPublisher.publishEvent(new MessageSentEvent(
                this, dto, sender.getEmail(), displayName(sender), recipient.getId(), recipient.getEmail()));
        return dto;
    }

    private Message newMessage(Conversation conversation, User sender, MessageType type) {
        long recent = messageRepository.countBySenderIdAndCreatedAtAfter(
                sender.getId(), LocalDateTime.now().minusSeconds(RATE_LIMIT_WINDOW_SECONDS));
        if (recent >= MAX_MESSAGES_PER_MINUTE) {
            throw new RateLimitExceededException("chat.rateLimit", RATE_LIMIT_WINDOW_SECONDS);
        }
        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setType(type);
        return message;
    }

    private User requireSender(String userEmail) {
        User sender = userFacade.requireByEmail(userEmail);
        if (sender.getStatus() != UserStatus.ACTIVE && sender.getStatus() != UserStatus.PARTIALLY_BLOCKED) {
            throw new InvalidChatMessageException("chat.cannotSend");
        }
        return sender;
    }

    private Conversation requireOwn(Long conversationId, User user) {
        return conversationRepository.findOfParticipant(conversationId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("chat.notFound"));
    }

    private ConversationReportView reportViewOf(Conversation conversation) {
        return conversationRepository.findReportView(conversation.getReportId())
                .orElseThrow(() -> new ResourceNotFoundException("chat.notFound"));
    }

    private int unreadCount(Conversation conversation, Long viewerId) {
        LocalDateTime readAt = conversation.readAtOf(viewerId);
        long count = readAt == null
                ? messageRepository.countFromOthers(conversation.getId(), viewerId)
                : messageRepository.countFromOthersSince(conversation.getId(), viewerId, readAt);
        return (int) count;
    }

    private ConversationDto toDto(Conversation conversation, User viewer, Message lastMessage) {
        User other = conversation.otherParticipant(viewer.getId());
        ConversationReportView report = reportViewOf(conversation);
        return new ConversationDto(
                conversation.getId(),
                other.getId(),
                displayName(other),
                other.getAvatarUrl(),
                conversation.getReportId(),
                report.getTitle(),
                ReportType.valueOf(report.getType()),
                lastMessage == null ? null : lastMessage.getType(),
                lastMessage == null ? null : lastMessage.getBody(),
                lastMessage == null ? null : lastMessage.getSender().getId().equals(viewer.getId()),
                conversation.getLastMessageAt(),
                unreadCount(conversation, viewer.getId()),
                conversation.getCreatedAt());
    }

    private MessageDto toDto(Message message, String clientId) {
        return new MessageDto(
                message.getId(),
                message.getConversation().getId(),
                message.getSender().getId(),
                message.getType(),
                message.getBody(),
                message.getLatitude(),
                message.getLongitude(),
                message.getPlaceLabel(),
                message.getContactName(),
                message.getContactEmail(),
                message.getContactPhone(),
                clientId,
                message.getCreatedAt());
    }

    private MessagingFacade.ReportableMessage toReportable(Message message) {
        String text = switch (message.getType()) {
            case TEXT -> message.getBody();
            case MEETING_POINT -> message.getPlaceLabel();
            case CONTACT -> message.getContactName();
        };
        String excerpt = text == null ? "" : text.length() > EXCERPT_LENGTH ? text.substring(0, EXCERPT_LENGTH) : text;
        return new MessagingFacade.ReportableMessage(
                message.getId(), message.getConversation().getId(), message.getSender().getId(), excerpt);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public record TypingTarget(Long typistId, String otherEmail) {
    }
}
