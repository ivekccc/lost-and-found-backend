package com.example.demo.shared.concurrency;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LockScope {
    REGISTRATION(1),
    PASSWORD_RESET(2),
    CLAIM_SUBMISSION(3),
    ABUSE_REPORT(4),
    SAVED_SEARCH(5),
    THANKS(6);

    private final int id;
}
