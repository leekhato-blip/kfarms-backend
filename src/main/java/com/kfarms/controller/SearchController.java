package com.kfarms.controller;

import com.kfarms.dto.SearchResultDto;
import com.kfarms.service.SearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;


import java.util.List;


@RestController
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping({"/search", "/api/search"})
    public List<SearchResultDto> search(
            @RequestParam("q") String q,
            @RequestParam(value = "limit", required = false, defaultValue = "20") int limit
    ) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return searchService.search(q, safeLimit);
    }
}