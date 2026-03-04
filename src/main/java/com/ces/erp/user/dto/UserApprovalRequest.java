package com.ces.erp.user.dto;

import lombok.Data;

import java.util.List;

@Data
public class UserApprovalRequest {

    private boolean hasApproval;

    // Approve edə biləcəyi şöbə ID-ləri
    private List<Long> approvalDepartmentIds;
}
