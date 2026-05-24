package com.ces.erp.projectmanager.dto;

import com.ces.erp.projectmanager.entity.PartyType;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ShortlistSaveRequest {

    private String notes;
    private List<Item> items;

    @Data
    public static class Item {
        private Long id; // null → yeni sətir; var → mövcud sətiri update et
        private PartyType partyType;
        private Long contractorId;
        private Long investorId;
        private Long equipmentId;
        private BigDecimal negotiatedPrice;
        private String notes;
    }
}
