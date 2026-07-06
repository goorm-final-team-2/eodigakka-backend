package com.eodigakka.domain.place;

/**
 * Single place result returned to the frontend from Kakao Local search.
 *
 * @param kakaoPlaceId Kakao Local place identifier
 * @param name place name
 * @param address lot-number address
 * @param roadAddress road-name address
 * @param category Kakao category name
 * @param placeUrl Kakao place detail URL
 * @param phone place phone number
 * @param latitude place latitude
 * @param longitude place longitude
 * @param distance distance from the requested center in meters
 */
public record PlaceSearchItemResponse(
    String kakaoPlaceId,
    String name,
    String address,
    String roadAddress,
    String category,
    String placeUrl,
    String phone,
    double latitude,
    double longitude,
    Integer distance) {}
