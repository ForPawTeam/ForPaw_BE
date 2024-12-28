package com.hong.forapw.common.utils;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static com.hong.forapw.common.constants.GlobalConstants.*;

public class PaginationUtils {

    private PaginationUtils() {
    }

    private static final Sort SORT_BY_PARTICIPANT = Sort.by(Sort.Order.desc(SORT_BY_PARTICIPANT_NUM));

    public static final Pageable DEFAULT_PAGEABLE = PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, SORT_BY_ID));
    public static final Pageable RECOMMEND_GROUP_PAGEABLE = PageRequest.of(0, 30, SORT_BY_PARTICIPANT);
    public static final Pageable DEFAULT_IMAGE_PAGEABLE = PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, SORT_BY_MESSAGE_DATE));

    public static boolean isLastPage(Page<?> page) {
        return !page.hasNext();
    }

    public static Pageable createPageable(int pageNumber, int pageSize, String sortByField, Sort.Direction direction) {
        return PageRequest.of(pageNumber, pageSize, Sort.by(direction, sortByField));
    }
}