package com.example.demo.location.repository;
public interface ZoneBoundaryView {

    String getName();

    String getCity();

    /**
     * Naziv nadredene opstine za zone nivoa 2, ili null za same opstine.
     */
    String getParentName();

    String getBoundaryGeoJson();
}
