package com.example.demo.util;

import java.security.SecureRandom;

public class VerificationCodeGenerator {

    private static final SecureRandom RANDOM=new SecureRandom();
    private static final String CHARACTERS="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static String generateVerificationCode(int length) {
        StringBuilder code=new StringBuilder(length);
        for(int i=0;i<length;i++){
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}
