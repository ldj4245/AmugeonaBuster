package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.Restaurant;

import java.util.List;

public interface RecommendRestaurantsPort {
    List<Restaurant> recommend(String menuName, String location);
}
