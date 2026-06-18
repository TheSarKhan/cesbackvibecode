package com.ces.erp.partydoc;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

/**
 * Sənəd mərkəzində göstərilən bir sənəd — istənilən mənbədən
 * (əl ilə, müqavilə, təhvil aktı, texnika sənədi, qaimə).
 */
@Data
@Builder
public class PartyDocumentDto {
    private String category;     // UI qruplaşması (Müqavilələr, Təhvil aktları, ...)
    private String sourceType;   // MANUAL | REQUEST_DOC | COORDINATOR_DOC | EQUIPMENT_DOC | GENERATED_DOC | INVOICE_AKT
    private Long sourceId;
    private String name;         // göstəriləcək ad
    private String fileType;     // PDF / JPG / ...
    private String context;      // layihə/texnika etiketi
    private LocalDate date;
    private boolean manual;      // əl ilə yüklənib (silinə bilər)
}
