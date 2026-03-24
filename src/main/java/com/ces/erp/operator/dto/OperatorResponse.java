package com.ces.erp.operator.dto;

import com.ces.erp.enums.OperatorDocumentType;
import com.ces.erp.operator.entity.Operator;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@Builder
public class OperatorResponse {

    private Long id;
    private String firstName;
    private String lastName;
    private String fullName;
    private String address;
    private String phone;
    private String email;
    private String specialization;
    private String notes;
    private boolean documentsComplete;
    private boolean busy;
    private Set<String> uploadedDocumentTypes;
    private List<OperatorDocumentResponse> documents;
    private LocalDateTime createdAt;

    public static OperatorResponse from(Operator o) {
        Set<String> uploaded = o.getDocuments().stream()
                .map(d -> d.getDocumentType().name())
                .collect(Collectors.toSet());

        boolean complete = uploaded.containsAll(
                List.of(OperatorDocumentType.values()).stream()
                        .map(Enum::name)
                        .collect(Collectors.toList())
        );

        return OperatorResponse.builder()
                .id(o.getId())
                .firstName(o.getFirstName())
                .lastName(o.getLastName())
                .fullName(o.getFirstName() + " " + o.getLastName())
                .address(o.getAddress())
                .phone(o.getPhone())
                .email(o.getEmail())
                .specialization(o.getSpecialization())
                .notes(o.getNotes())
                .documentsComplete(complete)
                .uploadedDocumentTypes(uploaded)
                .documents(o.getDocuments().stream()
                        .map(OperatorDocumentResponse::from)
                        .collect(Collectors.toList()))
                .createdAt(o.getCreatedAt())
                .build();
    }
}
