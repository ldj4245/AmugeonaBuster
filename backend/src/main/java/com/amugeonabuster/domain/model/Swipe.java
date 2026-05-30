package com.amugeonabuster.domain.model;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

@Getter
public class Swipe {
    private final UUID memberId;
    private final String menuName;
    private final boolean isLike;

    @Builder
    public Swipe(UUID memberId, String menuName, boolean isLike) {
        if (memberId == null) {
            throw new IllegalArgumentException("멤버 ID는 필수입니다.");
        }
        if (menuName == null || menuName.isBlank()) {
            throw new IllegalArgumentException("메뉴 이름은 필수입니다.");
        }
        this.memberId = memberId;
        this.menuName = menuName;
        this.isLike = isLike;
    }
}
