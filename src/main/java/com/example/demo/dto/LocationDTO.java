package com.example.demo.dto;


import com.example.demo.model.Location;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Location data")
public class LocationDTO {
    private Double latitude;
    private Double longitude;
    private String formattedAddress;

    public static LocationDTO fromEntity(Location location){
        if(location == null){
            return null;
        }
        return LocationDTO.builder().latitude(location.getLatitude().doubleValue())
                .longitude(location.getLongitude().doubleValue())
                .formattedAddress(location.getFormattedAddress()).build();
    }
}
