package com.example.demo.shared.exception;
public class EmailSendException extends  RuntimeException{
    public EmailSendException(String message){
        super(message);
    }
}
