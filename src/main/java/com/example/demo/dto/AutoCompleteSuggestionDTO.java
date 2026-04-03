package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AutoCompleteSuggestionDTO {
    private String osmId;
    private String osmType;
    private String displayName;
    private String displayPlace;
    private String displayAddress;
}
