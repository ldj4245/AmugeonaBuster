package com.amugeonabuster.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Restaurant {
    private final UUID id;
    private final String name;
    private final String address;
    private final double latitude;
    private final double longitude;
    private final String phone;
    private final String category;
    private final String placeUrl;

    @Builder
    public Restaurant(UUID id, String name, String address, double latitude, double longitude, String phone, String category, String placeUrl) {
        this.id = id != null ? id : UUID.randomUUID();
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.phone = phone;
        this.category = category != null ? category : "";
        this.placeUrl = placeUrl != null ? placeUrl : "";
    }
}
