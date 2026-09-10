package com.amugeonabuster.adapter.out.external;

import com.amugeonabuster.application.port.out.RecommendRestaurantsPort;
import com.amugeonabuster.domain.model.Restaurant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Component
@Primary
public class KakaoLocalSearchAdapter implements RecommendRestaurantsPort {

    private final String restKey;
    private final RestClient restClient;

    private static final int SEARCH_RADIUS_METERS = 1000;
    private static final int MAX_RESULTS = 5;

    public KakaoLocalSearchAdapter(@Value("${kakao.api.rest-key}") String restKey) {
        this.restKey = restKey;
        this.restClient = RestClient.create();
    }

    @Override
    public List<Restaurant> recommend(String menuName, String location) {
        return recommend(menuName, location, null, null);
    }

    @Override
    public List<Restaurant> recommend(String menuName, String location, Double latitude, Double longitude) {
        log.info("Kakao Local Search: menu='{}', location='{}', coordinates={},{}", menuName, location, latitude, longitude);

        if (restKey == null || restKey.isBlank() || restKey.equals("YOUR_REST_API_KEY")) {
            log.warn("Kakao REST API Key is missing or default. Returning empty list.");
            return List.of();
        }

        if (latitude != null && longitude != null
                && Double.isFinite(latitude) && Double.isFinite(longitude)
                && latitude >= -90 && latitude <= 90 && longitude >= -180 && longitude <= 180
                && (latitude != 0 || longitude != 0)) {
            return safeSearchRestaurants(menuName, longitude, latitude);
        }

        double[] coords = geocodeLocation(location);
        if (coords == null) {
            log.warn("Could not resolve meeting location '{}'. Skipping restaurant search.", location);
            return List.of();
        }
        log.info("Resolved '{}' to lng={}, lat={}", location, coords[0], coords[1]);
        return safeSearchRestaurants(menuName, coords[0], coords[1]);
    }

    private List<Restaurant> safeSearchRestaurants(String menuName, double lng, double lat) {
        try {
            return searchRestaurants(menuName, lng, lat);
        } catch (Exception e) {
            // 카카오 장애나 호출 한도 초과가 투표 결과 자체를 막지 않도록 빈 추천으로 마무리한다.
            log.warn("Restaurant search failed near {},{} for '{}': {}", lng, lat, menuName, e.getMessage());
            return List.of();
        }
    }

    private List<Restaurant> searchRestaurants(String menuName, double lng, double lat) {
        Map<String, Restaurant> results = new LinkedHashMap<>();

        for (String query : menuQueries(menuName)) {
            merge(results, searchByMenuAndCoords(query, lng, lat));
            if (results.size() >= MAX_RESULTS) break;
        }

        // 메뉴 검색 결과가 부족할 때만 주변 음식점을 보충한다. UI에서 매칭 여부를 구분해 표시한다.
        if (results.size() < MAX_RESULTS) {
            merge(results, searchNearbyRestaurants(lng, lat));
        }

        return results.values().stream().limit(MAX_RESULTS).toList();
    }

    private List<String> menuQueries(String menuName) {
        if (menuName == null || menuName.isBlank()) return List.of();
        String normalized = menuName.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
        List<String> queries = new ArrayList<>();
        queries.add(menuName.trim());
        if (normalized.contains("돈카츠") || normalized.contains("돈까스")) {
            queries.add("돈카츠");
            queries.add("돈까스");
        } else if (normalized.contains("짜장")) {
            queries.add("짜장면");
        } else if (normalized.contains("짬뽕")) {
            queries.add("짬뽕");
        } else if (normalized.contains("국밥")) {
            queries.add("국밥");
        }
        return queries.stream().distinct().limit(3).toList();
    }

    private void merge(Map<String, Restaurant> target, List<Restaurant> restaurants) {
        for (Restaurant restaurant : restaurants) {
            String key = restaurant.getExternalPlaceId();
            if (key == null || key.isBlank()) key = restaurant.getName() + "|" + restaurant.getAddress();
            target.putIfAbsent(key, restaurant);
            if (target.size() >= MAX_RESULTS) return;
        }
    }

