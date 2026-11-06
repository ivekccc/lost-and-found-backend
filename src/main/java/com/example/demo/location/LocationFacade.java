package com.example.demo.location;

import com.example.demo.location.model.City;
import com.example.demo.location.dto.CityDto;
import com.example.demo.location.dto.ReportZoneDto;
import com.example.demo.location.model.Location;
import com.example.demo.location.model.Zone;

public interface LocationFacade {

    Location createAt(java.math.BigDecimal latitude, java.math.BigDecimal longitude);

    City requireByCode(String code);

    City requireActiveCityOf(String userEmail);

    City setActiveCity(String userEmail, Long cityId);

    java.util.Optional<ReportZoneDto> describeZone(Long zoneId);

    java.util.Optional<Zone> findZone(Long zoneId);

    java.util.List<CityDto> listActiveCities();

    java.util.Optional<Zone> resolveZone(java.math.BigDecimal latitude, java.math.BigDecimal longitude);

    CityDto toCityDto(City city);
}
