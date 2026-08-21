package com.ces.erp.project.service;

import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.project.entity.Project;
import com.ces.erp.project.entity.ProjectDowntime;
import com.ces.erp.project.entity.ProjectEquipmentSwap;
import com.ces.erp.project.repository.ProjectDowntimeRepository;
import com.ces.erp.project.repository.ProjectEquipmentSwapRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectIncidentPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Color CES_DARK  = new Color(15, 23, 42);     // #0f172a
    private static final Color CES_GOLD  = new Color(202, 138, 4);    // #ca8a04
    private static final Color CES_MUTED = new Color(100, 116, 139);  // #64748b
    private static final Color CES_BG    = new Color(248, 250, 252);  // #f8fafc
    private static final Color CES_LINE  = new Color(226, 232, 240);  // #e2e8f0

    private final ProjectDowntimeRepository downtimeRepository;
    private final ProjectEquipmentSwapRepository swapRepository;

    @Transactional(readOnly = true)
    public byte[] generateDowntimeActPdf(Long downtimeId) {
        ProjectDowntime dt = downtimeRepository.findById(downtimeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dayanma qeydiyyatı", downtimeId));
        Project p = dt.getProject();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bfRegular = loadBaseFont(false);
            BaseFont bfBold    = loadBaseFont(true);

            Font titleFont   = font(bfBold, 15, Font.BOLD, CES_DARK);
            Font subFont     = font(bfRegular, 9.5f, Font.NORMAL, CES_MUTED);
            Font h2Font      = font(bfBold, 11, Font.BOLD, CES_DARK);
            Font labelFont   = font(bfBold, 9, Font.BOLD, CES_DARK);
            Font valFont     = font(bfRegular, 9, Font.NORMAL, CES_DARK);

            // Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell hLeft = new PdfPCell();
            hLeft.setBorder(Rectangle.NO_BORDER);
            hLeft.addElement(new Paragraph("CES MMC — LAYİHƏ DAYANMA VƏ GÖZLƏMƏ PROTOKOLU", titleFont));
            hLeft.addElement(new Paragraph("Tikinti və Xüsusi Texnika İcarəsi Xidməti", subFont));
            headerTable.addCell(hLeft);

            PdfPCell hRight = new PdfPCell();
            hRight.setBorder(Rectangle.NO_BORDER);
            hRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(new Paragraph("Protokol №: DOWNTIME-" + dt.getId(), h2Font));
            hRight.addElement(new Paragraph("Layihə Kodu: " + p.getProjectCode(), labelFont));
            hRight.addElement(new Paragraph("Tarix: " + dt.getStartDate().format(DATE_FMT), subFont));
            headerTable.addCell(hRight);

            doc.add(headerTable);
            doc.add(new Paragraph(" "));

            // Məlumat bloku
            PdfPTable infoTable = new PdfPTable(2);
            infoTable.setWidthPercentage(100);
            infoTable.setWidths(new float[]{50, 50});

            String client = p.getRequest() != null ? nz(p.getRequest().getCompanyName()) : "—";
            String contact = p.getRequest() != null ? nz(p.getRequest().getContactPerson()) : "—";
            String region = p.getRequest() != null ? nz(p.getRequest().getRegion()) : "—";
            String endStr = dt.getEndDate() != null ? dt.getEndDate().format(DATE_FMT) : "Davam edir (Aktiv)";

            addKeyValueCell(infoTable, "Sifarişçi (Müştəri):", client, labelFont, valFont);
            addKeyValueCell(infoTable, "Məsul Şəxs:", contact, labelFont, valFont);
            addKeyValueCell(infoTable, "Layihə Ünvanı/Region:", region, labelFont, valFont);
            addKeyValueCell(infoTable, "Dayanma Statusu:", dt.getStatus().equals("RESOLVED") ? "Bərpa edildi" : "Dondurulub (Aktiv)", labelFont, valFont);
            addKeyValueCell(infoTable, "Dayanma Başlanğıcı:", dt.getStartDate().format(DATE_FMT), labelFont, valFont);
            addKeyValueCell(infoTable, "Bərpa / Yekun Tarix:", endStr, labelFont, valFont);
            addKeyValueCell(infoTable, "Səbəb Kateqoriyası:", translateReason(dt.getReasonType()), labelFont, valFont);
            addKeyValueCell(infoTable, "Maliyyə Şərti:", dt.isPaid() ? "Ödənişli Gözləmə (Standby: " + fmt(dt.getStandbyRate()) + " ₼)" : "Ödənişsiz Dayanma (Güzəşt)", labelFont, valFont);

            doc.add(infoTable);
            doc.add(new Paragraph(" "));

            // Təsvir bloku
            PdfPTable descTable = new PdfPTable(1);
            descTable.setWidthPercentage(100);
            PdfPCell descCell = new PdfPCell();
            descCell.setBackgroundColor(CES_BG);
            descCell.setPadding(10f);
            descCell.setBorderColor(CES_LINE);
            descCell.addElement(new Paragraph("Hadisənin / Dayanmanın Təsviri və Səbəbi:", labelFont));
            descCell.addElement(new Paragraph(nz(dt.getReasonDescription()), valFont));
            if (dt.getResolvedNotes() != null && !dt.getResolvedNotes().isBlank()) {
                descCell.addElement(new Paragraph(" "));
                descCell.addElement(new Paragraph("Bərpa Qeydləri və Qərarlar:", labelFont));
                descCell.addElement(new Paragraph(dt.getResolvedNotes(), valFont));
            }
            descTable.addCell(descCell);
            doc.add(descTable);
            doc.add(new Paragraph(" "));

            // Signatures
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setWidths(new float[]{50, 50});
            sigTable.addCell(createSignatureBlock("İCARƏYƏ VERƏN:", "CES MMC Nümayəndəsi\nİmza: __________________\nM.Y.", bfBold, bfRegular));
            sigTable.addCell(createSignatureBlock("İCARƏYƏ GÖTÜRƏN:", client + "\nSəlahiyyətli Nümayəndə: __________________\nİmza: __________________\nM.Y.", bfBold, bfRegular));
            doc.add(sigTable);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Dayanma protokolu PDF generasiya xətası: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generasiya edilə bilmədi: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] generateEquipmentSwapActPdf(Long swapId) {
        ProjectEquipmentSwap swap = swapRepository.findById(swapId)
                .orElseThrow(() -> new ResourceNotFoundException("Əvəzləmə qeydiyyatı", swapId));
        Project p = swap.getProject();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bfRegular = loadBaseFont(false);
            BaseFont bfBold    = loadBaseFont(true);

            Font titleFont   = font(bfBold, 15, Font.BOLD, CES_DARK);
            Font subFont     = font(bfRegular, 9.5f, Font.NORMAL, CES_MUTED);
            Font h2Font      = font(bfBold, 11, Font.BOLD, CES_DARK);
            Font labelFont   = font(bfBold, 9, Font.BOLD, CES_DARK);
            Font valFont     = font(bfRegular, 9, Font.NORMAL, CES_DARK);

            // Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell hLeft = new PdfPCell();
            hLeft.setBorder(Rectangle.NO_BORDER);
            hLeft.addElement(new Paragraph("CES MMC — TEXNİKA ƏVƏZLƏMƏ AKTI", titleFont));
            hLeft.addElement(new Paragraph("Sahədə Texnikanın Dəyişdirilməsi və Təhvil Protokolu", subFont));
            headerTable.addCell(hLeft);

            PdfPCell hRight = new PdfPCell();
            hRight.setBorder(Rectangle.NO_BORDER);
            hRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(new Paragraph("Akt №: SWAP-" + swap.getId(), h2Font));
            hRight.addElement(new Paragraph("Layihə Kodu: " + p.getProjectCode(), labelFont));
            hRight.addElement(new Paragraph("Tarix: " + swap.getSwapDate().format(DATE_FMT), subFont));
            headerTable.addCell(hRight);

            doc.add(headerTable);
            doc.add(new Paragraph(" "));

            // Comparison Table
            PdfPTable swapTable = new PdfPTable(2);
            swapTable.setWidthPercentage(100);
            swapTable.setWidths(new float[]{50, 50});

            // Column 1: Old Equipment
            PdfPCell oldCell = new PdfPCell();
            oldCell.setPadding(10f);
            oldCell.setBackgroundColor(new Color(254, 242, 242)); // light red
            oldCell.setBorderColor(new Color(252, 165, 165));
            oldCell.addElement(new Paragraph("🛑 ÇIXARILAN TEXNİKA (KÖHNƏ)", font(bfBold, 10.5f, Font.BOLD, new Color(185, 28, 28))));
            oldCell.addElement(new Paragraph("Ad / Model: " + swap.getOldEquipment().getName(), valFont));
            oldCell.addElement(new Paragraph("Dövlət Qeydiyyat №: " + nz(swap.getOldEquipment().getPlateNumber()), valFont));
            oldCell.addElement(new Paragraph("Son Motosaat/Sayğac: " + (swap.getOldEquipmentFinalCounter() != null ? swap.getOldEquipmentFinalCounter() + " saat" : "—"), valFont));
            oldCell.addElement(new Paragraph("Yeni Statusu: Təmir / Servis (" + swap.getOldEquipmentNextStatus() + ")", valFont));
            swapTable.addCell(oldCell);

            // Column 2: New Equipment
            PdfPCell newCell = new PdfPCell();
            newCell.setPadding(10f);
            newCell.setBackgroundColor(new Color(240, 253, 244)); // light green
            newCell.setBorderColor(new Color(134, 239, 172));
            newCell.addElement(new Paragraph("✅ TƏYİN EDİLƏN TEXNİKA (YENİ)", font(bfBold, 10.5f, Font.BOLD, new Color(21, 128, 61))));
            newCell.addElement(new Paragraph("Ad / Model: " + swap.getNewEquipment().getName(), valFont));
            newCell.addElement(new Paragraph("Dövlət Qeydiyyat №: " + nz(swap.getNewEquipment().getPlateNumber()), valFont));
            newCell.addElement(new Paragraph("İlkin Motosaat/Sayğac: " + (swap.getNewEquipmentInitialCounter() != null ? swap.getNewEquipmentInitialCounter() + " saat" : "—"), valFont));
            newCell.addElement(new Paragraph("Status: İcarədə (RENTED)", valFont));
            swapTable.addCell(newCell);

            doc.add(swapTable);
            doc.add(new Paragraph(" "));

            // Reason & Notes
            PdfPTable noteTable = new PdfPTable(1);
            noteTable.setWidthPercentage(100);
            PdfPCell noteCell = new PdfPCell();
            noteCell.setBackgroundColor(CES_BG);
            noteCell.setPadding(10f);
            noteCell.setBorderColor(CES_LINE);
            noteCell.addElement(new Paragraph("Əvəzləmənin Səbəbi:", labelFont));
            noteCell.addElement(new Paragraph(swap.getSwapReason(), valFont));
            if (swap.getNotes() != null && !swap.getNotes().isBlank()) {
                noteCell.addElement(new Paragraph(" "));
                noteCell.addElement(new Paragraph("Əlavə Qeydlər:", labelFont));
                noteCell.addElement(new Paragraph(swap.getNotes(), valFont));
            }
            noteTable.addCell(noteCell);
            doc.add(noteTable);
            doc.add(new Paragraph(" "));

            // Signatures
            String client = p.getRequest() != null ? nz(p.getRequest().getCompanyName()) : "Sifarişçi";
            PdfPTable sigTable = new PdfPTable(2);
            sigTable.setWidthPercentage(100);
            sigTable.setWidths(new float[]{50, 50});
            sigTable.addCell(createSignatureBlock("CES MMC KOORDİNATORU:", "Təhvil verdi: __________________\nİmza: __________________\nM.Y.", bfBold, bfRegular));
            sigTable.addCell(createSignatureBlock("SİFARİŞÇİ NÜMAYƏNDƏSİ:", client + "\nTəhvil aldı: __________________\nİmza: __________________\nM.Y.", bfBold, bfRegular));
            doc.add(sigTable);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Əvəzləmə aktı PDF generasiya xətası: {}", e.getMessage(), e);
            throw new RuntimeException("PDF generasiya edilə bilmədi: " + e.getMessage());
        }
    }

    private PdfPCell createSignatureBlock(String title, String details, BaseFont bfBold, BaseFont bfRegular) {
        PdfPCell c = new PdfPCell();
        c.setPadding(10f);
        c.setBorderColor(CES_LINE);
        c.addElement(new Paragraph(title, font(bfBold, 8.5f, Font.BOLD, CES_DARK)));
        c.addElement(new Paragraph("\n" + details, font(bfRegular, 8f, Font.NORMAL, CES_MUTED)));
        return c;
    }

    private void addKeyValueCell(PdfPTable table, String key, String val, Font kf, Font vf) {
        PdfPCell c = new PdfPCell();
        c.setPadding(5f);
        c.setBorderColor(CES_LINE);
        c.addElement(new Phrase(key + " ", kf));
        c.addElement(new Phrase(val, vf));
        table.addCell(c);
    }

    private String translateReason(String reason) {
        if (reason == null) return "Göstərilməyib";
        return switch (reason) {
            case "WEATHER" -> "🌪️ Hava Şəraiti / Fors-major";
            case "CUSTOMER_SITE" -> "🚧 Müştəri Səbəbli / Sahə Gözləməsi";
            case "TECHNICAL_BREAKDOWN" -> "🔧 Texniki Nasazlıq / Təmir";
            case "PAYMENT_DELAY" -> "💳 Ödəniş Gecikməsi";
            default -> "📋 " + reason;
        };
    }

    private BaseFont loadBaseFont(boolean bold) {
        String resource = bold ? "/fonts/DejaVuSans-Bold.ttf" : "/fonts/DejaVuSans.ttf";
        try (var is = ProjectIncidentPdfService.class.getResourceAsStream(resource)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String fakeName = bold ? "DejaVuSans-Bold.ttf" : "DejaVuSans.ttf";
                return BaseFont.createFont(fakeName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, bytes, null);
            }
        } catch (Exception e) {
            log.warn("Font resursdan yüklənmədi: {}", e.getMessage());
        }
        try {
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Font font(BaseFont bf, float size, int style, Color color) {
        Font f = new Font(bf, size, style);
        f.setColor(color);
        return f;
    }

    private static String fmt(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static String nz(String s) {
        return s != null && !s.isBlank() ? s : "—";
    }
}
