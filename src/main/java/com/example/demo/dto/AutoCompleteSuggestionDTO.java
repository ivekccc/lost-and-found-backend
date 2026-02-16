package com.example.demo.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class AutoCompleteSuggestionDTO {
    private String placeId;
    private String displayName;
    private String displayPlace;
    private String displayAddress;
    private Double latitude;
    private Double longitude;
}
