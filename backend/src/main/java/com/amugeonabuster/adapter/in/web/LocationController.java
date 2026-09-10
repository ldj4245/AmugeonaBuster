package com.amugeonabuster.adapter.in.web;

import com.amugeonabuster.application.port.out.SearchLocationPort;
import com.amugeonabuster.domain.model.LocationCandidate;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
public class LocationController {
    private final SearchLocationPort searchLocationPort;

    @GetMapping("/search")
    public List<LocationResponse> search(@RequestParam("query") String query) {
        if (query == null || query.isBlank()) return List.of();
        return searchLocationPort.search(query.trim()).stream().map(LocationResponse::from).toList();
    }

    @GetMapping("/reverse")
    public ResponseEntity<LocationResponse> reverse(@RequestParam double latitude, @RequestParam double longitude) {
        if (!Double.isFinite(latitude) || !Double.isFinite(longitude)
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            return ResponseEntity.badRequest().build();
        }
        LocationCandidate candidate = searchLocationPort.reverse(latitude, longitude);
        return candidate == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(LocationResponse.from(candidate));
    }

    public record LocationResponse(String placeId, String name, String address, double latitude, double longitude) {
        static LocationResponse from(LocationCandidate candidate) {
            return new LocationResponse(candidate.getPlaceId(), candidate.getName(), candidate.getAddress(),
                    candidate.getLatitude(), candidate.getLongitude());
        }
    }
}
