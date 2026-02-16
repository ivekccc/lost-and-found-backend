package com.example.demo.service;


import com.example.demo.dto.AutoCompleteSuggestionDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LocationService {
    @Value("${locationiq.api-key}")
    private String apiKey;

    @Value("${locationiq.base-url}")
    private String baseUrl;

    @Value("${locationiq.default-country}")
    private String defaultCountry;

    private final RestTemplate restTemplate;

    @SuppressWarnings("unchecked")
    public List<AutoCompleteSuggestionDTO>  getAutoCompleteSuggestions(String query){
        String url = baseUrl + "/autocomplete"
                + "?key=" + apiKey
                + "&q=" + query
                + "&countrycodes=" + defaultCountry
                + "&viewbox=20.22,44.93,20.65,44.68"
                + "&bounded=1"
                + "&limit=5";
        try{
            Map<String,Object>[] response =restTemplate.getForObject(url,Map[].class);
            if(response==null || response.length==0){
                return Collections.emptyList();
            }
            return Arrays.stream(response).map(this::mapToDTO)
                    .collect(Collectors.toList());
        }catch (Exception e){
            throw  new RuntimeException("LocationIQ API error: "+ e.getMessage(),e);
        }
    }

    private AutoCompleteSuggestionDTO mapToDTO(Map<String,Object> item){
        AutoCompleteSuggestionDTO dto=new AutoCompleteSuggestionDTO();
        dto.setPlaceId((String) item.get("place_id"));
        dto.setDisplayName((String) item.get("display_name"));
        dto.setDisplayPlace((String) item.get("display_place"));
        dto.setDisplayAddress((String) item.get("display_address"));
        dto.setLatitude(Double.parseDouble((String) item.get("lat")));
        dto.setLongitude(Double.parseDouble((String) item.get("lon")));
        return dto;
    }


}
