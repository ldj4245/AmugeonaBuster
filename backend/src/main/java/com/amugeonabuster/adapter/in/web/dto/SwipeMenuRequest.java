package com.amugeonabuster.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class SwipeMenuRequest {
    private UUID memberId;
    private String menuName;

    @JsonProperty("isLike")
    private boolean isLike;
}

