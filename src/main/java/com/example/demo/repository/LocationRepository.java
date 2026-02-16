package com.example.demo.repository;

import com.example.demo.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;


public interface LocationRepository extends JpaRepository<Location,Integer> {

    Optional<Location> findByOsmId(String osmId);

    List<Location> findByCity(String city);

    List<Location> findByCityAndDistrict(String city, String district);

    @Query("SELECT DISTINCT l.city FROM Location l WHERE l.city IS NOT NULL")
    List<String> findDistinctCities();

    @Query("SELECT DISTINCT l.district FROM Location l WHERE l.city = :city AND l.district IS NOT NULL")
    List<String> findDistinctDistrictsByCity(@Param("city") String city);
}
