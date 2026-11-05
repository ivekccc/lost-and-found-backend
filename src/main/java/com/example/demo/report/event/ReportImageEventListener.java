package com.example.demo.report.event;

import com.example.demo.shared.media.CloudinaryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class ReportImageEventListener {

    private final CloudinaryService cloudinaryService;

    @TransactionalEventListener
    public void onImagesRemoved(ReportImagesRemovedEvent event) {
        log.info("Removing {} photos dropped from an edited report", event.getPublicIds().size());
        cloudinaryService.deleteImages(event.getPublicIds());
    }
}