    /** 문자열 위치를 주소 검색 우선으로 좌표로 변환한다. */
    private double[] geocodeLocation(String location) {
        if (location == null || location.isBlank()) return null;
        try {
            URI addressUri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/address.json")
                    .queryParam("query", location)
                    .queryParam("size", 1)
                    .build()
                    .toUri();
            double[] addressCoords = requestFirstCoordinates(addressUri);
            if (addressCoords != null) return addressCoords;

            URI keywordUri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", location)
                    .queryParam("size", 1)
                    .build()
                    .toUri();
            return requestFirstCoordinates(keywordUri);
        } catch (Exception e) {
            log.warn("Location lookup failed for '{}': {}", location, e.getMessage());
            return null;
        }
    }

    private double[] requestFirstCoordinates(URI uri) {
        KakaoSearchResponse response = restClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + restKey)
                .retrieve()
                .body(KakaoSearchResponse.class);
        if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) return null;
        return parseCoordinates(response.getDocuments().get(0));
    }

    private List<Restaurant> searchByMenuAndCoords(String query, double lng, double lat) {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                .queryParam("query", query)
                .queryParam("x", lng)
                .queryParam("y", lat)
                .queryParam("radius", SEARCH_RADIUS_METERS)
                .queryParam("category_group_code", "FD6")
                .queryParam("sort", "distance")
                .queryParam("size", MAX_RESULTS)
                .build()
                .toUri();
        return parseDocuments(fetch(uri), "MENU_MATCH");
    }

    private List<Restaurant> searchNearbyRestaurants(double lng, double lat) {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/category.json")
                .queryParam("category_group_code", "FD6")
                .queryParam("x", lng)
                .queryParam("y", lat)
                .queryParam("radius", SEARCH_RADIUS_METERS)
                .queryParam("sort", "distance")
                .queryParam("size", MAX_RESULTS)
                .build()
                .toUri();
        return parseDocuments(fetch(uri), "NEARBY");
    }

    private KakaoSearchResponse fetch(URI uri) {
        return restClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + restKey)
                .retrieve()
                .body(KakaoSearchResponse.class);
    }

    private List<Restaurant> parseDocuments(KakaoSearchResponse response, String matchType) {
        if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) return List.of();

        List<Restaurant> restaurants = new ArrayList<>();
        for (KakaoDocument doc : response.getDocuments()) {
            double[] coords = parseCoordinates(doc);
            if (coords == null || doc.getPlace_name() == null || doc.getPlace_name().isBlank()) continue;

            String address = doc.getRoad_address_name();
            if (address == null || address.isBlank()) address = doc.getAddress_name();
            int distance = 0;
            try {
                if (doc.getDistance() != null && !doc.getDistance().isBlank()) {
                    distance = Integer.parseInt(doc.getDistance());
                }
            } catch (NumberFormatException ignored) {
                // 거리 정보가 없는 결과도 장소 자체는 사용할 수 있다.
            }

            restaurants.add(Restaurant.builder()
                    .id(UUID.randomUUID())
                    .externalPlaceId(doc.getId())
                    .name(doc.getPlace_name())
                    .address(address != null ? address : "주소 정보 없음")
                    .latitude(coords[1])
                    .longitude(coords[0])
                    .phone(doc.getPhone() != null ? doc.getPhone() : "")
                    .category(doc.getCategory_group_name() != null ? doc.getCategory_group_name() : "")
                    .placeUrl(doc.getPlace_url() != null ? doc.getPlace_url() : "")
                    .distanceMeters(distance)
                    .matchType(matchType)
                    .build());
        }
        return restaurants;
    }

    private double[] parseCoordinates(KakaoDocument doc) {
        if (doc == null || doc.getX() == null || doc.getY() == null) return null;
        try {
            double lng = Double.parseDouble(doc.getX());
            double lat = Double.parseDouble(doc.getY());
            if (!Double.isFinite(lng) || !Double.isFinite(lat)) return null;
            return new double[]{lng, lat};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @lombok.Data
    public static class KakaoSearchResponse {
        private List<KakaoDocument> documents;
    }

    @lombok.Data
    public static class KakaoDocument {
        private String id;
        private String place_name;
        private String address_name;
        private String road_address_name;
        private String x;
        private String y;
        private String distance;
        private String phone;
        private String category_group_name;
        private String place_url;
    }
}
