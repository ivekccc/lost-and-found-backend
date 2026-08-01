package com.example.demo.repository;

import com.example.demo.model.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long> {

    List<City> findByActiveTrueOrderByNameAsc();

    Optional<City> findByCode(String code);

    /**
     * Grad korisnika, ucitan kao pun entitet jednim upitom.
     *
     * Namerno se ne ide preko {@code user.getActiveCity()}: to je LAZY proxy, pa bi citanje
     * okvira ({@code bbox*}) izvan transakcije radilo samo zahvaljujuci open-in-view-u.
     * Gasenje te podrazumevane opcije je uobicajen korak pri profilisanju i tiho bi oborilo
     * pretragu adresa.
     */
    @Query("SELECT u.activeCity FROM User u WHERE u.email = :email")
    Optional<City> findActiveCityByUserEmail(@Param("email") String email);

    /**
     * Grad kome pripada data tacka, kroz {@code zone_resolve} pa {@code zones.city_id}.
     *
     * Namerno se ne poredi sa granicom grada: granica grada nigde ne postoji kao zaseban
     * poligon nego kao unija zona, pa bi drugi put do odgovora znacio drugo pravilo koje
     * moze da se raziđe od {@code zone_resolve} — greska zbog koje V40 postoji.
     *
     * Prazan rezultat je ocekivan odgovor, ne greska: tacka u Uzicu nije ni u jednom
     * pokrivenom gradu.
     */
    @Query(value = "SELECT c.* FROM zone_resolve(:latitude, :longitude) r "
            + "JOIN zones z ON z.id = r.zone_id "
            + "JOIN cities c ON c.id = z.city_id "
            + "WHERE c.active = TRUE", nativeQuery = true)
    Optional<City> findCityContaining(@Param("latitude") double latitude,
                                      @Param("longitude") double longitude);
}
