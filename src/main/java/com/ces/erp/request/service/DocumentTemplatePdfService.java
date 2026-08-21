package com.ces.erp.request.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.TechRequestRepository;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentTemplatePdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private static final Color CES_DARK  = new Color(15, 23, 42);     // #0f172a
    private static final Color CES_GOLD  = new Color(202, 138, 4);    // #ca8a04
    private static final Color CES_MUTED = new Color(100, 116, 139);  // #64748b
    private static final Color CES_BG    = new Color(248, 250, 252);  // #f8fafc
    private static final Color CES_LINE  = new Color(226, 232, 240);  // #e2e8f0

    private final TechRequestRepository requestRepository;
    private final CoordinatorPlanRepository planRepository;

    @Transactional(readOnly = true)
    public byte[] generateHandoverActPdf(Long requestId) {
        TechRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Sorğu", requestId));
        CoordinatorPlan plan = planRepository.findByRequestId(requestId).orElse(null);

        Equipment eq = plan != null && plan.getSelectedEquipment() != null
                ? plan.getSelectedEquipment()
                : request.getSelectedEquipment();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bfRegular = loadBaseFont(false);
            BaseFont bfBold    = loadBaseFont(true);

            Font titleFont   = font(bfBold, 16, Font.BOLD, CES_DARK);
            Font subFont     = font(bfRegular, 10, Font.NORMAL, CES_MUTED);
            Font h2Font      = font(bfBold, 11, Font.BOLD, CES_DARK);
            Font labelFont   = font(bfBold, 9, Font.BOLD, CES_DARK);
            Font valFont     = font(bfRegular, 9, Font.NORMAL, CES_DARK);
            Font smallFont   = font(bfRegular, 8, Font.NORMAL, CES_MUTED);

            // Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell hLeft = new PdfPCell();
            hLeft.setBorder(Rectangle.NO_BORDER);
            hLeft.addElement(new Paragraph("CES MMC — TƏHVİL-TƏSLİM AKTI", titleFont));
            hLeft.addElement(new Paragraph("Tikinti və Xüsusi Texnika İcarəsi Xidməti", subFont));
            headerTable.addCell(hLeft);

            PdfPCell hRight = new PdfPCell();
            hRight.setBorder(Rectangle.NO_BORDER);
            hRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(new Paragraph("Akt №: ACT-" + request.getRequestCode(), h2Font));
            hRight.addElement(new Paragraph("Tarix: " + LocalDate.now().format(DATE_FMT), subFont));
            headerTable.addCell(hRight);

            doc.add(headerTable);
            doc.add(new Paragraph(" ", font(bfRegular, 6, Font.NORMAL, Color.WHITE)));

            // Horizontal Line
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            PdfPCell lc = new PdfPCell(new Phrase(""));
            lc.setBorder(Rectangle.BOTTOM);
            lc.setBorderColor(CES_GOLD);
            lc.setBorderWidth(2);
            line.addCell(lc);
            doc.add(line);
            doc.add(new Paragraph(" ", font(bfRegular, 8, Font.NORMAL, Color.WHITE)));

            // 1. Tərəflər
            PdfPTable parties = new PdfPTable(2);
            parties.setWidthPercentage(100);
            parties.setSpacingBefore(8f);
            parties.setSpacingAfter(8f);

            PdfPCell p1 = createBoxCell("TƏHVİL VERƏN (İCARƏYƏ VERƏN)", bfBold, bfRegular,
                    "Şirkət: CES MMC\n" +
                    "VÖEN: 1405829631\n" +
                    "Ünvan: Bakı ş., Heydər Əliyev pr. 115\n" +
                    "Əlaqə: +994 12 555 00 00 / info@ces.com.az");

            String clientInfo = String.format(
                    "Şirkət: %s\n" +
                    "Əlaqədar şəxs: %s\n" +
                    "Telefon: %s\n" +
                    "Bölgə / Ünvan: %s",
                    request.getCompanyName(),
                    nz(request.getContactPerson()),
                    nz(request.getContactPhone()),
                    nz(request.getRegion())
            );
            PdfPCell p2 = createBoxCell("TƏHVİL ALAN (SİFARİŞÇİ / İCARƏÇİ)", bfBold, bfRegular, clientInfo);

            parties.addCell(p1);
            parties.addCell(p2);
            doc.add(parties);

            // 2. Texnika Məlumatları
            PdfPTable eqTable = new PdfPTable(4);
            eqTable.setWidthPercentage(100);
            eqTable.setWidths(new float[]{25, 25, 25, 25});
            eqTable.setSpacingBefore(6f);
            eqTable.setSpacingAfter(8f);

            addHeaderCell(eqTable, "Texnika Adı / Kodu", bfBold);
            addHeaderCell(eqTable, "Dövlət Qeydiyyat №", bfBold);
            addHeaderCell(eqTable, "Buraxılış İli / Brend", bfBold);
            addHeaderCell(eqTable, "Təhvil Sayğacı", bfBold);

            String eqTitle = eq != null ? (eq.getName() + " (" + eq.getEquipmentCode() + ")") : "Texnika təyin edilməyib";
            String plate = eq != null ? nz(eq.getPlateNumber()) : "—";
            String brandYear = eq != null ? (nz(eq.getBrand()) + " / " + (eq.getManufactureYear() != null ? eq.getManufactureYear() : "—")) : "—";
            String counter = eq != null && eq.getHourKmCounter() != null ? eq.getHourKmCounter() + " Saat/KM" : "0 Saat/KM";

            addValueCell(eqTable, eqTitle, bfRegular);
            addValueCell(eqTable, plate, bfRegular);
            addValueCell(eqTable, brandYear, bfRegular);
            addValueCell(eqTable, counter, bfRegular);

            doc.add(new Paragraph("1. TƏHVİL VERİLƏN TEXNİKA VƏ GÖSTƏRİCİLƏR", h2Font));
            doc.add(eqTable);

            // 3. İcra və Şərtlər
            PdfPTable termsTable = new PdfPTable(2);
            termsTable.setWidthPercentage(100);
            termsTable.setWidths(new float[]{50, 50});
            termsTable.setSpacingBefore(6f);
            termsTable.setSpacingAfter(10f);

            String opName = plan != null && plan.getOperator() != null
                    ? (plan.getOperator().getFirstName() + " " + plan.getOperator().getLastName())
                    : "Sifarişçi tərəfindən";

            String startDateStr = plan != null && plan.getStartDate() != null
                    ? plan.getStartDate().format(DATE_FMT)
                    : (request.getRequestDate() != null ? request.getRequestDate().format(DATE_FMT) : "—");
            String endDateStr = plan != null && plan.getEndDate() != null
                    ? plan.getEndDate().format(DATE_FMT)
                    : "—";

            String dates = startDateStr + " — " + endDateStr + " (" + nz(request.getDayCount() != null ? request.getDayCount().toString() : null) + " gün)";

            addKeyValueCell(termsTable, "Təyin edilmiş Operator:", opName, labelFont, valFont);
            addKeyValueCell(termsTable, "İcarə Müddəti:", dates, labelFont, valFont);
            addKeyValueCell(termsTable, "Ödəniş Üsulu:", request.getPaymentMethod() != null ? request.getPaymentMethod() : "Bank köçürməsi", labelFont, valFont);
            addKeyValueCell(termsTable, "Daşınma (Mobilizasiya):", request.isTransportationRequired() ? "CES MMC tərəfindən" : "Sifarişçi tərəfindən", labelFont, valFont);

            doc.add(new Paragraph("2. İCARƏ VƏ İSTİSMAR ŞƏRTLƏRİ", h2Font));
            doc.add(termsTable);

            // Akt Bəyanatı
            Paragraph pStatement = new Paragraph(
                    "Bununla təsdiq edilir ki, yuxarıda göstərilən texnika tam işlək, saz vəziyyətdə və vizual qüsursuz olaraq " +
                    "Sifarişçiyə təhvil verildi. Sifarişçi texnikanın cari sayğac göstəricisini və texniki vəziyyətini yoxlayaraq qəbul etdi.",
                    smallFont);
            pStatement.setSpacingAfter(16f);
            doc.add(pStatement);

            // İmza Bloku
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{50, 50});
            signTable.setSpacingBefore(12f);

            PdfPCell s1 = createSignCell("TƏHVİL VERDİ (CES MMC):", "İmza: _______________________", "M.Y.");
            PdfPCell s2 = createSignCell("TƏHVİL ALDI (SİFARİŞÇİ):", "İmza: _______________________", "M.Y.");

            signTable.addCell(s1);
            signTable.addCell(s2);
            doc.add(signTable);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Təhvil-təslim aktı PDF generasiya xətası", e);
            throw new BusinessException("Təhvil-təslim aktı PDF faylı yaradıla bilmədi: " + e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public byte[] generatePriceProtocolPdf(Long requestId) {
        TechRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Sorğu", requestId));
        CoordinatorPlan plan = planRepository.findByRequestId(requestId).orElse(null);

        Equipment eq = plan != null && plan.getSelectedEquipment() != null
                ? plan.getSelectedEquipment()
                : request.getSelectedEquipment();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
            PdfWriter.getInstance(doc, out);
            doc.open();

            BaseFont bfRegular = loadBaseFont(false);
            BaseFont bfBold    = loadBaseFont(true);

            Font titleFont   = font(bfBold, 15, Font.BOLD, CES_DARK);
            Font subFont     = font(bfRegular, 10, Font.NORMAL, CES_MUTED);
            Font h2Font      = font(bfBold, 11, Font.BOLD, CES_DARK);
            Font valFont     = font(bfRegular, 9, Font.NORMAL, CES_DARK);
            Font boldValFont = font(bfBold, 9, Font.BOLD, CES_DARK);
            Font totalFont   = font(bfBold, 12, Font.BOLD, Color.WHITE);

            // Header Table
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setWidths(new float[]{60, 40});

            PdfPCell hLeft = new PdfPCell();
            hLeft.setBorder(Rectangle.NO_BORDER);
            hLeft.addElement(new Paragraph("CES MMC — QİYMƏT RAZILAŞMA PROTOKOLU", titleFont));
            hLeft.addElement(new Paragraph("Müqaviləyə Əlavə №1", subFont));
            headerTable.addCell(hLeft);

            PdfPCell hRight = new PdfPCell();
            hRight.setBorder(Rectangle.NO_BORDER);
            hRight.setHorizontalAlignment(Element.ALIGN_RIGHT);
            hRight.addElement(new Paragraph("Protokol: PRT-" + request.getRequestCode(), h2Font));
            hRight.addElement(new Paragraph("Tarix: " + LocalDate.now().format(DATE_FMT), subFont));
            headerTable.addCell(hRight);

            doc.add(headerTable);

            // Horizontal Line
            PdfPTable line = new PdfPTable(1);
            line.setWidthPercentage(100);
            line.setSpacingBefore(4f);
            line.setSpacingAfter(8f);
            PdfPCell lc = new PdfPCell(new Phrase(""));
            lc.setBorder(Rectangle.BOTTOM);
            lc.setBorderColor(CES_GOLD);
            lc.setBorderWidth(2);
            line.addCell(lc);
            doc.add(line);

            // Tərəflər
            Paragraph pIntro = new Paragraph(
                    String.format("Bu Protokol bir tərəfdən 'CES' MMC (İcarəyə verən) ilə digər tərəfdən '%s' (Sifarişçi) " +
                                  "arasında bağlanmış müqaviləyə əsasən aşağıdakı xidmət və qiymət şərtlərini təsdiq edir:",
                            request.getCompanyName()), valFont);
            pIntro.setSpacingAfter(10f);
            doc.add(pIntro);

            // Qiymət Cədvəli
            PdfPTable table = new PdfPTable(5);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{35, 15, 15, 15, 20});
            table.setSpacingBefore(6f);
            table.setSpacingAfter(10f);

            addHeaderCell(table, "Xidmət / Texnikanın Təsviri", bfBold);
            addHeaderCell(table, "Ölçü Vahidi", bfBold);
            addHeaderCell(table, "Miqdar", bfBold);
            addHeaderCell(table, "Vahid Qiymət", bfBold);
            addHeaderCell(table, "Məbləğ (AZN)", bfBold);

            String eqDesc = eq != null ? (eq.getName() + " (" + eq.getEquipmentCode() + ")") : "Texnika icarəsi";
            BigDecimal agreedEq = request.getAgreedEquipmentPrice() != null
                    ? request.getAgreedEquipmentPrice()
                    : (plan != null && plan.getCustomerEquipmentPrice() != null ? plan.getCustomerEquipmentPrice() : BigDecimal.ZERO);
            BigDecimal agreedTr = request.getAgreedTransportPrice() != null ? request.getAgreedTransportPrice() : BigDecimal.ZERO;
            BigDecimal agreedTotal = request.getAgreedTotalPrice() != null ? request.getAgreedTotalPrice() : agreedEq.add(agreedTr);

            addValueCell(table, eqDesc + " — icarə haqqı", bfRegular);
            addValueCell(table, "Gün", bfRegular);
            addValueCell(table, String.valueOf(request.getDayCount() != null ? request.getDayCount() : 1), bfRegular);
            addValueCell(table, fmt(agreedEq) + " ₼", bfRegular);
            addValueCell(table, fmt(agreedEq) + " ₼", bfBold);

            if (agreedTr.compareTo(BigDecimal.ZERO) > 0) {
                addValueCell(table, "Texnikanın daşınması və mobilizasiya xərci", bfRegular);
                addValueCell(table, "Xidmət", bfRegular);
                addValueCell(table, "1", bfRegular);
                addValueCell(table, fmt(agreedTr) + " ₼", bfRegular);
                addValueCell(table, fmt(agreedTr) + " ₼", bfBold);
            }

            // Total Row
            PdfPCell totLabel = new PdfPCell(new Phrase("YEKUN RAZILAŞDIRILMIŞ MƏBLƏĞ:", totalFont));
            totLabel.setColspan(4);
            totLabel.setBackgroundColor(CES_DARK);
            totLabel.setPadding(8f);
            totLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(totLabel);

            PdfPCell totVal = new PdfPCell(new Phrase(fmt(agreedTotal) + " AZN", totalFont));
            totVal.setBackgroundColor(CES_DARK);
            totVal.setPadding(8f);
            totVal.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(totVal);

            doc.add(table);

            // Şərtlər
            String startStr = plan != null && plan.getStartDate() != null
                    ? plan.getStartDate().format(DATE_FMT)
                    : (request.getRequestDate() != null ? request.getRequestDate().format(DATE_FMT) : "—");

            Paragraph pTerms = new Paragraph(
                    "Ödəniş Şərtləri: Ödəniş " + (request.getPaymentMethod() != null ? request.getPaymentMethod() : "Bank köçürməsi") + " yolu ilə icra edilir.\n" +
                    "İcarə Müddəti: " + startStr + " tarixindən etibarən qüvvədədir.",
                    valFont);
            pTerms.setSpacingAfter(18f);
            doc.add(pTerms);

            // İmza Bloku
            PdfPTable signTable = new PdfPTable(2);
            signTable.setWidthPercentage(100);
            signTable.setWidths(new float[]{50, 50});
            signTable.setSpacingBefore(14f);

            PdfPCell s1 = createSignCell("İCARƏYƏ VERƏN (CES MMC):", "İmza: _______________________", "M.Y.");
            PdfPCell s2 = createSignCell("SİFARİŞÇİ:", "İmza: _______________________", "M.Y.");

            signTable.addCell(s1);
            signTable.addCell(s2);
            doc.add(signTable);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Qiymət protokolu PDF generasiya xətası", e);
            throw new BusinessException("Qiymət protokolu PDF faylı yaradıla bilmədi: " + e.getMessage());
        }
    }

    private PdfPCell createBoxCell(String title, BaseFont bfBold, BaseFont bfRegular, String content) {
        PdfPCell cell = new PdfPCell();
        cell.setPadding(8f);
        cell.setBackgroundColor(CES_BG);
        cell.setBorderColor(CES_LINE);
        cell.setBorderWidth(1f);

        Paragraph pTitle = new Paragraph(title, font(bfBold, 9, Font.BOLD, CES_GOLD));
        pTitle.setSpacingAfter(4f);
        cell.addElement(pTitle);

        Paragraph pContent = new Paragraph(content, font(bfRegular, 8.5f, Font.NORMAL, CES_DARK));
        pContent.setLeading(12f);
        cell.addElement(pContent);
        return cell;
    }

    private PdfPCell createSignCell(String title, String signLine, String stamp) {
        PdfPCell c = new PdfPCell();
        c.setBorder(Rectangle.BOX);
        c.setBorderColor(CES_LINE);
        c.setPadding(10f);
        c.setBackgroundColor(CES_BG);

        c.addElement(new Paragraph(title, new Font(Font.HELVETICA, 9, Font.BOLD, CES_DARK)));
        c.addElement(new Paragraph("\n\n" + signLine + "\n\n" + stamp, new Font(Font.HELVETICA, 8.5f, Font.NORMAL, CES_MUTED)));
        return c;
    }

    private void addHeaderCell(PdfPTable table, String text, BaseFont bfBold) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(bfBold, 8.5f, Font.BOLD, Color.WHITE)));
        c.setBackgroundColor(CES_DARK);
        c.setPadding(6f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c);
    }

    private void addValueCell(PdfPTable table, String text, BaseFont bf) {
        PdfPCell c = new PdfPCell(new Phrase(text, font(bf, 8.5f, Font.NORMAL, CES_DARK)));
        c.setPadding(6f);
        c.setBorderColor(CES_LINE);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(c);
    }

    private void addKeyValueCell(PdfPTable table, String key, String val, Font kf, Font vf) {
        PdfPCell c = new PdfPCell();
        c.setPadding(5f);
        c.setBorderColor(CES_LINE);
        c.addElement(new Phrase(key + " ", kf));
        c.addElement(new Phrase(val, vf));
        table.addCell(c);
    }

    private BaseFont loadBaseFont(boolean bold) {
        String resource = bold ? "/fonts/DejaVuSans-Bold.ttf" : "/fonts/DejaVuSans.ttf";
        try (var is = DocumentTemplatePdfService.class.getResourceAsStream(resource)) {
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
