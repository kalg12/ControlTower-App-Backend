package com.controltower.app.finance.application;

import com.controltower.app.finance.api.dto.ExpenseSummaryResponse;
import com.controltower.app.finance.api.dto.InvoiceResponse;
import com.controltower.app.finance.api.dto.PnlReportResponse;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class FinancePdfService {

    private static final Color PRIMARY    = new Color(99, 102, 241);
    private static final Color GREEN      = new Color(22, 163, 74);
    private static final Color RED        = new Color(220, 38, 38);
    private static final Color LIGHT_BG   = new Color(238, 242, 255);
    private static final Color TEXT_MUTED = new Color(107, 114, 128);
    private static final Color BORDER     = new Color(229, 231, 235);

    private static final Font FONT_TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, PRIMARY);
    private static final Font FONT_H2    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, Color.BLACK);
    private static final Font FONT_H3    = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, PRIMARY);
    private static final Font FONT_BODY  = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.BLACK);
    private static final Font FONT_MUTED = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MUTED);
    private static final Font FONT_BOLD  = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.BLACK);
    private static final Font FONT_WHITE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
    private static final Font FONT_TOTAL = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, PRIMARY);
    private static final Font FONT_GREEN = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, GREEN);
    private static final Font FONT_RED   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, RED);

    // ── Invoice PDF ─────────────────────────────────────────────────────────

    public byte[] generateInvoicePdf(InvoiceResponse inv) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addInvoiceHeader(doc, inv);
            doc.add(Chunk.NEWLINE);
            addInvoiceInfo(doc, inv);
            doc.add(Chunk.NEWLINE);
            addInvoiceLineItems(doc, inv);
            doc.add(Chunk.NEWLINE);
            addInvoiceTotals(doc, inv);

            if (inv.notes() != null && !inv.notes().isBlank()) {
                doc.add(Chunk.NEWLINE);
                addSection(doc, "Notas", inv.notes());
            }

            addPdfFooter(doc, "Factura " + inv.number());
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Invoice PDF generation failed for {}", inv.id(), e);
            throw new RuntimeException("Error generating invoice PDF: " + e.getMessage(), e);
        }
    }

    private void addInvoiceHeader(Document doc, InvoiceResponse inv) throws Exception {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{60, 40});

        PdfPCell left = new PdfPCell();
        left.setBorder(Rectangle.NO_BORDER);
        left.addElement(new Paragraph("FACTURA", FONT_MUTED));
        left.addElement(new Paragraph(inv.number(), FONT_TITLE));
        header.addCell(left);

        PdfPCell right = new PdfPCell();
        right.setBorder(Rectangle.NO_BORDER);
        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
        right.addElement(new Paragraph("Estado: " + inv.status().name(), FONT_MUTED));
        right.addElement(new Paragraph(fmt(inv.total(), inv.currency()), FONT_TOTAL));
        header.addCell(right);

        doc.add(header);
        doc.add(separator());
    }

    private void addInvoiceInfo(Document doc, InvoiceResponse inv) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{50, 50});
        table.setSpacingBefore(10);

        addInfoCell(table, "Cliente", inv.clientName() != null ? inv.clientName() : "—");
        addInfoCell(table, "RFC / Tax ID", inv.clientTaxId() != null ? inv.clientTaxId() : "—");
        addInfoCell(table, "Fecha emisión", inv.issuedAt() != null ? inv.issuedAt().toString() : "—");
        addInfoCell(table, "Fecha vencimiento", inv.dueDate() != null ? inv.dueDate().toString() : "—");
        addInfoCell(table, "Moneda", inv.currency());
        addInfoCell(table, "IVA", inv.taxRate() + "%");

        doc.add(table);
    }

    private void addInvoiceLineItems(Document doc, InvoiceResponse inv) throws Exception {
        Paragraph title = new Paragraph("Conceptos", FONT_H2);
        title.setSpacingBefore(8);
        doc.add(title);

        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{45, 15, 20, 20});
        table.setSpacingBefore(6);

        for (String h : new String[]{"Descripción", "Cantidad", "Precio Unitario", "Subtotal"}) {
            PdfPCell cell = new PdfPCell(new Phrase(h, FONT_WHITE));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            table.addCell(cell);
        }

        boolean alt = false;
        for (var item : inv.lineItems()) {
            Color bg = alt ? LIGHT_BG : Color.WHITE;
            addItemCell(table, item.description(), bg, Element.ALIGN_LEFT);
            addItemCell(table, item.quantity().stripTrailingZeros().toPlainString(), bg, Element.ALIGN_CENTER);
            addItemCell(table, fmt(item.unitPrice(), inv.currency()), bg, Element.ALIGN_RIGHT);
            addItemCell(table, fmt(item.total(), inv.currency()), bg, Element.ALIGN_RIGHT);
            alt = !alt;
        }

        doc.add(table);
    }

    private void addInvoiceTotals(Document doc, InvoiceResponse inv) throws Exception {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(45);
        table.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.setWidths(new float[]{55, 45});
        table.setSpacingBefore(4);

        addTotalRow(table, "Subtotal:", fmt(inv.subtotal(), inv.currency()), false);
        addTotalRow(table, "IVA (" + inv.taxRate() + "%):", fmt(inv.taxAmount(), inv.currency()), false);

        PdfPCell lc = new PdfPCell(new Phrase("TOTAL:", FONT_TOTAL));
        lc.setBorder(Rectangle.TOP); lc.setPadding(8); lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(fmt(inv.total(), inv.currency()), FONT_TOTAL));
        vc.setBorder(Rectangle.TOP); vc.setPadding(8); vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vc);

        doc.add(table);
    }

    // ── Expense Report PDF ───────────────────────────────────────────────────

    public byte[] generateExpenseReportPdf(ExpenseSummaryResponse summary, String fromLabel, String toLabel) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Header
            Paragraph title = new Paragraph("REPORTE DE GASTOS", FONT_MUTED);
            doc.add(title);
            Paragraph subtitle = new Paragraph("Período: " + fromLabel + " — " + toLabel, FONT_TITLE);
            doc.add(subtitle);
            doc.add(separator());
            doc.add(Chunk.NEWLINE);

            // Grand total card
            PdfPTable card = new PdfPTable(1);
            card.setWidthPercentage(40);
            card.setHorizontalAlignment(Element.ALIGN_LEFT);
            PdfPCell cardCell = new PdfPCell();
            cardCell.setBackgroundColor(LIGHT_BG);
            cardCell.setBorder(Rectangle.NO_BORDER);
            cardCell.setPadding(12);
            cardCell.addElement(new Phrase("Total de Gastos", FONT_MUTED));
            cardCell.addElement(new Phrase(fmt(summary.grandTotal(), "MXN"), FONT_TOTAL));
            card.addCell(cardCell);
            doc.add(card);
            doc.add(Chunk.NEWLINE);

            // Category breakdown table
            Paragraph catTitle = new Paragraph("Desglose por Categoría", FONT_H2);
            catTitle.setSpacingBefore(8);
            doc.add(catTitle);

            PdfPTable catTable = new PdfPTable(4);
            catTable.setWidthPercentage(100);
            catTable.setWidths(new float[]{35, 25, 15, 25});
            catTable.setSpacingBefore(6);

            for (String h : new String[]{"Categoría", "Total", "Registros", "% del total"}) {
                PdfPCell cell = new PdfPCell(new Phrase(h, FONT_WHITE));
                cell.setBackgroundColor(PRIMARY);
                cell.setPadding(7);
                cell.setBorder(Rectangle.NO_BORDER);
                catTable.addCell(cell);
            }

            boolean alt = false;
            for (var cb : summary.byCategory()) {
                Color bg = alt ? LIGHT_BG : Color.WHITE;
                addItemCell(catTable, cb.category(), bg, Element.ALIGN_LEFT);
                addItemCell(catTable, fmt(cb.total(), "MXN"), bg, Element.ALIGN_RIGHT);
                addItemCell(catTable, String.valueOf(cb.count()), bg, Element.ALIGN_CENTER);
                addItemCell(catTable, String.format("%.1f%%", cb.percentage()), bg, Element.ALIGN_CENTER);
                alt = !alt;
            }
            doc.add(catTable);
            doc.add(Chunk.NEWLINE);

            // Monthly breakdown
            if (!summary.byMonth().isEmpty()) {
                Paragraph monthTitle = new Paragraph("Desglose Mensual", FONT_H2);
                monthTitle.setSpacingBefore(8);
                doc.add(monthTitle);

                PdfPTable monthTable = new PdfPTable(2);
                monthTable.setWidthPercentage(60);
                monthTable.setWidths(new float[]{50, 50});
                monthTable.setSpacingBefore(6);

                for (String h : new String[]{"Mes", "Total"}) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, FONT_WHITE));
                    cell.setBackgroundColor(PRIMARY);
                    cell.setPadding(7);
                    cell.setBorder(Rectangle.NO_BORDER);
                    monthTable.addCell(cell);
                }

                alt = false;
                for (var mb : summary.byMonth()) {
                    Color bg = alt ? LIGHT_BG : Color.WHITE;
                    addItemCell(monthTable, mb.month(), bg, Element.ALIGN_LEFT);
                    addItemCell(monthTable, fmt(mb.total(), "MXN"), bg, Element.ALIGN_RIGHT);
                    alt = !alt;
                }
                doc.add(monthTable);
            }

            addPdfFooter(doc, "Reporte de Gastos");
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Expense report PDF generation failed", e);
            throw new RuntimeException("Error generating expense report PDF: " + e.getMessage(), e);
        }
    }

    // ── P&L Report PDF ───────────────────────────────────────────────────────

    public byte[] generatePnlReportPdf(PnlReportResponse pnl, String fromLabel, String toLabel) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.LETTER, 50, 50, 60, 60);
            PdfWriter.getInstance(doc, out);
            doc.open();

            // Header
            doc.add(new Paragraph("ESTADO DE RESULTADOS (P&L)", FONT_MUTED));
            doc.add(new Paragraph("Período: " + fromLabel + " — " + toLabel, FONT_TITLE));
            doc.add(separator());
            doc.add(Chunk.NEWLINE);

            // Summary cards row
            PdfPTable cards = new PdfPTable(3);
            cards.setWidthPercentage(100);
            cards.setSpacingBefore(6);

            addPnlCard(cards, "INGRESOS", pnl.totalIncome(), GREEN);
            addPnlCard(cards, "EGRESOS", pnl.totalExpenses().negate(), RED);

            Font netFont = pnl.netProfit().compareTo(BigDecimal.ZERO) >= 0 ? FONT_GREEN : FONT_RED;
            PdfPCell netCard = new PdfPCell();
            netCard.setBackgroundColor(LIGHT_BG);
            netCard.setBorder(Rectangle.NO_BORDER);
            netCard.setPadding(12);
            netCard.addElement(new Phrase("UTILIDAD NETA", FONT_MUTED));
            netCard.addElement(new Phrase(fmt(pnl.netProfit(), "MXN"), netFont));
            netCard.addElement(new Phrase(String.format("Margen: %.1f%%", pnl.marginPct()), FONT_MUTED));
            cards.addCell(netCard);

            doc.add(cards);
            doc.add(Chunk.NEWLINE);

            // Monthly P&L table
            if (!pnl.byMonth().isEmpty()) {
                Paragraph monthTitle = new Paragraph("Desglose Mensual", FONT_H2);
                monthTitle.setSpacingBefore(8);
                doc.add(monthTitle);

                PdfPTable table = new PdfPTable(5);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{20, 20, 20, 20, 20});
                table.setSpacingBefore(6);

                for (String h : new String[]{"Mes", "Ingresos", "Gastos", "Compras", "Neto"}) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, FONT_WHITE));
                    cell.setBackgroundColor(PRIMARY);
                    cell.setPadding(7);
                    cell.setBorder(Rectangle.NO_BORDER);
                    table.addCell(cell);
                }

                boolean alt = false;
                for (var row : pnl.byMonth()) {
                    Color bg = alt ? LIGHT_BG : Color.WHITE;
                    addItemCell(table, row.month(), bg, Element.ALIGN_LEFT);
                    addItemCell(table, fmt(row.income(), "MXN"), bg, Element.ALIGN_RIGHT);
                    addItemCell(table, fmt(row.expenses(), "MXN"), bg, Element.ALIGN_RIGHT);
                    addItemCell(table, fmt(row.purchases(), "MXN"), bg, Element.ALIGN_RIGHT);
                    // Net cell with color
                    Color netColor = row.net().compareTo(BigDecimal.ZERO) >= 0 ? GREEN : RED;
                    Font netF = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, netColor);
                    PdfPCell nc = new PdfPCell(new Phrase(fmt(row.net(), "MXN"), netF));
                    nc.setBackgroundColor(bg);
                    nc.setBorder(Rectangle.NO_BORDER);
                    nc.setPadding(6);
                    nc.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    table.addCell(nc);
                    alt = !alt;
                }
                doc.add(table);
            }

            addPdfFooter(doc, "Estado de Resultados");
            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            log.error("P&L PDF generation failed", e);
            throw new RuntimeException("Error generating P&L PDF: " + e.getMessage(), e);
        }
    }

    // ── Helpers ────────────────────────────────────────────────────────────

    private PdfPTable separator() throws Exception {
        PdfPTable sep = new PdfPTable(1);
        sep.setWidthPercentage(100);
        sep.setSpacingBefore(8);
        PdfPCell line = new PdfPCell();
        line.setFixedHeight(3);
        line.setBackgroundColor(PRIMARY);
        line.setBorder(Rectangle.NO_BORDER);
        sep.addCell(line);
        return sep;
    }

    private void addSection(Document doc, String heading, String text) throws Exception {
        Paragraph h = new Paragraph(heading, FONT_H2);
        h.setSpacingBefore(4);
        doc.add(h);
        Paragraph body = new Paragraph(text, FONT_BODY);
        body.setSpacingBefore(4);
        doc.add(body);
    }

    private void addPdfFooter(Document doc, String label) throws Exception {
        doc.add(new Chunk(Chunk.NEWLINE));
        Paragraph footer = new Paragraph("Generado por Control Tower · " + label, FONT_MUTED);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

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
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "—", FONT_BODY));
        cell.setBackgroundColor(bg);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(6);
        cell.setHorizontalAlignment(align);
        table.addCell(cell);
    }

    private void addTotalRow(PdfPTable table, String label, String value, boolean bold) {
        Font f = bold ? FONT_BOLD : FONT_BODY;
        PdfPCell lc = new PdfPCell(new Phrase(label, f));
        lc.setBorder(Rectangle.NO_BORDER); lc.setPadding(4);
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, f));
        vc.setBorder(Rectangle.NO_BORDER); vc.setPadding(4);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vc);
    }

    private void addPnlCard(PdfPTable cards, String label, BigDecimal amount, Color color) {
        Font amtFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, color);
        PdfPCell card = new PdfPCell();
        card.setBackgroundColor(LIGHT_BG);
        card.setBorder(Rectangle.NO_BORDER);
        card.setPadding(12);
        card.addElement(new Phrase(label, FONT_MUTED));
        card.addElement(new Phrase(fmt(amount, "MXN"), amtFont));
        cards.addCell(card);
    }

    private String fmt(BigDecimal amount, String currency) {
        if (amount == null) return "$0.00";
        return String.format("$%,.2f %s", amount, currency != null ? currency : "MXN");
    }
}
