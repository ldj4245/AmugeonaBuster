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
    private int maxSwipeCount;
    private List<String> customMenus;
}
