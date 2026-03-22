package com.ces.erp.common.search;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SearchResultItem {
    private Long id;
    private String type;       // "MÜŞTƏRİ", "PODRATÇI", "TEXNİKA", "SORĞU", "LAYİHƏ", "OPERATOR", "İNVESTOR"
    private String label;      // main display text
    private String subLabel;   // secondary text (e.g., VÖEN, equipment code)
    private String path;       // frontend route (e.g., "/customers")
}
