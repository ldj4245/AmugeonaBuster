package com.amugeonabuster.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Member {
    private final UUID id;
    private final String nickname;
    private boolean isReady;

    @Builder
    public Member(UUID id, String nickname, boolean isReady) {
        this.id = id != null ? id : UUID.randomUUID();
        this.nickname = nickname;
        this.isReady = isReady;
    }

    public void toggleReady() {
        this.isReady = !this.isReady;
    }
}
