package com.example.demo.service;

import com.example.demo.repository.PreRegistrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduleTasks {

    private final PreRegistrationRepository preRegistrationRepository;

    @Scheduled(fixedRateString = "${app.cleanup.interval-ms:3600000}")
    @Transactional
    public void cleanupExpiredPreRegistrations() {
        int deleted=preRegistrationRepository.deleteByExpiresAtBefore(LocalDateTime.now());

        if(deleted>0){
            log.info("Cleaned up {} expired pre-registrations", deleted);
        }
    }


}
