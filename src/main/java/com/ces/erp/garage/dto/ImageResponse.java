package com.ces.erp.garage.dto;

import com.ces.erp.garage.entity.EquipmentImage;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ImageResponse {

    private Long id;
    private String imageName;
    private String fileType;
    private String uploadedByUserName;
    private LocalDateTime createdAt;

    public static ImageResponse from(EquipmentImage img) {
        return ImageResponse.builder()
                .id(img.getId())
                .imageName(img.getImageName())
                .fileType(img.getFileType())
                .uploadedByUserName(img.getUploadedBy() != null ? img.getUploadedBy().getFullName() : null)
                .createdAt(img.getCreatedAt())
                .build();
    }
}
