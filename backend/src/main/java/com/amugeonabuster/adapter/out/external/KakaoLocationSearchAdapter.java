package com.amugeonabuster.adapter.out.external;

import com.amugeonabuster.application.port.out.SearchLocationPort;
import com.amugeonabuster.domain.model.LocationCandidate;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@Slf4j
@Component
public class KakaoLocationSearchAdapter implements SearchLocationPort {
    private final String restKey;
    private final RestClient restClient;

    public KakaoLocationSearchAdapter(@Value("${kakao.api.rest-key}") String restKey) {
        this.restKey = restKey;
        this.restClient = RestClient.create();
    }

    @Override
    public List<LocationCandidate> search(String query) {
        if (!available() || query == null || query.isBlank()) return List.of();
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .queryParam("size", 5)
                    .build()
                    .toUri();
            SearchResponse response = request(uri, SearchResponse.class);
            if (response == null || response.getDocuments() == null) return List.of();
            return response.getDocuments().stream()
                    .map(this::toCandidate)
                    .filter(candidate -> candidate != null)
                    .toList();
        } catch (Exception e) {
            log.warn("Kakao location search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    @Override
    public LocationCandidate reverse(double latitude, double longitude) {
        if (!available()) return null;
        try {
            URI uri = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/geo/coord2address.json")
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .build()
                    .toUri();
            SearchResponse response = request(uri, SearchResponse.class);
            if (response == null || response.getDocuments() == null || response.getDocuments().isEmpty()) return null;
            SearchDocument document = response.getDocuments().get(0);
            String address = document.getRoad_address() != null
                    ? document.getRoad_address().getAddress_name()
                    : document.getAddress() != null ? document.getAddress().getAddress_name() : "";
            String name = address == null || address.isBlank() ? "현재 위치" : address;
            return new LocationCandidate("", name, address, latitude, longitude);
        } catch (Exception e) {
            log.warn("Kakao reverse geocoding failed: {}", e.getMessage());
            return null;
        }
    }

    private boolean available() {
        return restKey != null && !restKey.isBlank() && !restKey.equals("YOUR_REST_API_KEY");
    }

    private <T> T request(URI uri, Class<T> type) {
        return restClient.get()
                .uri(uri)
                .header("Authorization", "KakaoAK " + restKey)
                .retrieve()
                .body(type);
    }

    private LocationCandidate toCandidate(SearchDocument document) {
        if (document == null || document.getX() == null || document.getY() == null) return null;
        try {
            double longitude = Double.parseDouble(document.getX());
            double latitude = Double.parseDouble(document.getY());
            String address = document.getRoad_address_name();
            if (address == null || address.isBlank()) address = document.getAddress_name();
            return new LocationCandidate(document.getId(), document.getPlace_name(), address, latitude, longitude);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Data
    public static class SearchResponse {
        private List<SearchDocument> documents;
    }

    @Data
    public static class SearchDocument {
        private String id;
        private String place_name;
        private String address_name;
        private String road_address_name;
        private String x;
        private String y;
        private ReverseAddress address;
        private ReverseAddress road_address;
    }

    @Data
    public static class ReverseAddress {
        private String address_name;
    }
}
