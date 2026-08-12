package com.example.demo.location.repository;

import com.example.demo.location.model.City;
import com.example.demo.user.model.User;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByActiveTrueOrderByNameAsc();

    Optional<City> findByCode(String code);

    /**
     * The user's city as a full entity in one query. Deliberately not user.getActiveCity(): that is a
     * lazy proxy and callers read the bounding box outside a transaction.
     */
    @Query("SELECT u.activeCity FROM User u WHERE u.email = :email")
    Optional<City> findActiveCityByUserEmail(@Param("email") String email);

    /**
     * The city a point falls in, through zone_resolve and zones.city_id. Deliberately not compared
     * against a bounding box — a rectangle around Belgrade also contains Pančevo.
     */
    @Query(value = "SELECT c.* FROM zone_resolve(:latitude, :longitude) r "
            + "JOIN zones z ON z.id = r.zone_id "
            + "JOIN cities c ON c.id = z.city_id "
            + "WHERE c.active = TRUE", nativeQuery = true)
    Optional<City> findCityContaining(@Param("latitude") double latitude,
                                      @Param("longitude") double longitude);
}
