package com.example.demo.shared.web;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.Map;

public final class PageQuery {

    public static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_PAGE_SIZE = 25;

    private PageQuery() {
    }

    public static PageRequest of(Integer page, Integer size, String sort, String direction,
                                 Map<String, String> sortableColumns, String defaultColumn, Sort.Direction defaultDirection) {
        int safePage = page == null ? 0 : Math.max(page, 0);
        int safeSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String column = sort != null && sortableColumns.containsKey(sort) ? sort : defaultColumn;
        Sort.Direction sortDirection = parseDirection(direction, column.equals(defaultColumn) ? defaultDirection : Sort.Direction.ASC);
        Sort order = Sort.by(sortDirection, sortableColumns.get(column));
        if (!"id".equals(sortableColumns.get(column))) {
            order = order.and(Sort.by(sortDirection, "id"));
        }
        return PageRequest.of(safePage, safeSize, order);
    }

    private static Sort.Direction parseDirection(String direction, Sort.Direction fallback) {
        if (direction == null) {
            return fallback;
        }
        return "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC
                : "desc".equalsIgnoreCase(direction) ? Sort.Direction.DESC
                : fallback;
    }
}
