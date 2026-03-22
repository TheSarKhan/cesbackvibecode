package com.ces.erp.common.search;

import com.ces.erp.common.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<SearchResultItem>>> search(@RequestParam(defaultValue = "") String q) {
        return ResponseEntity.ok(ApiResponse.success("Axtarış nəticələri", searchService.search(q)));
    }
}
