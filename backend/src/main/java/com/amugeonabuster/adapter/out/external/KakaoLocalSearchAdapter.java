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

    public KakaoLocalSearchAdapter(@Value("${kakao.api.rest-key}") String restKey) {
        this.restKey = restKey;
        this.restClient = RestClient.create();
    }

    @Override
    public List<Restaurant> recommend(String menuName, String location) {
        String query = location + " " + menuName;
        log.info("Kakao Local Search Request: query='{}'", query);

        try {
            if (restKey == null || restKey.isBlank() || restKey.equals("YOUR_REST_API_KEY")) {
                log.warn("Kakao REST API Key is missing or invalid. Returning empty list.");
                return new ArrayList<>();
            }

            URI uri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .queryParam("size", 5)
                    .build()
                    .toUri();

            KakaoSearchResponse response = restClient.get()
                    .uri(uri)
                    .header("Authorization", "KakaoAK " + restKey)
                    .retrieve()
                    .body(KakaoSearchResponse.class);

            if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) {
                log.warn("Kakao Search returned empty results for query '{}'.", query);
                return new ArrayList<>();
            }

            List<Restaurant> restaurants = new ArrayList<>();
            for (KakaoDocument doc : response.getDocuments()) {
                double lat = 37.5665;
                double lng = 126.9780;

                try {
                    if (doc.getY() != null) {
                        lat = Double.parseDouble(doc.getY());
                    }
                    if (doc.getX() != null) {
                        lng = Double.parseDouble(doc.getX());
                    }
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

            log.info("Successfully fetched {} restaurants from Kakao API for query '{}'", restaurants.size(), query);
            return restaurants;

        } catch (Exception e) {
            log.error("Error occurred while calling Kakao Local API for query '{}'. Returning empty list.", query, e);
            return new ArrayList<>();
        }
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
