package com.ces.erp.department.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class DepartmentRequest {

    @NotBlank(message = "Şöbə adı boş ola bilməz")
    @Size(min = 2, max = 100, message = "Şöbə adı 2-100 simvol arasında olmalıdır")
    @Pattern(regexp = ".*\\p{L}.*", message = "Şöbə adı ən azı bir hərf içərməlidir")
    private String name;

    private String description;
}
