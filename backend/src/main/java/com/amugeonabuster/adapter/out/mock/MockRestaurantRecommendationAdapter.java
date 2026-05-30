package com.amugeonabuster.adapter.out.mock;

import com.amugeonabuster.application.port.out.RecommendRestaurantsPort;
import com.amugeonabuster.domain.model.Restaurant;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Component
public class MockRestaurantRecommendationAdapter implements RecommendRestaurantsPort {

    @Override
    public List<Restaurant> recommend(String menuName, String location) {
        List<Restaurant> mockList = new ArrayList<>();

        // 5개의 그럴듯한 모의(Mock) 맛집 데이터 생성
        mockList.add(Restaurant.builder()
                .id(UUID.randomUUID())
                .name("명가 " + menuName + " 본점")
                .address("서울시 강남구 " + location + "대로 456길 12")
                .latitude(37.4981)
                .longitude(127.0280)
                .phone("02-111-2222")
                .build());

        mockList.add(Restaurant.builder()
                .id(UUID.randomUUID())
                .name(location + " " + menuName + " 천국")
                .address("서울시 강남구 " + location + "로 789길 34")
                .latitude(37.4975)
                .longitude(127.0270)
                .phone("02-333-4444")
                .build());

        mockList.add(Restaurant.builder()
                .id(UUID.randomUUID())
                .name("진짜 맛있는 " + menuName + "집")
                .address("서울시 강남구 " + location + "동 101번지 1층")
                .latitude(37.4990)
                .longitude(127.0265)
                .phone("02-555-6666")
                .build());

        mockList.add(Restaurant.builder()
                .id(UUID.randomUUID())
                .name("황금 " + menuName + " 하우스")
                .address("서울시 강남구 " + location + "역 5번출구 바로 앞")
                .latitude(37.4965)
                .longitude(127.0290)
                .phone("02-777-8888")
                .build());

        mockList.add(Restaurant.builder()
                .id(UUID.randomUUID())
                .name(menuName + " 대감")
                .address("서울시 강남구 " + location + " 먹자골목 중심길 9")
                .latitude(37.4985)
                .longitude(127.0285)
                .phone("02-999-0000")
                .build());

        return mockList;
    }
}
