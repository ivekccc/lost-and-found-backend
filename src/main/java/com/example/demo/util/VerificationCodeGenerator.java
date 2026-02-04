package com.example.demo.util;

import java.security.SecureRandom;

public class VerificationCodeGenerator {

    private static final SecureRandom RANDOM=new SecureRandom();
    private static final String CHARACTERS="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int VERIFICATION_CODE_LENGTH=6;

    public static String generateVerificationCode() {
        StringBuilder code=new StringBuilder(VERIFICATION_CODE_LENGTH);
        for(int i=0;i<VERIFICATION_CODE_LENGTH;i++){
            code.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }
}
