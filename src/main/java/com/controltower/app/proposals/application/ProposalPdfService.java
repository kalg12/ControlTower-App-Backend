package com.controltower.app.proposals.application;

import com.controltower.app.proposals.api.dto.ProposalResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProposalPdfService {

    private static final Color PRIMARY   = new Color(99, 102, 241);   // indigo-500
    private static final Color LIGHT_BG  = new Color(238, 242, 255);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);
    private static final Color BORDER    = new Color(229, 231, 235);

    private static final Font FONT_TITLE  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY);
    private static final Font FONT_H2     = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font FONT_BODY   = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FONT_MUTED  = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MUTED);
    private static final Font FONT_BOLD   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_WHITE  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONT_TOTAL  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY);

    public byte[] generate(ProposalResponse p) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, p);
            doc.add(Chunk.NEWLINE);
            addProposalInfo(doc, p);
            doc.add(Chunk.NEWLINE);
            addLineItems(doc, p);
            doc.add(Chunk.NEWLINE);
            addTotals(doc, p);

            if (p.notes() != null && !p.notes().isBlank()) {
                doc.add(Chunk.NEWLINE);
                addSection(doc, "Notas", p.notes());
            }
            if (p.terms() != null && !p.terms().isBlank()) {
                doc.add(Chunk.NEWLINE);
                addSection(doc, "Términos y Condiciones", p.terms());
            }

            addFooter(doc, p);
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("PDF generation failed for proposal {}", p.id(), e);
            throw new RuntimeException("Error generating PDF: " + e.getMessage(), e);
        }
    }

    // ── Sections ──────────────────────────────────────────────────────────────

    private void addHeader(Document doc, ProposalResponse p) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60, 40});

        // Left: Company / proposal title
        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("PROPUESTA ECONÓMICA", FONT_MUTED));
        left.addElement(new Paragraph(p.title(), FONT_TITLE));
        header.addCell(left);

        // Right: Number + status
        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph(p.number(), FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_MUTED)));
        right.addElement(new Paragraph("Estado: " + p.status().name(), FONT_MUTED));
        header.addCell(right);

        // Separator line
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        sep.setSpacingBefore(8);
        PdfPCell line = new PdfPCell();
        line.setFixedHeight(3);
        line.setBackgroundColor(PRIMARY);
        line.setBorder(Rectangle.NO_BORDER);
        sep.addCell(line);

        doc.add(header);
        doc.add(sep);
    }

    private void addProposalInfo(Document doc, ProposalResponse p) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});
        table.setSpacingBefore(10);

        addInfoCell(table, "Cliente", p.clientName() != null ? p.clientName() : "—");
        addInfoCell(table, "Email", p.clientEmail() != null ? p.clientEmail() : "—");
        addInfoCell(table, "Moneda", p.currency());
        addInfoCell(table, "Válido hasta", p.validityDate() != null
                ? p.validityDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Sin límite");
        addInfoCell(table, "Creado", p.createdAt() != null
                ? p.createdAt().toString().substring(0, 10) : "—");
        addInfoCell(table, "IVA", p.taxRate() + "%");

        doc.add(table);
    }

    private void addLineItems(Document doc, ProposalResponse p) throws Exception {
        Paragraph title = new Paragraph("Conceptos", FONT_H2);
        title.setSpacingBefore(8);
        doc.add(title);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{45, 15, 20, 20});
        table.setSpacingBefore(6);

        // Header row
        for (String h : new String[]{"Descripción", "Cantidad", "Precio Unitario", "Subtotal"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_WHITE));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        boolean alt = false;
        for (var item : p.lineItems()) {
            Color bg = alt ? LIGHT_BG : Color.WHITE;
            addItemCell(table, item.description(), bg, Element.ALIGN_LEFT);
            addItemCell(table, item.quantity().stripTrailingZeros().toPlainString(), bg, Element.ALIGN_CENTER);
            addItemCell(table, fmt(item.unitPrice(), p.currency()), bg, Element.ALIGN_RIGHT);
            addItemCell(table, fmt(item.subtotal(), p.currency()), bg, Element.ALIGN_RIGHT);
            alt = !alt;
        }

        doc.add(table);
    }

    private void addTotals(Document doc, ProposalResponse p) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{55, 45});
        table.setSpacingBefore(4);

        addTotalRow(table, "Subtotal:", fmt(p.subtotal(), p.currency()), false);

        if (p.discountAmount() != null && p.discountAmount().compareTo(BigDecimal.ZERO) > 0) {
            String discLabel = "PERCENTAGE".equals(p.discountType())
                    ? "Descuento (" + p.discountValue().stripTrailingZeros().toPlainString() + "%):"
                    : "Descuento:";
            Font discFont = FontFactory.getFont(FontFactory.HELVETICA, 10, new Color(220, 38, 38));
            PdfPCell lc = new PdfPCell(new Phrase(discLabel, discFont));
            lc.setBorder(Rectangle.NO_BORDER);
            lc.setPadding(4);
            lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(lc);
            PdfPCell vc = new PdfPCell(new Phrase("-" + fmt(p.discountAmount(), p.currency()), discFont));
            vc.setBorder(Rectangle.NO_BORDER);
            vc.setPadding(4);
            vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
            table.addCell(vc);
        }

        addTotalRow(table, "IVA (" + p.taxRate() + "%):", fmt(p.taxAmount(), p.currency()), false);

        PdfPCell labelCell = new PdfPCell(new Phrase("TOTAL:", FONT_TOTAL));
        labelCell.setBorder(Rectangle.TOP);
        labelCell.setPadding(8);
        labelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(labelCell);

        PdfPCell valueCell = new PdfPCell(new Phrase(fmt(p.total(), p.currency()), FONT_TOTAL));
        valueCell.setBorder(Rectangle.TOP);
        valueCell.setPadding(8);
        valueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(valueCell);

        doc.add(table);
    }

    private void addSection(Document doc, String heading, String text) throws Exception {
        Paragraph h = new Paragraph(heading, FONT_H2);
        h.setSpacingBefore(4);
        doc.add(h);
        Paragraph body = new Paragraph(text, FONT_BODY);
        body.setSpacingBefore(4);
        doc.add(body);
    }

    private void addFooter(Document doc, ProposalResponse p) throws Exception {
        doc.add(new Chunk(Chunk.NEWLINE));
        Paragraph footer = new Paragraph("Este documento fue generado por Control Tower · " + p.number(), FONT_MUTED);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    // ── Cell helpers ──────────────────────────────────────────────────────────

    private void addInfoCell(PdfPTable table, String label, String value) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(BORDER);
        cell.setPadding(6);
        cell.addElement(new Phrase(label, FONT_MUTED));
        cell.addElement(new Phrase(value, FONT_BOLD));
        table.addCell(cell);
    }

    private void addItemCell(PdfPTable table, String text, Color bg, int align) {
        PdfPCell cell = new PdfPCell(new Phrase(text, FONT_BODY));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean bold) {
        Font f = bold ? FONT_BOLD : FONT_BODY;
        PdfPCell lc = new PdfPCell(new Phrase(label, f));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setPadding(4);
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, f));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setPadding(4);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vc);
    }

    private String fmt(BigDecimal amount, String currency) {
        if (amount == null) return "$0.00";
        return String.format("$%,.2f %s", amount, currency != null ? currency : "MXN");
    }
}
