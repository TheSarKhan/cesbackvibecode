package com.ces.erp.hr.service;

import com.ces.erp.common.exception.BusinessException;
import com.ces.erp.common.exception.ResourceNotFoundException;
import com.ces.erp.hr.entity.PayrollEntry;
import com.ces.erp.hr.entity.PayrollPeriod;
import com.ces.erp.hr.repository.PayrollEntryRepository;
import com.ces.erp.hr.repository.PayrollPeriodRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PayrollPdfService {

    private final PayrollPeriodRepository periodRepository;
    private final PayrollEntryRepository entryRepository;

    private static final String[] AZ_MONTHS = {
        "Yanvar", "Fevral", "Mart", "Aprel", "May", "İyun",
        "İyul", "Avqust", "Sentyabr", "Oktyabr", "Noyabr", "Dekabr"
    };

    private static final Color HEADER_BG = new Color(30, 64, 120);
    private static final Color ACCENT = new Color(212, 117, 6);
    private static final Color LIGHT_GRAY = new Color(245, 245, 248);

    public byte[] generatePeriodReport(Long periodId) {
        PayrollPeriod p = periodRepository.findByIdAndDeletedFalse(periodId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll period", periodId));
        try {
            return buildPeriodPdf(p);
        } catch (Exception e) {
            log.error("Payroll period PDF generation failed", e);
            throw new BusinessException("PDF yaradıla bilmədi: " + e.getMessage());
        }
    }

    public byte[] generatePayslip(Long entryId) {
        PayrollEntry entry = entryRepository.findByIdAndDeletedFalse(entryId)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll entry", entryId));
        try {
            return buildPayslipPdf(entry);
        } catch (Exception e) {
            log.error("Payslip PDF generation failed", e);
            throw new BusinessException("Pay slip yaradıla bilmədi: " + e.getMessage());
        }
    }

    // ─── Aylıq cədvəl ──
    private byte[] buildPeriodPdf(PayrollPeriod p) throws Exception {
        BaseFont bf = loadBaseFont(false);
        BaseFont bfBold = loadBaseFont(true);

        Document doc = new Document(PageSize.A4.rotate(), 24, 24, 28, 28);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font title = font(bfBold, 14, Font.BOLD, HEADER_BG);
        Font subtitle = font(bf, 10, Font.NORMAL, Color.DARK_GRAY);
        Font tableHeader = font(bfBold, 8, Font.BOLD, Color.WHITE);
        Font cell = font(bf, 8, Font.NORMAL, Color.BLACK);
        Font cellBold = font(bfBold, 8, Font.BOLD, Color.BLACK);

        String monthName = AZ_MONTHS[p.getMonth() - 1];

        Paragraph header = new Paragraph(monthName + " " + p.getYear() + " — Əməkhaqqı Cədvəli", title);
        header.setAlignment(Element.ALIGN_CENTER);
        doc.add(header);

        Paragraph sub = new Paragraph(
                "Status: " + p.getStatus() + "    |    İş günü: " + p.getWorkingDaysInMonth()
                + "    |    İşçi sayı: " + p.getEntries().stream().filter(e -> !e.isDeleted()).count(),
                subtitle);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(10);
        doc.add(sub);

        // Cədvəl başlıqları
        String[] headers = {
                "№", "İşçi (Ad Soyad)", "FİN", "Vəzifə",
                "Gross", "İş günü",
                "Gəlir vergisi", "Pensiya 3-10%", "İSH 0.5%", "İTSH 2-0.5%",
                "Cəmi tutulan", "Net (Ödəniləcək)",
                "İGÖ Pensiya", "İGÖ İSH", "İGÖ İTSH"
        };
        float[] widths = {1.5f, 6f, 4f, 4.5f, 4f, 2.5f, 4f, 4.5f, 3.5f, 4f, 4.5f, 5f, 4.5f, 3.5f, 4f};
        PdfPTable table = new PdfPTable(headers.length);
        table.setWidthPercentage(100);
        table.setWidths(widths);
        table.setHeaderRows(1);

        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, tableHeader));
            c.setBackgroundColor(HEADER_BG);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setPadding(4);
            c.setBorderColor(Color.WHITE);
            table.addCell(c);
        }

        int idx = 1;
        BigDecimal totalGross = BigDecimal.ZERO;
        BigDecimal totalNet = BigDecimal.ZERO;
        BigDecimal totalEmpDed = BigDecimal.ZERO;
        BigDecimal totalEmpContrib = BigDecimal.ZERO;
        BigDecimal totalIncomeTax = BigDecimal.ZERO;

        for (PayrollEntry e : p.getEntries()) {
            if (e.isDeleted()) continue;
            boolean alt = idx % 2 == 0;
            Color bg = alt ? LIGHT_GRAY : Color.WHITE;

            addCell(table, String.valueOf(idx++), cell, bg, Element.ALIGN_CENTER);
            addCell(table, e.getEmployeeFullName(), cell, bg, Element.ALIGN_LEFT);
            addCell(table, e.getEmployee() != null && e.getEmployee().getFin() != null ? e.getEmployee().getFin() : "—", cell, bg, Element.ALIGN_CENTER);
            addCell(table, nz(e.getPositionName()), cell, bg, Element.ALIGN_LEFT);
            addCell(table, fmt(e.getGrossTotal()), cell, bg, Element.ALIGN_RIGHT);
            addCell(table, e.getActualDaysWorked() + "/" + e.getWorkingDaysInMonth(), cell, bg, Element.ALIGN_CENTER);
            addCell(table, fmt(e.getIncomeTax()), cell, bg, Element.ALIGN_RIGHT);
            addCell(table, fmt(e.getEmployeePension()), cell, bg, Element.ALIGN_RIGHT);
            addCell(table, fmt(e.getEmployeeUnemployment()), cell, bg, Element.ALIGN_RIGHT);
            addCell(table, fmt(e.getEmployeeMedical()), cell, bg, Element.ALIGN_RIGHT);
            addCell(table, fmt(e.getTotalDeductions()), cellBold, bg, Element.ALIGN_RIGHT);
            addCell(table, fmt(e.getNetPay()), cellBold, new Color(232, 252, 235), Element.ALIGN_RIGHT);
            addCell(table, fmt(e.getEmployerPension()), cell, bg, Element.ALIGN_RIGHT);
            addCell(table, fmt(e.getEmployerUnemployment()), cell, bg, Element.ALIGN_RIGHT);
            addCell(table, fmt(e.getEmployerMedical()), cell, bg, Element.ALIGN_RIGHT);

            totalGross = totalGross.add(nzBig(e.getGrossTotal()));
            totalNet = totalNet.add(nzBig(e.getNetPay()));
            totalEmpDed = totalEmpDed.add(nzBig(e.getTotalDeductions()));
            totalEmpContrib = totalEmpContrib.add(nzBig(e.getTotalEmployerContributions()));
            totalIncomeTax = totalIncomeTax.add(nzBig(e.getIncomeTax()));
        }

        // Cəmi sətir
        Color totalBg = new Color(255, 247, 230);
        addTotalCell(table, "CƏMİ", tableHeader, ACCENT, 4);
        addTotalCell(table, fmt(totalGross), cellBold, totalBg, 1);
        addTotalCell(table, "—", cellBold, totalBg, 1);
        addTotalCell(table, fmt(totalIncomeTax), cellBold, totalBg, 1);
        addTotalCell(table, "—", cellBold, totalBg, 3);
        addTotalCell(table, fmt(totalEmpDed), cellBold, totalBg, 1);
        addTotalCell(table, fmt(totalNet), cellBold, new Color(232, 252, 235), 1);
        addTotalCell(table, fmt(totalEmpContrib), cellBold, totalBg, 3);

        doc.add(table);

        // Xülasə
        doc.add(new Paragraph(" "));
        Paragraph summary = new Paragraph();
        summary.add(new Chunk("Cəmi gross: ", cellBold));
        summary.add(new Chunk(fmt(totalGross) + " ₼     ", cell));
        summary.add(new Chunk("Cəmi tutulan: ", cellBold));
        summary.add(new Chunk(fmt(totalEmpDed) + " ₼     ", cell));
        summary.add(new Chunk("Cəmi ödəniləcək: ", cellBold));
        summary.add(new Chunk(fmt(totalNet) + " ₼     ", cell));
        summary.add(new Chunk("Şirkət əlavə xərci: ", cellBold));
        summary.add(new Chunk(fmt(totalEmpContrib) + " ₼", cell));
        doc.add(summary);

        // Footer
        doc.add(new Paragraph(" "));
        Paragraph footer = new Paragraph(
                "Sənəd avtomatik yaradılıb — " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")),
                font(bf, 8, Font.ITALIC, Color.GRAY));
        footer.setAlignment(Element.ALIGN_RIGHT);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    // ─── Pay slip (1 işçi) ──
    private byte[] buildPayslipPdf(PayrollEntry e) throws Exception {
        BaseFont bf = loadBaseFont(false);
        BaseFont bfBold = loadBaseFont(true);

        Document doc = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(doc, out);
        doc.open();

        Font title = font(bfBold, 16, Font.BOLD, HEADER_BG);
        Font label = font(bfBold, 10, Font.BOLD, Color.DARK_GRAY);
        Font value = font(bf, 10, Font.NORMAL, Color.BLACK);
        Font small = font(bf, 9, Font.NORMAL, Color.GRAY);

        PayrollPeriod p = e.getPeriod();
        String monthName = AZ_MONTHS[p.getMonth() - 1];

        Paragraph header = new Paragraph("Aylıq Əməkhaqqı Hesabı", title);
        header.setAlignment(Element.ALIGN_CENTER);
        doc.add(header);
        Paragraph sub = new Paragraph(monthName + " " + p.getYear(), font(bf, 12, Font.NORMAL, Color.DARK_GRAY));
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(20);
        doc.add(sub);

        // İşçi məlumatları
        PdfPTable info = new PdfPTable(2);
        info.setWidthPercentage(100);
        info.setSpacingAfter(15);
        info.setWidths(new float[]{1f, 2f});
        addInfoRow(info, "Ad Soyad:", e.getEmployeeFullName(), label, value);
        addInfoRow(info, "Vəzifə:", nz(e.getPositionName()), label, value);
        if (e.getEmployee() != null && e.getEmployee().getFin() != null) {
            addInfoRow(info, "FİN:", e.getEmployee().getFin(), label, value);
        }
        if (e.getEmployee() != null && e.getEmployee().getEmployeeCode() != null) {
            addInfoRow(info, "İşçi kodu:", e.getEmployee().getEmployeeCode(), label, value);
        }
        addInfoRow(info, "İş günü:", e.getActualDaysWorked() + " / " + e.getWorkingDaysInMonth(), label, value);
        doc.add(info);

        // Gəlirlər
        PdfPTable earn = section(bfBold, "GƏLİRLƏR");
        addLine(earn, "Əsas əməkhaqqı (gross)", e.getBaseSalary(), label, value);
        if (positive(e.getOvertimePay()))   addLine(earn, "Saatlıq əlavə",      e.getOvertimePay(), label, value);
        if (positive(e.getBonus()))         addLine(earn, "Mükafat",             e.getBonus(), label, value);
        if (positive(e.getVacationPay()))   addLine(earn, "Məzuniyyət ödənişi", e.getVacationPay(), label, value);
        if (positive(e.getPenalty()))       addLine(earn, "Cərimə (-)",          e.getPenalty().negate(), label, value);
        addTotalLine(earn, "CƏMİ HESABLANIB", e.getGrossTotal(), bfBold);
        doc.add(earn);
        doc.add(new Paragraph(" "));

        // Tutulanlar
        PdfPTable ded = section(bfBold, "İŞÇİDƏN TUTULANLAR");
        addLine(ded, "Gəlir vergisi",         e.getIncomeTax(), label, value);
        addLine(ded, "Pensiya Fondu (3% / 10%)", e.getEmployeePension(), label, value);
        addLine(ded, "İşsizlik sığ. (0.5%)",  e.getEmployeeUnemployment(), label, value);
        addLine(ded, "Tibbi sığ. (2% / 0.5%)", e.getEmployeeMedical(), label, value);
        addTotalLine(ded, "CƏMİ TUTULMUŞDUR", e.getTotalDeductions(), bfBold);
        doc.add(ded);
        doc.add(new Paragraph(" "));

        // Net
        PdfPTable net = new PdfPTable(2);
        net.setWidthPercentage(100);
        net.setWidths(new float[]{2f, 1f});
        PdfPCell nLabel = new PdfPCell(new Phrase("ÖDƏNİLMƏLİ MƏBLƏĞ", font(bfBold, 13, Font.BOLD, Color.WHITE)));
        nLabel.setBackgroundColor(new Color(34, 139, 34));
        nLabel.setPadding(10);
        nLabel.setHorizontalAlignment(Element.ALIGN_LEFT);
        nLabel.setBorder(Rectangle.NO_BORDER);
        net.addCell(nLabel);
        PdfPCell nValue = new PdfPCell(new Phrase(fmt(e.getNetPay()) + " ₼", font(bfBold, 13, Font.BOLD, Color.WHITE)));
        nValue.setBackgroundColor(new Color(34, 139, 34));
        nValue.setPadding(10);
        nValue.setHorizontalAlignment(Element.ALIGN_RIGHT);
        nValue.setBorder(Rectangle.NO_BORDER);
        net.addCell(nValue);
        doc.add(net);

        // İşəgötürən
        doc.add(new Paragraph(" "));
        PdfPTable emp = section(bfBold, "İŞƏGÖTÜRƏN TƏRƏFİNDƏN");
        addLine(emp, "Pensiya Fondu (22% / 15%)", e.getEmployerPension(), label, value);
        addLine(emp, "İşsizlik sığ. (0.5%)",      e.getEmployerUnemployment(), label, value);
        addLine(emp, "Tibbi sığ. (2% / 0.5%)",     e.getEmployerMedical(), label, value);
        addTotalLine(emp, "ŞİRKƏT ƏLAVƏ XƏRCİ", e.getTotalEmployerContributions(), bfBold);
        doc.add(emp);

        doc.add(new Paragraph(" "));
        Paragraph footer = new Paragraph(
                "Yaradıldı: " + java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")), small);
        footer.setAlignment(Element.ALIGN_RIGHT);
        doc.add(footer);

        doc.close();
        return out.toByteArray();
    }

    // ─── Köməkçilər ──
    private PdfPTable section(BaseFont bfBold, String title) throws Exception {
        PdfPTable t = new PdfPTable(2);
        t.setWidthPercentage(100);
        t.setWidths(new float[]{2f, 1f});
        PdfPCell c = new PdfPCell(new Phrase(title, font(bfBold, 11, Font.BOLD, Color.WHITE)));
        c.setColspan(2);
        c.setBackgroundColor(HEADER_BG);
        c.setPadding(6);
        c.setBorder(Rectangle.NO_BORDER);
        t.addCell(c);
        return t;
    }

    private void addLine(PdfPTable t, String name, BigDecimal amount, Font label, Font value) {
        PdfPCell l = new PdfPCell(new Phrase(name, label));
        l.setPadding(5);
        l.setBackgroundColor(LIGHT_GRAY);
        l.setBorderColor(Color.LIGHT_GRAY);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(fmt(amount) + " ₼", value));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(5);
        v.setBorderColor(Color.LIGHT_GRAY);
        t.addCell(v);
    }

    private void addTotalLine(PdfPTable t, String name, BigDecimal amount, BaseFont bfBold) {
        Font tf = font(bfBold, 11, Font.BOLD, Color.WHITE);
        PdfPCell l = new PdfPCell(new Phrase(name, tf));
        l.setPadding(7);
        l.setBackgroundColor(ACCENT);
        l.setBorder(Rectangle.NO_BORDER);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(fmt(amount) + " ₼", tf));
        v.setHorizontalAlignment(Element.ALIGN_RIGHT);
        v.setPadding(7);
        v.setBackgroundColor(ACCENT);
        v.setBorder(Rectangle.NO_BORDER);
        t.addCell(v);
    }

    private void addInfoRow(PdfPTable t, String name, String value, Font label, Font val) {
        PdfPCell l = new PdfPCell(new Phrase(name, label));
        l.setBorder(Rectangle.NO_BORDER);
        l.setPadding(4);
        t.addCell(l);
        PdfPCell v = new PdfPCell(new Phrase(value, val));
        v.setBorder(Rectangle.NO_BORDER);
        v.setPadding(4);
        t.addCell(v);
    }

    private void addCell(PdfPTable t, String text, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setHorizontalAlignment(align);
        c.setPadding(4);
        c.setBorderColor(Color.LIGHT_GRAY);
        t.addCell(c);
    }

    private void addTotalCell(PdfPTable t, String text, Font f, Color bg, int colspan) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setColspan(colspan);
        c.setHorizontalAlignment(Element.ALIGN_RIGHT);
        c.setPadding(6);
        t.addCell(c);
    }

    private BaseFont loadBaseFont(boolean bold) throws Exception {
        String resource = bold ? "/fonts/DejaVuSans-Bold.ttf" : "/fonts/DejaVuSans.ttf";
        try (var is = PayrollPdfService.class.getResourceAsStream(resource)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String fakeName = bold ? "DejaVuSans-Bold.ttf" : "DejaVuSans.ttf";
                return BaseFont.createFont(fakeName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, bytes, null);
            }
        } catch (Exception e) {
            log.warn("Font yüklənə bilmədi: {}", e.getMessage());
        }
        String[] winFonts = bold
                ? new String[]{"C:/Windows/Fonts/arialbd.ttf", "C:/Windows/Fonts/calibrib.ttf"}
                : new String[]{"C:/Windows/Fonts/arial.ttf", "C:/Windows/Fonts/calibri.ttf"};
        for (String path : winFonts) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {}
        }
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
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

    private static String nz(String v) { return v == null ? "—" : v; }
    private static BigDecimal nzBig(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static boolean positive(BigDecimal v) { return v != null && v.signum() > 0; }
}
