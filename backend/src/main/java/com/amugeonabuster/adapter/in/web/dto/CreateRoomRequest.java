package com.amugeonabuster.adapter.in.web.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class CreateRoomRequest {
    private String hostNickname;
    private String location;
    private String locationAddress;
    private String locationPlaceId;
    private Double latitude;
    private Double longitude;
    private int maxSwipeCount;
    private List<String> customMenus;
}
