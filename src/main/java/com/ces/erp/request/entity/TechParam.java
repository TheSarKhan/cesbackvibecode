package com.ces.erp.request.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TechParam {

    @Column(name = "param_key", length = 100)
    private String paramKey;

    @Column(name = "param_value", length = 255)
    private String paramValue;
}
