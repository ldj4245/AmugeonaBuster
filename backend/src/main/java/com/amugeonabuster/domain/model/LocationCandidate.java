package com.amugeonabuster.domain.model;

/** 검색 또는 현재 위치에서 확정한 약속 장소 후보. */
public final class LocationCandidate {
    private final String placeId;
    private final String name;
    private final String address;
    private final double latitude;
    private final double longitude;

    public LocationCandidate(String placeId, String name, String address, double latitude, double longitude) {
        this.placeId = placeId != null ? placeId : "";
        this.name = name != null && !name.isBlank() ? name : "선택한 위치";
        this.address = address != null ? address : "";
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getPlaceId() { return placeId; }
    public String getName() { return name; }
    public String getAddress() { return address; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
}
