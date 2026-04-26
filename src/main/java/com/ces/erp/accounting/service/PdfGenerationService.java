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

    private static final String[] AZ_MONTHS = {
        "yanvar", "fevral", "mart", "aprel", "may", "iyun",
        "iyul", "avqust", "sentyabr", "oktyabr", "noyabr", "dekabr"
    };
    private static final String[] ONES = {
        "", "bir", "iki", "üç", "dörd", "beş", "altı", "yeddi", "səkkiz", "doqquz"
    };
    private static final String[] TENS = {
        "", "on", "iyirmi", "otuz", "qırx", "əlli", "altmış", "yetmiş", "səksən", "doxsan"
    };

    // ─── Ana metod ────────────────────────────────────────────────────────────

    public String generateAndStore(GeneratedDocument doc) throws IOException, DocumentException {
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
        String resource = bold ? "/fonts/DejaVuSans-Bold.ttf" : "/fonts/DejaVuSans.ttf";
        // 1. Classpath-dən yüklə (src/main/resources/fonts/)
        try (var is = PdfGenerationService.class.getResourceAsStream(resource)) {
            if (is != null) {
                byte[] bytes = is.readAllBytes();
                String fakeName = bold ? "DejaVuSans-Bold.ttf" : "DejaVuSans.ttf";
                return BaseFont.createFont(fakeName, BaseFont.IDENTITY_H, BaseFont.EMBEDDED, false, bytes, null);
            }
        } catch (Exception e) {
            log.warn("Classpath font yüklənə bilmədi: {}", e.getMessage());
        }
        // 2. Windows sistem şriftləri (ə, ş, ğ, ı dəstəkləyir)
        String[] winFonts = bold
            ? new String[]{"C:/Windows/Fonts/arialbd.ttf",  "C:/Windows/Fonts/calibrib.ttf"}
            : new String[]{"C:/Windows/Fonts/arial.ttf",    "C:/Windows/Fonts/calibri.ttf"};
        for (String path : winFonts) {
            try {
                return BaseFont.createFont(path, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } catch (Exception ignored) {}
        }
        // 3. Son çıxış — Helvetica (AZ xüsusi hərfləri göstərmir)
        log.warn("DejaVu və sistem şriftləri tapılmadı — Helvetica istifadə edilir (AZ hərfləri çatışmaya bilər)");
        return BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
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

    // ─── Azərbaycan dili köməkçiləri ──────────────────────────────────────────

    private String numberToWords(long n) {
        if (n == 0) return "sıfır";
        StringBuilder sb = new StringBuilder();
        if (n >= 1_000_000_000L) { sb.append(numberToWords(n / 1_000_000_000L)).append(" milyard "); n %= 1_000_000_000L; }
        if (n >= 1_000_000L)     { sb.append(numberToWords(n / 1_000_000L)).append(" milyon "); n %= 1_000_000L; }
        if (n >= 1000L) {
            long th = n / 1000;
            sb.append(th == 1 ? "min " : numberToWords(th) + " min ");
            n %= 1000;
        }
        if (n >= 100) { long h = n / 100; sb.append(h == 1 ? "yüz " : ONES[(int)h] + " yüz "); n %= 100; }
        if (n >= 10)  { sb.append(TENS[(int)(n / 10)]).append(" "); n %= 10; }
        if (n > 0)    { sb.append(ONES[(int)n]).append(" "); }
        return sb.toString().trim();
    }

    private String amountInWords(BigDecimal amount) {
        if (amount == null) return "";
        long total   = amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
        long manats  = total / 100;
        long qepiks  = total % 100;
        String s = numberToWords(manats);
        if (!s.isEmpty()) s = Character.toUpperCase(s.charAt(0)) + s.substring(1);
        s += " manat";
        if (qepiks > 0) s += " " + qepiks + " qəpik";
        return s;
    }

    private String azOrdinalSuffix(int year) {
        int last2 = year % 100;
        String word = numberToWords(last2 == 0 ? year : last2);
        char lastVowel = 0;
        for (int i = word.length() - 1; i >= 0; i--) {
            char c = word.charAt(i);
            if ("aıeəiouöü".indexOf(c) >= 0) { lastVowel = c; break; }
        }
        if (lastVowel == 'a' || lastVowel == 'ı') return "cı";
        if (lastVowel == 'o' || lastVowel == 'u') return "cu";
        if (lastVowel == 'ö' || lastVowel == 'ü') return "cü";
        return "ci";
    }

    private String formatAzDateFormal(java.time.LocalDate date) {
        if (date == null) return "";
        String day   = String.format("%02d", date.getDayOfMonth());
        String month = AZ_MONTHS[date.getMonthValue() - 1];
        int    year  = date.getYear();
        return " \"" + day + "\" " + month + " " + year + "-" + azOrdinalSuffix(year) + " il";
    }

    private String formatAzDateInline(java.time.LocalDate date) {
        if (date == null) return "";
        String day   = String.format("%02d", date.getDayOfMonth());
        String month = AZ_MONTHS[date.getMonthValue() - 1];
        int    year  = date.getYear();
        return day + " " + month + " " + year + "-" + azOrdinalSuffix(year) + " il";
    }

    // ─── Hesab-Faktura ────────────────────────────────────────────────────────

    private byte[] generateHesabFaktura(GeneratedDocument doc) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(pdf, baos);
        pdf.open();

        BaseFont bf     = loadBaseFont(false);
        BaseFont bfBold = loadBaseFont(true);

        Font titleFont  = font(bfBold, 15, Font.BOLD,   HEADER_BG);
        Font labelFont  = font(bfBold,  9, Font.BOLD,   Color.DARK_GRAY);
        Font bodyFont   = font(bf,       9, Font.NORMAL, Color.DARK_GRAY);
        Font thFont     = font(bfBold,   8, Font.BOLD,   Color.WHITE);
        Font cellFont   = font(bf,       8, Font.NORMAL, Color.DARK_GRAY);
        Font boldCell   = font(bfBold,   8, Font.BOLD,   Color.DARK_GRAY);
        Font smallItal  = font(bf,       8, Font.ITALIC, new Color(80, 80, 80));
        Font bankLblF   = font(bfBold,   8, Font.BOLD,   Color.DARK_GRAY);
        Font bankValF   = font(bf,       8, Font.NORMAL, HEADER_BG);

        String compName    = getCompanyConfig("COMPANY_NAME");
        String compVoen    = getCompanyConfig("VOEN");
        String compAddress = getCompanyConfig("ADDRESS");
        if (compName.isEmpty()) compName = "Construction Equipment Services MMC";

        // ─── 1. Şirkət info (sol) + Logo (sağ) ───
        PdfPTable topRow = new PdfPTable(2);
        topRow.setWidthPercentage(100);
        topRow.setWidths(new float[]{2f, 1f});
        topRow.setSpacingAfter(14);

        PdfPCell compCell = new PdfPCell();
        compCell.setBorder(Rectangle.NO_BORDER);
        compCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph compPara = new Paragraph();
        compPara.add(new Chunk(compName + "\n", labelFont));
        if (!compAddress.isEmpty()) compPara.add(new Chunk(compAddress, bodyFont));
        compCell.addElement(compPara);
        topRow.addCell(compCell);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try (var is = PdfGenerationService.class.getResourceAsStream("/images/filelogo.png")) {
            if (is != null) {
                Image logo = Image.getInstance(is.readAllBytes());
                logo.scaleToFit(120, 60);
                logo.setAlignment(Image.RIGHT);
                logoCell.addElement(logo);
            }
        } catch (Exception ignored) {}
        topRow.addCell(logoCell);
        pdf.add(topRow);

        // ─── 2. Başlıq ───
        Paragraph titlePara = new Paragraph("HESAB-FAKTURA № " + doc.getDocumentNumber(), titleFont);
        titlePara.setAlignment(Element.ALIGN_CENTER);
        titlePara.setSpacingAfter(4);
        pdf.add(titlePara);

        // ─── 3. Tarix ───
        Paragraph datePara = new Paragraph(formatAzDateInline(doc.getDocumentDate()), font(bf, 10, Font.NORMAL, Color.DARK_GRAY));
        datePara.setAlignment(Element.ALIGN_CENTER);
        datePara.setSpacingAfter(14);
        pdf.add(datePara);

        // ─── 4. Məlumat bloku ───
        PdfPTable infoTbl = new PdfPTable(new float[]{1.8f, 3f});
        infoTbl.setWidthPercentage(100);
        infoTbl.setSpacingAfter(10);
        addInfoRow(infoTbl, "Hesabı təqdim edən:", "\"" + compName + "\"", labelFont, bodyFont);
        if (!compVoen.isEmpty())
            addInfoRow(infoTbl, "VÖEN :", compVoen, bodyFont, bodyFont);
        addInfoRow(infoTbl, "Hesabı alan:", "\"" + doc.getCustomerName() + "\"", labelFont, bodyFont);
        if (doc.getCustomerVoen() != null && !doc.getCustomerVoen().isBlank())
            addInfoRow(infoTbl, "VÖEN :", doc.getCustomerVoen(), bodyFont, bodyFont);
        if (doc.getContractDate() != null)
            addInfoRow(infoTbl, "Müqavilənin tarixi:", doc.getContractDate().format(DATE_FMT), labelFont, bodyFont);
        if (doc.getContractNumber() != null && !doc.getContractNumber().isBlank())
            addInfoRow(infoTbl, "Müqavilə №:", doc.getContractNumber(), labelFont, bodyFont);
        addInfoRow(infoTbl, "Valyuta:", "AZN", labelFont, bodyFont);
        pdf.add(infoTbl);

        // ─── 5. Xidmət cədvəli ───
        pdf.add(buildHfLineTable(doc.getLines(), thFont, cellFont, boldCell));

        // ─── 6. Cəmi ───
        pdf.add(buildHfTotals(doc, bf, bfBold, boldCell));
        Paragraph wordsLine = new Paragraph(
                "*Ödəniləcək məbləğ, ƏDV ilə: " + amountInWords(doc.getGrandTotal()), smallItal);
        wordsLine.setSpacingAfter(12);
        pdf.add(wordsLine);

        // ─── 7. Bank məlumatları ───
        pdf.add(buildHfBankSection(bankLblF, bankValF, doc));

        // ─── 8. Qeyd ───
        if (doc.getNotes() != null && !doc.getNotes().isBlank()) {
            Paragraph notePara = new Paragraph("Qeyd: " + doc.getNotes(),
                    font(bf, 8, Font.NORMAL, Color.GRAY));
            notePara.setSpacingBefore(4);
            pdf.add(notePara);
        }

        // ─── 9. Direktor ───
        pdf.add(buildHfDirectorLine(bf, bfBold));

        pdf.close();
        return baos.toByteArray();
    }

    // ─── Təhvil-Təslim Aktı ───────────────────────────────────────────────────

    private byte[] generateTehvilTeslimAkti(GeneratedDocument doc) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Document pdf = new Document(PageSize.A4, 50, 50, 50, 50);
        PdfWriter.getInstance(pdf, baos);
        pdf.open();

        BaseFont bf     = loadBaseFont(false);
        BaseFont bfBold = loadBaseFont(true);

        Font titleFont  = font(bfBold, 14, Font.BOLD,   HEADER_BG);
        Font bodyFont   = font(bf,      9, Font.NORMAL, Color.DARK_GRAY);
        Font bodyBold   = font(bfBold,  9, Font.BOLD,   Color.DARK_GRAY);
        Font smallFont  = font(bf,      8, Font.NORMAL, Color.GRAY);

        // ─── 1. Logo + Başlıq ───
        PdfPTable logoRow = new PdfPTable(2);
        logoRow.setWidthPercentage(100);
        logoRow.setWidths(new float[]{1.2f, 2f});
        logoRow.setSpacingAfter(4);

        PdfPCell logoCell = new PdfPCell();
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        try (var is = PdfGenerationService.class.getResourceAsStream("/images/filelogo.png")) {
            if (is != null) {
                Image logo = Image.getInstance(is.readAllBytes());
                logo.scaleToFit(130, 55);
                logo.setAlignment(Image.LEFT);
                logoCell.addElement(logo);
            } else {
                logoCell.addElement(new Paragraph(getCompanyConfig("COMPANY_NAME"), bodyBold));
            }
        } catch (Exception e) {
            logoCell.addElement(new Paragraph(getCompanyConfig("COMPANY_NAME"), bodyBold));
        }
        logoRow.addCell(logoCell);

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        Paragraph titleP = new Paragraph("TƏHVİL – TƏSLİM AKTI", titleFont);
        titleP.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(titleP);
        Paragraph docNumP = new Paragraph("№ " + doc.getDocumentNumber(), font(bf, 9, Font.NORMAL, HEADER_BG));
        docNumP.setAlignment(Element.ALIGN_RIGHT);
        titleCell.addElement(docNumP);
        logoRow.addCell(titleCell);
        pdf.add(logoRow);

        // Horizontal divider
        PdfPTable hr = new PdfPTable(1);
        hr.setWidthPercentage(100);
        hr.setSpacingAfter(10);
        PdfPCell hrC = new PdfPCell(new Phrase(""));
        hrC.setBackgroundColor(HEADER_BG);
        hrC.setFixedHeight(2);
        hrC.setBorder(Rectangle.NO_BORDER);
        hr.addCell(hrC);
        pdf.add(hr);

        // ─── 2. Bakı şəhəri + Tarix ───
        PdfPTable cityDate = new PdfPTable(2);
        cityDate.setWidthPercentage(100);
        cityDate.setSpacingAfter(12);
        PdfPCell cityC = new PdfPCell(new Phrase("Bakı şəhəri", bodyBold));
        cityC.setBorder(Rectangle.NO_BORDER); cityC.setPadding(2);
        cityDate.addCell(cityC);
        PdfPCell dateC = new PdfPCell(new Phrase(formatAzDateFormal(doc.getDocumentDate()), bodyBold));
        dateC.setBorder(Rectangle.NO_BORDER); dateC.setPadding(2);
        dateC.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cityDate.addCell(dateC);
        pdf.add(cityDate);

        // ─── 3. Giriş mətni ───
        String compName = getCompanyConfig("COMPANY_NAME");
        if (compName.isEmpty()) compName = "Construction Equipment Services";

        String addendumRef = buildAddendumRef(doc.getAddendumNumbers());
        boolean hasAddendum = !addendumRef.isEmpty();

        boolean hasDate   = doc.getContractDate()   != null;
        boolean hasNumber = doc.getContractNumber() != null && !doc.getContractNumber().isBlank();

        // addendum varsa "Müqavilə" (yalın hal), yoxdursa "Müqaviləyə" (yönlük hal)
        String muqBase;
        if (hasDate && hasNumber) {
            muqBase = formatAzDateInline(doc.getContractDate()) + " tarixli "
                    + doc.getContractNumber() + " nömrəli Müqavilə";
        } else if (hasDate) {
            muqBase = formatAzDateInline(doc.getContractDate()) + " tarixli Müqavilə";
        } else if (hasNumber) {
            muqBase = doc.getContractNumber() + " nömrəli Müqavilə";
        } else {
            muqBase = "Müqavilə";
        }

        String contractRef = hasAddendum ? muqBase : muqBase + "yə";

        String intro = "Biz, aşağıda imza edənlər, bu aktı tərtib etdik ki, həqiqətən \""
                + compName + "\" MMC və \"" + doc.getCustomerName()
                + "\" MMC arasında imzalanmış " + contractRef
                + (hasAddendum ? ". " + addendumRef : " əsasən")
                + " \"İCRAÇI\" \"SİFARİŞÇİ\"nin sifarişi əsasında aşağıda "
                + "göstərilən xidməti tam və düzgün şəkildə yerinə yetirmişdir.";
        Paragraph introPara = new Paragraph(intro, bodyFont);
        introPara.setAlignment(Element.ALIGN_JUSTIFIED);
        introPara.setSpacingAfter(12);
        pdf.add(introPara);

        // ─── 4. Xidmət cədvəli ───
        pdf.add(buildAktLineTable(doc.getLines(), bf, bfBold));

        // ─── 5. Yekun cədvəli ───
        pdf.add(buildAktTotals(doc, bf, bfBold));

        // ─── 6. Qeyd ───
        if (doc.getNotes() != null && !doc.getNotes().isBlank()) {
            Paragraph notePara = new Paragraph("Qeyd: " + doc.getNotes(), smallFont);
            notePara.setSpacingBefore(4);
            notePara.setSpacingAfter(8);
            pdf.add(notePara);
        }

        // ─── 7. Təsdiq mətni ───
        Paragraph confirmPara = new Paragraph(
                "Yuxarıda göstərilənləri öz imzalarımızla təsdiq edirik.", bodyFont);
        confirmPara.setSpacingBefore(6);
        confirmPara.setSpacingAfter(16);
        pdf.add(confirmPara);

        // ─── 8. İmza bloku ───
        pdf.add(buildAktSignatureBlock(doc, bf, bfBold));

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
        pdf.add(buildBankSection(bf, bfBold, sectionFont, bodyFont, smallFont, accentFont, doc));

        // ─── Notes ───
        if (doc.getNotes() != null && !doc.getNotes().isBlank()) {
            pdf.add(new Paragraph("Note: " + doc.getNotes(), smallFont));
        }

        pdf.close();
        return baos.toByteArray();
    }

    // ─── Akt üçün xüsusi metodlar ─────────────────────────────────────────────

    private PdfPTable buildAktLineTable(List<DocumentLine> lines, BaseFont bf, BaseFont bfBold)
            throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.5f, 4f, 1.2f, 1f, 1.5f, 1.8f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(6);
        table.setSpacingAfter(6);

        Font thFont   = font(bfBold, 8, Font.BOLD,   Color.WHITE);
        Font cellFont = font(bf,     8, Font.NORMAL, Color.DARK_GRAY);
        Font boldCell = font(bfBold, 8, Font.BOLD,   Color.DARK_GRAY);

        for (String h : new String[]{"№", "Göstərilən xidmətlərin məzmunu", "Ölçü vahidi", "Sayı", "Qiyməti", "Məbləğ"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(HEADER_BG);
            c.setBorder(Rectangle.BOX);
            c.setBorderColor(new Color(20, 50, 100));
            c.setPadding(5);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(c);
        }

        int i = 1;
        for (DocumentLine line : lines) {
            Color rowBg = (i % 2 == 0) ? LIGHT_GRAY : Color.WHITE;
            addLineCell(table, String.valueOf(i++), cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, line.getDescription(),          cellFont, rowBg, Element.ALIGN_LEFT);
            addLineCell(table, "xidmət",                       cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, "1",                            cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, fmtAz(line.getUnitPrice()),     cellFont, rowBg, Element.ALIGN_RIGHT);
            addLineCell(table, fmtAz(line.getTotalPrice()),    boldCell, rowBg, Element.ALIGN_RIGHT);
        }
        return table;
    }

    private Element buildAktTotals(GeneratedDocument doc, BaseFont bf, BaseFont bfBold)
            throws DocumentException {
        Font labelF  = font(bf,      9,  Font.NORMAL, Color.DARK_GRAY);
        Font boldF   = font(bfBold,  9,  Font.BOLD,   Color.DARK_GRAY);
        Font totalF  = font(bfBold, 11,  Font.BOLD,   HEADER_BG);
        Font wordsF  = font(bf,      8,  Font.ITALIC, new Color(80, 80, 80));

        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100);
        outer.setSpacingBefore(4);
        outer.setSpacingAfter(10);
        outer.setWidths(new float[]{1f, 1f});

        PdfPCell emptyCell = new PdfPCell(new Phrase(""));
        emptyCell.setBorder(Rectangle.NO_BORDER);
        outer.addCell(emptyCell);

        PdfPTable inner = new PdfPTable(2);
        inner.setWidthPercentage(100);
        inner.setWidths(new float[]{1.5f, 1.5f});

        addTotalRow(inner, "CƏMİ:", fmtAz(doc.getSubtotal()), labelF, boldF, Color.WHITE);
        addTotalRow(inner,
                "ƏDV – " + doc.getVatRate().setScale(0, RoundingMode.HALF_UP) + "%:",
                fmtAz(doc.getVatAmount()), labelF, boldF, Color.WHITE);

        PdfPCell gLbl = new PdfPCell(new Phrase("Yekunu:", totalF));
        gLbl.setBorderColor(HEADER_BG); gLbl.setBorder(Rectangle.BOX);
        gLbl.setPadding(6); gLbl.setHorizontalAlignment(Element.ALIGN_RIGHT);
        inner.addCell(gLbl);

        PdfPCell gAmt = new PdfPCell(new Phrase(fmtAz(doc.getGrandTotal()) + " AZN", totalF));
        gAmt.setBorderColor(HEADER_BG); gAmt.setBorder(Rectangle.BOX);
        gAmt.setPadding(6); gAmt.setHorizontalAlignment(Element.ALIGN_RIGHT);
        inner.addCell(gAmt);

        PdfPCell wordsC = new PdfPCell(new Phrase(amountInWords(doc.getGrandTotal()), wordsF));
        wordsC.setColspan(2);
        wordsC.setBorder(Rectangle.BOX); wordsC.setBorderColor(MID_GRAY);
        wordsC.setPadding(6); wordsC.setBackgroundColor(LIGHT_GRAY);
        inner.addCell(wordsC);

        PdfPCell rightWrapper = new PdfPCell(inner);
        rightWrapper.setBorder(Rectangle.NO_BORDER);
        rightWrapper.setPadding(0);
        outer.addCell(rightWrapper);
        return outer;
    }

    private Element buildAktSignatureBlock(GeneratedDocument doc, BaseFont bf, BaseFont bfBold)
            throws DocumentException {
        Font roleFont = font(bfBold, 9, Font.BOLD,   HEADER_BG);
        Font nameFont = font(bfBold, 9, Font.BOLD,   Color.DARK_GRAY);
        Font infoFont = font(bf,     8, Font.NORMAL, Color.DARK_GRAY);
        Font lineFont = font(bf,     8, Font.NORMAL, new Color(100, 100, 100));

        String compName = getCompanyConfig("COMPANY_NAME");
        String compVoen = getCompanyConfig("VOEN");
        String dirName  = getCompanyConfig("DIRECTOR_NAME");
        if (compName.isEmpty()) compName = "Construction Equipment Services";

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(16);
        table.setWidths(new float[]{1f, 1f});

        // İCRAÇI
        PdfPCell lc = new PdfPCell();
        lc.setBorder(Rectangle.BOX); lc.setBorderColor(MID_GRAY);
        lc.setPadding(12); lc.setBackgroundColor(LIGHT_GRAY);
        Paragraph lp = new Paragraph();
        lp.add(new Chunk("\"İCRAÇI\"\n", roleFont));
        lp.add(new Chunk("\"" + compName + "\"\n", nameFont));
        if (!compVoen.isEmpty()) lp.add(new Chunk("VÖEN: " + compVoen + "\n\n", infoFont));
        else lp.add(new Chunk("\n", infoFont));
        lp.add(new Chunk("Direktor: " + (dirName.isEmpty() ? "_______________________" : dirName) + "\n\n", infoFont));
        lp.add(new Chunk("İmza: _______________________\n", lineFont));
        lp.add(new Chunk("M.Y.", lineFont));
        lc.addElement(lp);
        table.addCell(lc);

        // SİFARİŞÇİ
        PdfPCell rc = new PdfPCell();
        rc.setBorder(Rectangle.BOX); rc.setBorderColor(MID_GRAY);
        rc.setPadding(12); rc.setBackgroundColor(LIGHT_GRAY);
        Paragraph rp = new Paragraph();
        rp.add(new Chunk("\"SİFARİŞÇİ\"\n", roleFont));
        rp.add(new Chunk("\"" + doc.getCustomerName() + "\"\n", nameFont));
        if (doc.getCustomerVoen() != null && !doc.getCustomerVoen().isBlank())
            rp.add(new Chunk("VÖEN: " + doc.getCustomerVoen() + "\n\n", infoFont));
        else
            rp.add(new Chunk("\n", infoFont));
        String custDir = doc.getCustomerDirectorName();
        rp.add(new Chunk("Direktor: " + (custDir != null && !custDir.isBlank() ? custDir : "_______________________") + "\n\n", infoFont));
        rp.add(new Chunk("İmza: _______________________\n", lineFont));
        rp.add(new Chunk("M.Y.", lineFont));
        rc.addElement(rp);
        table.addCell(rc);

        return table;
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
        PdfPCell gLabel = new PdfPCell(new Phrase("YEKUN:", totalFont));
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
                                     Font bodyFont, Font smallFont, Font accentFont,
                                     GeneratedDocument doc)
            throws DocumentException {
        String bankName = notEmpty(doc.getBankName(),  () -> getBankConfig("BANK_NAME"));
        String iban     = notEmpty(doc.getBankIban(),  () -> getBankConfig("IBAN"));
        String swift    = notEmpty(doc.getBankSwift(), () -> getBankConfig("SWIFT"));
        String corrAcc  = notEmpty(doc.getBankMh(),    () -> getBankConfig("CORRESPONDENT_ACCOUNT"));

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

    // ─── Hesab-Faktura üçün xüsusi metodlar ───────────────────────────────────

    private void addInfoRow(PdfPTable tbl, String label, String value, Font lFont, Font vFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, lFont));
        lc.setBorder(Rectangle.NO_BORDER); lc.setPadding(2);
        tbl.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, vFont));
        vc.setBorder(Rectangle.NO_BORDER); vc.setPadding(2);
        tbl.addCell(vc);
    }

    private PdfPTable buildHfLineTable(List<DocumentLine> lines, Font thFont, Font cellFont, Font boldCell)
            throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{0.5f, 4f, 1.2f, 1f, 1.5f, 1.8f});
        table.setWidthPercentage(100);
        table.setSpacingBefore(4);
        table.setSpacingAfter(4);

        for (String h : new String[]{"№", "MALLAR (İŞLƏR, XİDMƏTLƏR)", "ÖLÇÜ VAHİDİ", "MİQDARI", "QİYMƏTİ", "CƏMİ"}) {
            PdfPCell c = new PdfPCell(new Phrase(h, thFont));
            c.setBackgroundColor(HEADER_BG);
            c.setBorder(Rectangle.BOX);
            c.setBorderColor(new Color(20, 50, 100));
            c.setPaddingTop(6); c.setPaddingBottom(6);
            c.setPaddingLeft(4); c.setPaddingRight(4);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);
            c.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(c);
        }

        int i = 1;
        for (DocumentLine line : lines) {
            Color rowBg = (i % 2 == 0) ? LIGHT_GRAY : Color.WHITE;
            addLineCell(table, String.valueOf(i++),        cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, line.getDescription(),      cellFont, rowBg, Element.ALIGN_LEFT);
            addLineCell(table, "xidmət",                   cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, "1",                        cellFont, rowBg, Element.ALIGN_CENTER);
            addLineCell(table, fmtAz(line.getUnitPrice()), cellFont, rowBg, Element.ALIGN_RIGHT);
            addLineCell(table, fmtAz(line.getTotalPrice()), boldCell, rowBg, Element.ALIGN_RIGHT);
        }
        return table;
    }

    private Element buildHfTotals(GeneratedDocument doc, BaseFont bf, BaseFont bfBold, Font boldCell)
            throws DocumentException {
        Font lblF = font(bfBold, 9, Font.BOLD, Color.DARK_GRAY);
        Font amtF = font(bfBold, 9, Font.BOLD, Color.DARK_GRAY);

        PdfPTable outer = new PdfPTable(2);
        outer.setWidthPercentage(100);
        outer.setSpacingBefore(4);
        outer.setSpacingAfter(4);
        outer.setWidths(new float[]{1f, 1f});

        PdfPCell blank = new PdfPCell(new Phrase(""));
        blank.setBorder(Rectangle.NO_BORDER);
        outer.addCell(blank);

        PdfPTable inner = new PdfPTable(new float[]{1.8f, 1.2f});
        inner.setWidthPercentage(100);
        addTotalRow(inner, "CƏMİ (ƏDV daxil):", fmtAz(doc.getGrandTotal()), lblF, amtF, LIGHT_GRAY);
        addTotalRow(inner, "O cümlədən ƏDV:", fmtAz(doc.getVatAmount()), lblF, amtF, LIGHT_GRAY);

        PdfPCell innerWrapper = new PdfPCell(inner);
        innerWrapper.setBorder(Rectangle.NO_BORDER);
        innerWrapper.setPadding(0);
        outer.addCell(innerWrapper);
        return outer;
    }

    private Element buildHfBankSection(Font bankLblF, Font bankValF, GeneratedDocument doc) throws DocumentException {
        String bankName = notEmpty(doc.getBankName(),  () -> getBankConfig("BANK_NAME"));
        String bankCode = notEmpty(doc.getBankCode(),  () -> getBankConfig("BANK_CODE"));
        String swift    = notEmpty(doc.getBankSwift(), () -> getBankConfig("SWIFT"));
        String iban     = notEmpty(doc.getBankIban(),  () -> getBankConfig("IBAN"));
        String corrAcc  = notEmpty(doc.getBankMh(),    () -> getBankConfig("CORRESPONDENT_ACCOUNT"));
        String settlAcc = notEmpty(doc.getBankHh(),    () -> "");

        PdfPTable table = new PdfPTable(new float[]{1f, 3f});
        table.setWidthPercentage(90);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.setSpacingBefore(10);
        table.setSpacingAfter(8);

        if (!bankName.isEmpty()) addBankRow(table, "Bank :",  bankName, bankLblF, bankValF);
        if (!bankCode.isEmpty()) addBankRow(table, "Kodu:",   bankCode, bankLblF, bankValF);
        if (!swift.isEmpty())    addBankRow(table, "SWIFT:",  swift,    bankLblF, bankValF);
        if (!iban.isEmpty())     addBankRow(table, "H./h :",  iban,     bankLblF, bankValF);
        if (!corrAcc.isEmpty())  addBankRow(table, "M./h :",  corrAcc,  bankLblF, bankValF);
        if (!settlAcc.isEmpty()) addBankRow(table, "H./h :",  settlAcc, bankLblF, bankValF);

        return table;
    }

    private String notEmpty(String val, java.util.function.Supplier<String> fallback) {
        return (val != null && !val.isBlank()) ? val : fallback.get();
    }

    private void addBankRow(PdfPTable tbl, String label, String value, Font lFont, Font vFont) {
        PdfPCell lc = new PdfPCell(new Phrase(label, lFont));
        lc.setBorder(Rectangle.NO_BORDER); lc.setPadding(3);
        tbl.addCell(lc);
        PdfPCell vc = new PdfPCell(new Phrase(value, vFont));
        vc.setBorder(Rectangle.NO_BORDER); vc.setPadding(3);
        tbl.addCell(vc);
    }

    private Element buildHfDirectorLine(BaseFont bf, BaseFont bfBold) throws DocumentException {
        Font dirFont  = font(bfBold, 9, Font.BOLD,   Color.DARK_GRAY);
        Font nameFont = font(bf,     9, Font.NORMAL, Color.DARK_GRAY);
        String dirName = getCompanyConfig("DIRECTOR_NAME");

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(24);

        PdfPCell lc = new PdfPCell(new Phrase("Direktor :", dirFont));
        lc.setBorder(Rectangle.NO_BORDER); lc.setPadding(4);
        table.addCell(lc);

        PdfPCell rc = new PdfPCell(new Phrase(dirName.isEmpty() ? "_______________________" : dirName, nameFont));
        rc.setBorder(Rectangle.NO_BORDER); rc.setPadding(4);
        rc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(rc);
        return table;
    }

    // ─── Əlavə referans mətni ─────────────────────────────────────────────────

    private String buildAddendumRef(String addendumNumbersJson) {
        if (addendumNumbersJson == null || addendumNumbersJson.isBlank()) return "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            List<Integer> nums = mapper.readValue(addendumNumbersJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Integer>>() {});
            if (nums == null || nums.isEmpty()) return "";
            if (nums.size() == 1) {
                return nums.get(0) + " saylı Əlavəyə əsasən";
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < nums.size(); i++) {
                    if (i > 0) sb.append(i == nums.size() - 1 ? " və " : ", ");
                    sb.append(nums.get(i)).append(" saylı");
                }
                sb.append(" Əlavəyə əsasən");
                return sb.toString();
            }
        } catch (Exception e) {
            return "";
        }
    }

    // ─── Format helpers ───────────────────────────────────────────────────────

    private String fmtAz(BigDecimal v) {
        if (v == null) return "0,00";
        return String.format(java.util.Locale.GERMANY, "%,.2f", v.doubleValue());
    }

    private String fmtQty(BigDecimal v) {
        if (v == null) return "0";
        if (v.remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0)
            return v.toBigInteger().toString();
        return String.format(java.util.Locale.GERMANY, "%.2f", v.doubleValue());
    }

    private String fmt2(BigDecimal v) {
        if (v == null) return "0.00";
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
