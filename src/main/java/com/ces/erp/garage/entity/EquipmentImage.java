package com.ces.erp.garage.entity;

import com.ces.erp.common.entity.BaseEntity;
import com.ces.erp.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "equipment_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EquipmentImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Column(nullable = false)
    private String imagePath;

    @Column(length = 255)
    private String imageName;

    @Column(length = 50)
    private String fileType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;
}
