package com.ces.erp.common.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ErrorReportRequest {

    private String errorMessage;
    private String pageUrl;
    private String requestUrl;
    private String requestMethod;
    private Integer httpStatus;
    private String errorDetails;
    private String userAgent;
    private String userEmail;
    private String userName;
    private String userRole;
    private String timestamp;
}
