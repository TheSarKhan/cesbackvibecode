package com.ces.erp.accounting.service;

import com.ces.erp.accounting.entity.DocumentLine;
import com.ces.erp.accounting.entity.GeneratedDocument;
import com.ces.erp.config.dto.ConfigItemResponse;
import com.ces.erp.config.service.ConfigService;
import com.ces.erp.enums.DocumentType;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfGenerationService {

    private final ConfigService configService;

    @Value("${app.upload.dir:/opt/ces-uploads}")
    private String uploadDir;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Color HEADER_BG = new Color(30, 64, 120);
    private static final Color ACCENT    = new Color(212, 170, 50);
    private static final Color LIGHT_GRAY = new Color(245, 245, 248);
    private static final Color MID_GRAY   = new Color(200, 200, 210);

    // ─── Ana metod ────────────────────────────────────────────────────────────

    public String generateAndStore(GeneratedDocument doc) throws IOException {
        byte[] pdfBytes = switch (doc.getDocumentType()) {
            case HESAB_FAKTURA       -> generateHesabFaktura(doc);
            case TEHVIL_TESLIM_AKTI  -> generateTehvilTeslimAkti(doc);
            case ENGLISH_INVOICE     -> generateEnglishInvoice(doc);
        };

        Path dir = Paths.get(uploadDir, "generated-documents").toAbsolutePath();
        Files.createDirectories(dir);
        String fileName = UUID.randomUUID() + ".pdf";
        Path target = dir.resolve(fileName);
        Files.write(target, pdfBytes);
        return "generated-documents/" + fileName;
    }

    // ─── Font yüklə ───────────────────────────────────────────────────────────

    private BaseFont loadBaseFont(boolean bold) throws DocumentException, IOException {
        String fontName = bold ? "fonts/DejaVuSans-Bold.ttf" : "fonts/DejaVuSans.ttf";
        try {
            return BaseFont.createFont(fontName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception e) {
            // Fallback: Helvetica
            return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        }
    }

    private Font font(BaseFont bf, float size, int style, Color color) {
        Font f = new Font(bf, size, style);
        f.setColor(color);
        return f;
    }

    // ─── Şirkət məlumatlarını konfiqurasiyadan al ──────────────────────────────

    private String getCompanyConfig(String key) {
        List<ConfigItemResponse> items = configService.getActiveByCategory("COMPANY_INFO");
        return items.stream().filter(i -> key.equals(i.getKey()))
                .map(ConfigItemResponse::getValue).findFirst().orElse("");
    }

    private String getBankConfig(String key) {
        List<ConfigItemResponse> items = configService.getActiveByCategory("COMPANY_BANK_DETAILS");
        return items.stream().filter(i -> key.equals(i.getKey()))
                .map(ConfigItemResponse::getValue).findFirst().orElse("");
    }

    // ─── Hesab-Faktura ────────────────────────────────────────────────────────

    private byte[] generateHesabFaktura(GeneratedDocument doc) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(pdf, baos);
        pdf.open();

        BaseFont bf     = loadBaseFont(false);
        BaseFont bfBold = loadBaseFont(true);

        Font titleFont    = font(bfBold, 13, Font.BOLD, Color.WHITE);
        Font subtitleFont = font(bf,     9,  Font.NORMAL, Color.WHITE);
        Font sectionFont  = font(bfBold, 9,  Font.BOLD, HEADER_BG);
        Font bodyFont     = font(bf,     8,  Font.NORMAL, Color.DARK_GRAY);
        Font bodyBold     = font(bfBold, 8,  Font.BOLD, Color.DARK_GRAY);
        Font smallFont    = font(bf,     7,  Font.NORMAL, Color.GRAY);
        Font totalFont    = font(bfBold, 10, Font.BOLD, HEADER_BG);
        Font accentFont   = font(bfBold, 8,  Font.BOLD, ACCENT);

        // ─── Başlıq bloku ───
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3f, 2f});
        header.setSpacingAfter(12);

        PdfPCell leftCell = new PdfPCell();
        leftCell.setBackgroundColor(HEADER_BG);
        leftCell.setBorder(Rectangle.NO_BORDER);
        leftCell.setPadding(14);
        Paragraph leftP = new Paragraph();
        leftP.add(new Chunk("HESAB-FAKTURA\n", titleFont));
        leftP.add(new Chunk("№ " + doc.getDocumentNumber() + "   " +
                (doc.getDocumentDate() != null ? doc.getDocumentDate().format(DATE_FMT) : ""), subtitleFont));
        leftCell.addElement(leftP);
        header.addCell(leftCell);

        PdfPCell rightCell = new PdfPCell();
        rightCell.setBackgroundColor(ACCENT);
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setPadding(14);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph rightP = new Paragraph();
        rightP.setAlignment(Element.ALIGN_RIGHT);
        String compName = getCompanyConfig("COMPANY_NAME");
        rightP.add(new Chunk(compName.isEmpty() ? "CES MMC" : compName, font(bfBold, 11, Font.BOLD, Color.WHITE)));
        rightCell.addElement(rightP);
        header.addCell(rightCell);

        pdf.add(header);

        // ─── Satıcı / Alıcı bloku ───
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setWidths(new float[]{1f, 1f});
        parties.setSpacingAfter(10);

        parties.addCell(partyCell("SATICI (SUPPLIER)", getCompanyInfo(), sectionFont, bodyFont, true));
        parties.addCell(partyCell("ALICI (BUYER)", getCustomerInfo(doc), sectionFont, bodyFont, false));
        pdf.add(parties);

        // ─── Müqavilə məlumatları ───
        if (doc.getContractNumber() != null || doc.getContractDate() != null) {
            Paragraph contract = new Paragraph();
            contract.setSpacingBefore(2);
            contract.setSpacingAfter(6);
            if (doc.getContractNumber() != null)
                contract.add(new Chunk("Müqavilə №: " + doc.getContractNumber() + "   ", bodyBold));
            if (doc.getContractDate() != null)
                contract.add(new Chunk("Tarix: " + doc.getContractDate().format(DATE_FMT), bodyFont));
            pdf.add(contract);
        }

        // ─── Xidmət cədvəli ───
        pdf.add(buildLineTable(doc.getLines(), bf, bfBold));

        // ─── Cəmi bloku ───
        pdf.add(buildTotalsTable(doc, bf, bfBold, totalFont, bodyFont, smallFont));

        // ─── Bank məlumatları ───
        pdf.add(buildBankSection(bf, bfBold, sectionFont, bodyFont, smallFont, accentFont));

        // ─── Qeyd ───
        if (doc.getNotes() != null && !doc.getNotes().isBlank()) {
            Paragraph notePara = new Paragraph("Qeyd: " + doc.getNotes(), smallFont);
            notePara.setSpacingBefore(6);
            pdf.add(notePara);
        }

        pdf.close();
        return baos.toByteArray();
    }

    // ─── Təhvil-Təslim Aktı ───────────────────────────────────────────────────

    private byte[] generateTehvilTeslimAkti(GeneratedDocument doc) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter.getInstance(pdf, baos);
        pdf.open();

        BaseFont bf     = loadBaseFont(false);
        BaseFont bfBold = loadBaseFont(true);

        Font titleFont  = font(bfBold, 13, Font.BOLD, Color.WHITE);
        Font subFont    = font(bf,     9,  Font.NORMAL, Color.WHITE);
        Font sectionFont = font(bfBold, 9, Font.BOLD, HEADER_BG);
        Font bodyFont   = font(bf,     8,  Font.NORMAL, Color.DARK_GRAY);
        Font bodyBold   = font(bfBold, 8,  Font.BOLD, Color.DARK_GRAY);
        Font smallFont  = font(bf,     7,  Font.NORMAL, Color.GRAY);
        Font totalFont  = font(bfBold, 10, Font.BOLD, HEADER_BG);

        // ─── Başlıq ───
        PdfPTable header = new PdfPTable(1);
        header.setWidthPercentage(100);
        header.setSpacingAfter(12);
        PdfPCell hCell = new PdfPCell();
        hCell.setBackgroundColor(HEADER_BG);
        hCell.setBorder(Rectangle.NO_BORDER);
        hCell.setPadding(14);
        Paragraph hp = new Paragraph();
        hp.add(new Chunk("TƏHVİL-TƏSLİM AKTI\n", titleFont));
        hp.add(new Chunk("№ " + doc.getDocumentNumber() + "   " +
                (doc.getDocumentDate() != null ? doc.getDocumentDate().format(DATE_FMT) : ""), subFont));
        hCell.addElement(hp);
        header.addCell(hCell);
        pdf.add(header);

        // ─── Tərəflər ───
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setWidths(new float[]{1f, 1f});
        parties.setSpacingAfter(10);
        parties.addCell(partyCell("TƏHVİL EDƏN (SUPPLIER)", getCompanyInfo(), sectionFont, bodyFont, true));
        parties.addCell(partyCell("TƏSLİM ALAN (BUYER)", getCustomerInfo(doc), sectionFont, bodyFont, false));
        pdf.add(parties);

        // ─── Xidmət cədvəli ───
        pdf.add(buildLineTable(doc.getLines(), bf, bfBold));

        // ─── Cəmi ───
        pdf.add(buildTotalsTable(doc, bf, bfBold, totalFont, bodyFont, smallFont));

        // ─── Qeyd ───
        if (doc.getNotes() != null && !doc.getNotes().isBlank()) {
            pdf.add(new Paragraph("Qeyd: " + doc.getNotes(), smallFont));
        }

        // ─── İmza bloku ───
        pdf.add(buildSignatureBlock(bf, bfBold, bodyFont, bodyBold, smallFont));

        pdf.close();
        return baos.toByteArray();
    }

    // ─── English Invoice ─────────────────────────────────────────────────────

    private byte[] generateEnglishInvoice(GeneratedDocument doc) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter.getInstance(pdf, baos);
        pdf.open();

        BaseFont bf     = loadBaseFont(false);
        BaseFont bfBold = loadBaseFont(true);

        Font titleFont  = font(bfBold, 16, Font.BOLD, Color.WHITE);
        Font subFont    = font(bf,      9,  Font.NORMAL, Color.WHITE);
        Font sectionFont = font(bfBold, 9,  Font.BOLD, HEADER_BG);
        Font bodyFont   = font(bf,      8,  Font.NORMAL, Color.DARK_GRAY);
        Font bodyBold   = font(bfBold,  8,  Font.BOLD, Color.DARK_GRAY);
        Font smallFont  = font(bf,      7,  Font.NORMAL, Color.GRAY);
        Font totalFont  = font(bfBold, 10,  Font.BOLD, HEADER_BG);
        Font accentFont = font(bfBold,  8,  Font.BOLD, ACCENT);

        // ─── Header ───
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{3f, 2f});
        header.setSpacingAfter(12);

        PdfPCell lc = new PdfPCell();
        lc.setBackgroundColor(HEADER_BG);
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setPadding(14);
        Paragraph lp = new Paragraph();
        lp.add(new Chunk("INVOICE\n", titleFont));
        lp.add(new Chunk("No: " + doc.getDocumentNumber() + "   Date: " +
                (doc.getDocumentDate() != null ? doc.getDocumentDate().format(DATE_FMT) : ""), subFont));
        lc.addElement(lp);
        header.addCell(lc);

        PdfPCell rc = new PdfPCell();
        rc.setBackgroundColor(ACCENT);
        rc.setBorder(Rectangle.NO_BORDER);
        rc.setPadding(14);
        rc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph rp = new Paragraph();
        rp.setAlignment(Element.ALIGN_RIGHT);
        String compName = getCompanyConfig("COMPANY_NAME");
        rp.add(new Chunk(compName.isEmpty() ? "CES MMC" : compName,
                font(bfBold, 11, Font.BOLD, Color.WHITE)));
        rc.addElement(rp);
        header.addCell(rc);

        pdf.add(header);

        // ─── From / To ───
        PdfPTable parties = new PdfPTable(2);
        parties.setWidthPercentage(100);
        parties.setWidths(new float[]{1f, 1f});
        parties.setSpacingAfter(10);
        parties.addCell(partyCell("FROM (SELLER)", getCompanyInfo(), sectionFont, bodyFont, true));
        parties.addCell(partyCell("TO (BUYER)", getCustomerInfoEn(doc), sectionFont, bodyFont, false));
        pdf.add(parties);

        // ─── Contract ref ───
        if (doc.getContractNumber() != null || doc.getContractDate() != null) {
            Paragraph contract = new Paragraph();
            contract.setSpacingBefore(2);
            contract.setSpacingAfter(6);
            if (doc.getContractNumber() != null)
                contract.add(new Chunk("Contract No: " + doc.getContractNumber() + "   ", bodyBold));
            if (doc.getContractDate() != null)
                contract.add(new Chunk("Date: " + doc.getContractDate().format(DATE_FMT), bodyFont));
            pdf.add(contract);
        }

        // ─── Line items ───
        pdf.add(buildEnglishLineTable(doc.getLines(), bf, bfBold));

        // ─── Totals ───
        pdf.add(buildEnglishTotalsTable(doc, bf, bfBold, totalFont, bodyFont, smallFont));

        // ─── Bank details ───
        pdf.add(buildBankSection(bf, bfBold, sectionFont, bodyFont, smallFont, accentFont));

        // ─── Notes ───
        if (doc.getNotes() != null && !doc.getNotes().isBlank()) {
            pdf.add(new Paragraph("Note: " + doc.getNotes(), smallFont));
        }

        pdf.close();
        return baos.toByteArray();
    }

    // ─── Köməkçi metodlar ─────────────────────────────────────────────────────

    private String getCompanyInfo() {
        String name    = getCompanyConfig("COMPANY_NAME");
        String voen    = getCompanyConfig("VOEN");
        String address = getCompanyConfig("ADDRESS");
        StringBuilder sb = new StringBuilder();
        if (!name.isEmpty()) sb.append(name).append("\n");
        if (!voen.isEmpty()) sb.append("VÖEN: ").append(voen).append("\n");
        if (!address.isEmpty()) sb.append(address);
        return sb.toString().trim();
    }

    private String getCustomerInfo(GeneratedDocument doc) {
        StringBuilder sb = new StringBuilder();
        if (doc.getCustomerName() != null) sb.append(doc.getCustomerName()).append("\n");
        if (doc.getCustomerVoen() != null && !doc.getCustomerVoen().isBlank())
            sb.append("VÖEN: ").append(doc.getCustomerVoen()).append("\n");
        if (doc.getCustomerAddress() != null && !doc.getCustomerAddress().isBlank())
            sb.append(doc.getCustomerAddress());
        return sb.toString().trim();
    }

    private String getCustomerInfoEn(GeneratedDocument doc) {
        StringBuilder sb = new StringBuilder();
        if (doc.getCustomerName() != null) sb.append(doc.getCustomerName()).append("\n");
        if (doc.getCustomerVoen() != null && !doc.getCustomerVoen().isBlank())
            sb.append("Tax ID: ").append(doc.getCustomerVoen()).append("\n");
        if (doc.getCustomerAddress() != null && !doc.getCustomerAddress().isBlank())
            sb.append(doc.getCustomerAddress());
        return sb.toString().trim();
    }

    private PdfPCell partyCell(String label, String content, Font labelFont, Font bodyFont, boolean leftBorder) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(MID_GRAY);
        cell.setPadding(10);
        cell.setBackgroundColor(LIGHT_GRAY);
        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n", labelFont));
        p.add(new Chunk(content, bodyFont));
        cell.addElement(p);
        return cell;
    }

    private PdfPTable buildLineTable(List<DocumentLine> lines, BaseFont bf, BaseFont bfBold)
            throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.5f, 4f, 1.2f, 1.5f, 1.5f, 1.8f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(6);

        Font thFont   = font(bfBold, 8, Font.BOLD, Color.WHITE);
        Font cellFont = font(bf,     8, Font.NORMAL, Color.DARK_GRAY);
        Font boldCell = font(bfBold, 8, Font.BOLD, Color.DARK_GRAY);

        String[] headers = {"№", "Xidmətin adı", "Vahid", "Miqdar", "Vahid qiymət", "Məbləğ"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(HEADER_BG);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(6);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }

        int i = 1;
        for (DocumentLine line : lines) {
            Color rowBg = (i % 2 == 0) ? LIGHT_GRAY : Color.WHITE;
            addLineCell(table, String.valueOf(i++), cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, line.getDescription(), cellFont, rowBg, Element.ALIGN_LEFT);
            addLineCell(table, line.getUnit(), cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, fmt2(line.getQuantity()), cellFont, rowBg, Element.ALIGN_RIGHT);
            addLineCell(table, fmt2(line.getUnitPrice()), boldCell, rowBg, Element.ALIGN_RIGHT);
            addLineCell(table, fmt2(line.getTotalPrice()), boldCell, rowBg, Element.ALIGN_RIGHT);
        }
        return table;
    }

    private PdfPTable buildEnglishLineTable(List<DocumentLine> lines, BaseFont bf, BaseFont bfBold)
            throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.5f, 4f, 1.2f, 1.5f, 1.5f, 1.8f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(6);

        Font thFont   = font(bfBold, 8, Font.BOLD, Color.WHITE);
        Font cellFont = font(bf,     8, Font.NORMAL, Color.DARK_GRAY);
        Font boldCell = font(bfBold, 8, Font.BOLD, Color.DARK_GRAY);

        String[] headers = {"#", "Description", "Unit", "Qty", "Unit Price", "Amount"};
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(HEADER_BG);
            c.setBorder(Rectangle.NO_BORDER);
            c.setPadding(6);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }

        int i = 1;
        for (DocumentLine line : lines) {
            Color rowBg = (i % 2 == 0) ? LIGHT_GRAY : Color.WHITE;
            addLineCell(table, String.valueOf(i++), cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, line.getDescription(), cellFont, rowBg, Element.ALIGN_LEFT);
            addLineCell(table, line.getUnit(), cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, fmt2(line.getQuantity()), cellFont, rowBg, Element.ALIGN_RIGHT);
            addLineCell(table, fmt2(line.getUnitPrice()), boldCell, rowBg, Element.ALIGN_RIGHT);
            addLineCell(table, fmt2(line.getTotalPrice()), boldCell, rowBg, Element.ALIGN_RIGHT);
        }
        return table;
    }

    private void addLineCell(PdfPTable table, String text, Font f, Color bg, int align) {
        PdfPCell c = new PdfPCell(new Phrase(text, f));
        c.setBackgroundColor(bg);
        c.setBorderColor(MID_GRAY);
        c.setPadding(5);
        c.setHorizontalAlignment(align);
        table.addCell(c);
    }

    private PdfPTable buildTotalsTable(GeneratedDocument doc, BaseFont bf, BaseFont bfBold,
                                       Font totalFont, Font bodyFont, Font smallFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(4);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{2f, 2f});

        Font labelF = font(bf, 8, Font.NORMAL, Color.DARK_GRAY);
        Font amountF = font(bfBold, 8, Font.BOLD, Color.DARK_GRAY);

        addTotalRow(table, "Cəmi:", fmt2(doc.getSubtotal()), labelF, amountF, LIGHT_GRAY);
        addTotalRow(table,
                "ƏDV (" + doc.getVatRate().setScale(0, RoundingMode.HALF_UP) + "%):",
                fmt2(doc.getVatAmount()), labelF, amountF, LIGHT_GRAY);

        // Grand total — styled
        PdfPCell gLabel = new PdfPCell(new Phrase("YEKUNa:", totalFont));
        gLabel.setBackgroundColor(HEADER_BG);
        gLabel.setBorder(Rectangle.NO_BORDER);
        gLabel.setPadding(7);
        gLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(gLabel);

        PdfPCell gAmount = new PdfPCell(new Phrase(fmt2(doc.getGrandTotal()) + " AZN", totalFont));
        gAmount.setBackgroundColor(HEADER_BG);
        gAmount.setBorder(Rectangle.NO_BORDER);
        gAmount.setPadding(7);
        gAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(gAmount);

        return table;
    }

    private PdfPTable buildEnglishTotalsTable(GeneratedDocument doc, BaseFont bf, BaseFont bfBold,
                                              Font totalFont, Font bodyFont, Font smallFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setSpacingBefore(4);
        table.setSpacingAfter(10);
        table.setWidths(new float[]{2f, 2f});

        Font labelF  = font(bf,     8, Font.NORMAL, Color.DARK_GRAY);
        Font amountF = font(bfBold, 8, Font.BOLD, Color.DARK_GRAY);

        addTotalRow(table, "Subtotal:", fmt2(doc.getSubtotal()), labelF, amountF, LIGHT_GRAY);
        addTotalRow(table,
                "VAT (" + doc.getVatRate().setScale(0, RoundingMode.HALF_UP) + "%):",
                fmt2(doc.getVatAmount()), labelF, amountF, LIGHT_GRAY);

        PdfPCell gLabel = new PdfPCell(new Phrase("TOTAL:", totalFont));
        gLabel.setBackgroundColor(HEADER_BG);
        gLabel.setBorder(Rectangle.NO_BORDER);
        gLabel.setPadding(7);
        gLabel.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(gLabel);

        PdfPCell gAmount = new PdfPCell(new Phrase(fmt2(doc.getGrandTotal()) + " AZN", totalFont));
        gAmount.setBackgroundColor(HEADER_BG);
        gAmount.setBorder(Rectangle.NO_BORDER);
        gAmount.setPadding(7);
        gAmount.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(gAmount);

        return table;
    }

    private void addTotalRow(PdfPTable table, String label, String value,
                             Font labelF, Font amountF, Color bg) {
        PdfPCell lc = new PdfPCell(new Phrase(label, labelF));
        lc.setBackgroundColor(bg);
        lc.setBorderColor(MID_GRAY);
        lc.setPadding(5);
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, amountF));
        vc.setBackgroundColor(bg);
        vc.setBorderColor(MID_GRAY);
        vc.setPadding(5);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vc);
    }

    private Element buildBankSection(BaseFont bf, BaseFont bfBold, Font sectionFont,
                                     Font bodyFont, Font smallFont, Font accentFont)
            throws DocumentException {
        String bankName = getBankConfig("BANK_NAME");
        String iban     = getBankConfig("IBAN");
        String swift    = getBankConfig("SWIFT");
        String corrAcc  = getBankConfig("CORRESPONDENT_ACCOUNT");

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(ACCENT);
        cell.setBackgroundColor(LIGHT_GRAY);
        cell.setPadding(10);

        Paragraph p = new Paragraph();
        p.add(new Chunk("BANK MƏLUMATlARI\n", font(bfBold, 9, Font.BOLD, HEADER_BG)));
        if (!bankName.isEmpty()) p.add(new Chunk("Bank: " + bankName + "\n", bodyFont));
        if (!iban.isEmpty())    p.add(new Chunk("IBAN: " + iban + "\n", bodyFont));
        if (!swift.isEmpty())   p.add(new Chunk("SWIFT: " + swift + "\n", bodyFont));
        if (!corrAcc.isEmpty()) p.add(new Chunk("Müxbir hesab: " + corrAcc + "\n", bodyFont));
        if (bankName.isEmpty() && iban.isEmpty() && swift.isEmpty())
            p.add(new Chunk("Bank məlumatları hələ konfiqurasiya edilməyib.", smallFont));

        cell.addElement(p);
        table.addCell(cell);
        return table;
    }

    private Element buildSignatureBlock(BaseFont bf, BaseFont bfBold,
                                        Font bodyFont, Font bodyBold, Font smallFont)
            throws DocumentException {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(20);
        table.setWidths(new float[]{1f, 1f});

        String directorName = getCompanyConfig("DIRECTOR_NAME");
        String compName     = getCompanyConfig("COMPANY_NAME");

        table.addCell(signatureCell("TƏHVİL EDƏN\n" + (compName.isEmpty() ? "CES MMC" : compName),
                directorName.isEmpty() ? "Direktor" : directorName, bf, bfBold));
        table.addCell(signatureCell("TƏSLİM ALAN", "Səlahiyyətli şəxs", bf, bfBold));

        return table;
    }

    private PdfPCell signatureCell(String label, String role, BaseFont bf, BaseFont bfBold) {
        Font labelF  = font(bfBold, 8, Font.BOLD, Color.DARK_GRAY);
        Font roleF   = font(bf,     7, Font.NORMAL, Color.GRAY);
        Font lineF   = font(bf,     8, Font.NORMAL, Color.DARK_GRAY);

        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(MID_GRAY);
        cell.setPadding(12);
        cell.setBackgroundColor(LIGHT_GRAY);

        Paragraph p = new Paragraph();
        p.add(new Chunk(label + "\n\n", labelF));
        p.add(new Chunk(role + ": ________________________\n\n", roleF));
        p.add(new Chunk("İmza: ________________________\n", lineF));
        p.add(new Chunk("M.Y.                          Tarix: ____________\n", lineF));
        cell.addElement(p);
        return cell;
    }

    // ─── Format helpers ───────────────────────────────────────────────────────

    private String fmt2(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
