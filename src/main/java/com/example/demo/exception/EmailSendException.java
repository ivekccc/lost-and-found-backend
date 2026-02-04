package com.example.demo.exception;

public class EmailSendException extends  RuntimeException{
    public EmailSendException(String message){
        super(message);
    }
}
