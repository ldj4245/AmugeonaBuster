package com.amugeonabuster.adapter.in.web.dto;

import com.amugeonabuster.domain.model.Restaurant;
import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class RestaurantResponse {
    private final UUID id;
    private final String name;
    private final String address;
    private final double latitude;
    private final double longitude;
    private final String phone;
    private final String category;
    private final String placeUrl;
    private final String externalPlaceId;
    private final int distanceMeters;
    private final String matchType;

    public static RestaurantResponse fromDomain(Restaurant restaurant) {
        if (restaurant == null) {
            return null;
        }
        return RestaurantResponse.builder()
                .id(restaurant.getId())
                .name(restaurant.getName())
                .address(restaurant.getAddress())
                .latitude(restaurant.getLatitude())
                .longitude(restaurant.getLongitude())
                .phone(restaurant.getPhone())
                .category(restaurant.getCategory())
                .placeUrl(restaurant.getPlaceUrl())
                .externalPlaceId(restaurant.getExternalPlaceId())
                .distanceMeters(restaurant.getDistanceMeters())
                .matchType(restaurant.getMatchType())
                .build();
    }
}
