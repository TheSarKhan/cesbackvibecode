package com.ces.erp.projectmanager.dto;

import com.ces.erp.projectmanager.entity.PartyType;
import com.ces.erp.projectmanager.entity.ShortlistItem;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class ShortlistItemDto {

    private Long id;
    private PartyType partyType;

    private Long contractorId;
    private String contractorName;

    private Long investorId;
    private String investorName;

    private Long equipmentId;
    private String equipmentName;
    private String equipmentCode;

    private BigDecimal negotiatedPrice;
    private String notes;

    public static ShortlistItemDto from(ShortlistItem item) {
        return ShortlistItemDto.builder()
                .id(item.getId())
                .partyType(item.getPartyType())
                .contractorId(item.getContractor() != null ? item.getContractor().getId() : null)
                .contractorName(item.getContractor() != null ? item.getContractor().getCompanyName() : null)
                .investorId(item.getInvestor() != null ? item.getInvestor().getId() : null)
                .investorName(item.getInvestor() != null ? item.getInvestor().getCompanyName() : null)
                .equipmentId(item.getEquipment() != null ? item.getEquipment().getId() : null)
                .equipmentName(item.getEquipment() != null ? item.getEquipment().getName() : null)
                .equipmentCode(item.getEquipment() != null ? item.getEquipment().getEquipmentCode() : null)
                .negotiatedPrice(item.getNegotiatedPrice())
                .notes(item.getNotes())
                .build();
    }
}
