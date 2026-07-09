package com.example.demo.event;

import com.example.demo.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {

    private final CloudinaryService cloudinaryService;

    @TransactionalEventListener
    public void onUserDeleted(UserDeletedEvent event) {
        log.info("UserDeletedEvent received for user {} ({} images to remove)",
                event.getUserId(), event.getImagePublicIds().size());
        cloudinaryService.deleteImages(event.getImagePublicIds());
    }
}
