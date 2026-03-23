package com.ces.erp.config.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ConfigItemRequest {

    @NotBlank(message = "Kateqoriya tələb olunur")
    @Size(max = 100)
    private String category;

    @NotBlank(message = "Açar tələb olunur")
    @Size(max = 200)
    private String key;

    @Size(max = 500)
    private String value;

    @Size(max = 1000)
    private String description;

    private int sortOrder;

    private boolean active = true;
}
