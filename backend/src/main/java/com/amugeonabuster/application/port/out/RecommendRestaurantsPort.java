package com.amugeonabuster.application.port.out;

import com.amugeonabuster.domain.model.Restaurant;

import java.util.List;

public interface RecommendRestaurantsPort {
    List<Restaurant> recommend(String menuName, String location);

    /**
     * 좌표가 이미 확정된 위치를 기준으로 맛집을 찾는다.
     * 기존 문자열 기반 호출과의 호환을 위해 기본 구현은 예전 메서드로 위임한다.
     */
    default List<Restaurant> recommend(String menuName, String location, Double latitude, Double longitude) {
        return recommend(menuName, location);
    }
}
