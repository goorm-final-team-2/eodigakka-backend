package com.eodigakka.domain.place;

import java.util.List;

/**
 * Paged Kakao Local keyword-search result.
 *
 * @param items places on the current page
 * @param page requested Kakao page number
 * @param size requested page size
 * @param totalCount total result count reported by Kakao
 * @param pageableCount pageable result count reported by Kakao
 * @param isEnd whether Kakao reports this page as the last page
 */
public record PlaceSearchResponse(
    List<PlaceSearchItemResponse> items,
    int page,
    int size,
    int totalCount,
    int pageableCount,
    boolean isEnd) {}
