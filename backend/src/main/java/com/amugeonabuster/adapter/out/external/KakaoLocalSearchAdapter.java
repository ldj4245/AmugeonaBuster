package com.amugeonabuster.adapter.out.external;

import com.amugeonabuster.application.port.out.RecommendRestaurantsPort;
import com.amugeonabuster.domain.model.Restaurant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@Primary
public class KakaoLocalSearchAdapter implements RecommendRestaurantsPort {

    private final String restKey;
    private final RestClient restClient;

    // 반경 2km 내 검색
    private static final int SEARCH_RADIUS_METERS = 2000;

    public KakaoLocalSearchAdapter(@Value("${kakao.api.rest-key}") String restKey) {
        this.restKey = restKey;
        this.restClient = RestClient.create();
    }

    @Override
    public List<Restaurant> recommend(String menuName, String location) {
        log.info("Kakao Local Search: menu='{}', location='{}'", menuName, location);

        if (restKey == null || restKey.isBlank() || restKey.equals("YOUR_REST_API_KEY")) {
            log.warn("Kakao REST API Key is missing or invalid. Returning empty list.");
            return new ArrayList<>();
        }

        try {
            // Step 1: 텍스트 위치 → 좌표 변환
            double[] coords = geocodeLocation(location);

            // Step 2: 메뉴명 + 좌표 + 반경으로 정확한 음식점 검색
            if (coords != null) {
                log.info("Geocoded '{}' → lng={}, lat={}", location, coords[0], coords[1]);
                List<Restaurant> results = searchByMenuAndCoords(menuName, coords[0], coords[1]);
                if (!results.isEmpty()) {
                    return results;
                }
                log.warn("No results with coords, falling back to text query.");
            }

            // Fallback: 좌표 변환 실패 시 기존 방식으로 검색
            return searchByTextQuery(location + " " + menuName);

        } catch (Exception e) {
            log.error("Error during restaurant recommendation for menu='{}', location='{}'", menuName, location, e);
            return new ArrayList<>();
        }
    }

    /**
     * Step 1: 텍스트 위치명을 카카오 키워드 검색으로 좌표로 변환
     * 반환값: [경도(x), 위도(y)] 또는 null
     */
    private double[] geocodeLocation(String location) {
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", location)
                    .queryParam("size", 1)
                    .build()
                    .toUri();

            KakaoSearchResponse response = restClient.get()
                    .uri(uri)
                    .header("Authorization", "KakaoAK " + restKey)
                    .retrieve()
                    .body(KakaoSearchResponse.class);

            if (response != null && response.getDocuments() != null && !response.getDocuments().isEmpty()) {
                KakaoDocument doc = response.getDocuments().get(0);
                if (doc.getX() != null && doc.getY() != null) {
                    double lng = Double.parseDouble(doc.getX());
                    double lat = Double.parseDouble(doc.getY());
                    return new double[]{lng, lat};
                }
            }
        } catch (Exception e) {
            log.warn("Geocoding failed for location '{}': {}", location, e.getMessage());
        }
        return null;
    }

    /**
     * Step 2: 메뉴명 + 좌표 + 반경으로 음식점 검색 (정확도 높음)
     */
    private List<Restaurant> searchByMenuAndCoords(String menuName, double lng, double lat) {
        URI uri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                .queryParam("query", menuName)
                .queryParam("x", lng)
                .queryParam("y", lat)
                .queryParam("radius", SEARCH_RADIUS_METERS)
                .queryParam("category_group_code", "FD6")  // 음식점만
                .queryParam("sort", "distance")             // 가까운 순
                .queryParam("size", 5)
                .build()
                .toUri();

        KakaoSearchResponse response = restClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + restKey)
                .retrieve()
                .body(KakaoSearchResponse.class);

        return parseDocuments(response);
    }

    /**
     * Fallback: 텍스트 쿼리로 검색 (기존 방식)
     */
    private List<Restaurant> searchByTextQuery(String query) {
        log.info("Fallback text search: query='{}'", query);
        URI uri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                .queryParam("query", query)
                .queryParam("category_group_code", "FD6")
                .queryParam("size", 5)
                .build()
                .toUri();

        KakaoSearchResponse response = restClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + restKey)
                .retrieve()
                .body(KakaoSearchResponse.class);

        return parseDocuments(response);
    }

    private List<Restaurant> parseDocuments(KakaoSearchResponse response) {
        if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
            return new ArrayList<>();
        }

        List<Restaurant> restaurants = new ArrayList<>();
        for (KakaoDocument doc : response.getDocuments()) {
            double lat = 37.5665;
            double lng = 126.9780;
            try {
                if (doc.getY() != null) lat = Double.parseDouble(doc.getY());
                if (doc.getX() != null) lng = Double.parseDouble(doc.getX());
            } catch (NumberFormatException e) {
                log.error("Failed to parse coordinates: x={}, y={}", doc.getX(), doc.getY(), e);
            }

            String address = doc.getRoad_address_name();
            if (address == null || address.isBlank()) {
                address = doc.getAddress_name();
            }

            restaurants.add(Restaurant.builder()
                    .id(UUID.randomUUID())
                    .name(doc.getPlace_name())
                    .address(address)
                    .latitude(lat)
                    .longitude(lng)
                    .phone(doc.getPhone() != null ? doc.getPhone() : "")
                    .category(doc.getCategory_group_name() != null ? doc.getCategory_group_name() : "")
                    .placeUrl(doc.getPlace_url() != null ? doc.getPlace_url() : "")
                    .build());
        }
        log.info("Parsed {} restaurants", restaurants.size());
        return restaurants;
    }

    @lombok.Data
    public static class KakaoSearchResponse {
        private List<KakaoDocument> documents;
    }

    @lombok.Data
    public static class KakaoDocument {
        private String place_name;
        private String address_name;
        private String road_address_name;
        private String x;
        private String y;
        private String phone;
        private String category_group_name;
        private String place_url;
    }
}
