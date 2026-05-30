package com.amugeonabuster.adapter.in.web.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
@Builder
public class MemberResponse {
    private final UUID id;
    private final String nickname;
    private final boolean isReady;
}
