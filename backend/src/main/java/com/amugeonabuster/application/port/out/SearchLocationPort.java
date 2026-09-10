package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.LocationCandidate;

import java.util.List;

public interface SearchLocationPort {
    List<LocationCandidate> search(String query);

    LocationCandidate reverse(double latitude, double longitude);
}
