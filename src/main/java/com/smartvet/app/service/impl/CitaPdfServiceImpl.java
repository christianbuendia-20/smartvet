package com.smartvet.app.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.Cita;
import com.smartvet.app.model.Mascota;
import com.smartvet.app.model.Usuario;
import com.smartvet.app.repository.CitaRepository;
import com.smartvet.app.service.CitaPdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@Transactional(readOnly = true)
public class CitaPdfServiceImpl implements CitaPdfService {

    // ── Paleta corporativa ────────────────────────────────────────────────────
    private static final Color NARANJA        = new Color(0xFF, 0x98, 0x00);
    private static final Color NARANJA_OSCURO = new Color(0xF5, 0x7C, 0x00);
    private static final Color NARANJA_LIGHT  = new Color(0xFF, 0xF3, 0xE0);
    private static final Color CHARCOAL       = new Color(0x26, 0x32, 0x38);
    private static final Color TEXTO_OSCURO   = new Color(0x1A, 0x1A, 0x1A);
    private static final Color TEXTO_MEDIO    = new Color(0x3D, 0x3D, 0x3D);
    private static final Color GRIS_FONDO     = new Color(0xF8, 0xF9, 0xFA);
    private static final Color GRIS_BORDE     = new Color(0xDE, 0xE2, 0xE6);
    private static final Color GRIS_PIE       = new Color(0x9E, 0x9E, 0x9E);
    private static final Color BLANCO         = new Color(0xFF, 0xFF, 0xFF);

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CitaRepository citaRepository;

    public CitaPdfServiceImpl(CitaRepository citaRepository) {
        this.citaRepository = citaRepository;
    }

    // ── Punto de entrada ──────────────────────────────────────────────────────

    @Override
    public byte[] generarPdf(Integer idCita) {
        Cita cita = citaRepository.findByIdWithDetalle(idCita)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Cita con id=" + idCita + " no encontrada"));

        byte[] logoBytes = cargarLogo();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 48, 48, 48, 52);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            cabecera(doc, cita, logoBytes);
            seccion(doc, "DETALLES DE LA CITA");
            tablaDetalles(doc, cita);
            seccion(doc, "PACIENTE Y PROPIETARIO");
            tablaPaciente(doc, cita);
            seccion(doc, "VETERINARIO ASIGNADO");
            tablaVeterinario(doc, cita);
            seccion(doc, "MOTIVO DE CONSULTA");
            bloqueTexto(doc, cita.getMotivo());
            pie(doc);

            doc.close();
            log.info("Comprobante PDF generado: cita_id={}", idCita);
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Error generando comprobante PDF cita_id={}", idCita, ex);
            throw new RuntimeException("No se pudo generar el comprobante de la cita.", ex);
        }
    }

    // ── Logo ──────────────────────────────────────────────────────────────────

    private byte[] cargarLogo() {
        try {
            ClassPathResource res = new ClassPathResource("static/img/logo.jpg");
            try (InputStream is = res.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (Exception ex) {
            log.warn("Logo no disponible para el PDF: {}", ex.getMessage());
            return null;
        }
    }

    // ── Cabecera ──────────────────────────────────────────────────────────────

    private void cabecera(Document doc, Cita cita, byte[] logoBytes) throws DocumentException {
        /* ── Franja 1: Logo + Datos clínica ── */
        PdfPTable franjaInfo = anchoCompleto(2);
        franjaInfo.setWidths(new float[]{26f, 74f});
        franjaInfo.setSpacingAfter(0);

        PdfPCell celdaLogo = new PdfPCell();
        celdaLogo.setBackgroundColor(BLANCO);
        celdaLogo.setBorder(Rectangle.NO_BORDER);
        celdaLogo.setPadding(12);
        celdaLogo.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaLogo.setHorizontalAlignment(Element.ALIGN_CENTER);

        if (logoBytes != null) {
            try {
                Image logo = Image.getInstance(logoBytes);
                logo.scaleToFit(90f, 68f);
                logo.setAlignment(Image.ALIGN_CENTER);
                celdaLogo.addElement(logo);
            } catch (Exception ex) {
                celdaLogo.addElement(logoFallback());
            }
        } else {
            celdaLogo.addElement(logoFallback());
        }
        franjaInfo.addCell(celdaLogo);

        PdfPCell celdaInfo = new PdfPCell();
        celdaInfo.setBackgroundColor(BLANCO);
        celdaInfo.setBorder(Rectangle.NO_BORDER);
        celdaInfo.setPaddingLeft(6);
        celdaInfo.setPaddingRight(12);
        celdaInfo.setPaddingTop(18);
        celdaInfo.setPaddingBottom(14);
        celdaInfo.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph pNombre = new Paragraph("VETERINARIA SANTA VICTORIA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, TEXTO_OSCURO));
        pNombre.setSpacingAfter(3);
        Paragraph pSlogan = new Paragraph("Clínica Veterinaria de Alta Especialidad",
                FontFactory.getFont(FontFactory.HELVETICA, 9, NARANJA_OSCURO));
        pSlogan.setSpacingAfter(7);
        Paragraph pContacto = new Paragraph(
                "Lima, Perú   |   Tel. (01) 234-5678   |   WhatsApp: +51 999 888 777",
                FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS_PIE));

        celdaInfo.addElement(pNombre);
        celdaInfo.addElement(pSlogan);
        celdaInfo.addElement(pContacto);
        franjaInfo.addCell(celdaInfo);
        doc.add(franjaInfo);

        /* ── Línea de acento naranja (4 px) ── */
        doc.add(franjaColor(NARANJA, 4f));

        /* ── Franja 2: Título del documento + Número de cita ── */
        PdfPTable franjaTitulo = anchoCompleto(2);
        franjaTitulo.setWidths(new float[]{65f, 35f});
        franjaTitulo.setSpacingAfter(18);

        PdfPCell celdaTitulo = new PdfPCell();
        celdaTitulo.setBackgroundColor(CHARCOAL);
        celdaTitulo.setBorder(Rectangle.NO_BORDER);
        celdaTitulo.setPaddingLeft(18);
        celdaTitulo.setPaddingTop(13);
        celdaTitulo.setPaddingBottom(13);
        celdaTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph pTitulo = new Paragraph("COMPROBANTE DE CITA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BLANCO));
        pTitulo.setSpacingAfter(3);
        Paragraph pSubtitulo = new Paragraph("Veterinaria Santa Victoria — Documento de agendamiento",
                FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(0xB0, 0xBE, 0xC5)));
        celdaTitulo.addElement(pTitulo);
        celdaTitulo.addElement(pSubtitulo);
        franjaTitulo.addCell(celdaTitulo);

        PdfPCell celdaRef = new PdfPCell();
        celdaRef.setBackgroundColor(NARANJA);
        celdaRef.setBorder(Rectangle.NO_BORDER);
        celdaRef.setPaddingRight(18);
        celdaRef.setPaddingLeft(10);
        celdaRef.setPaddingTop(10);
        celdaRef.setPaddingBottom(10);
        celdaRef.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaRef.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph pRefLabel = new Paragraph("CITA N°",
                FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(0xFF, 0xE0, 0xB2)));
        pRefLabel.setAlignment(Element.ALIGN_RIGHT);
        pRefLabel.setSpacingAfter(1);

        Paragraph pRefNum = new Paragraph(String.format("%04d", cita.getIdCita()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLANCO));
        pRefNum.setAlignment(Element.ALIGN_RIGHT);
        pRefNum.setSpacingAfter(3);

        Paragraph pRefFecha = new Paragraph(LocalDateTime.now().format(FMT),
                FontFactory.getFont(FontFactory.HELVETICA, 8, BLANCO));
        pRefFecha.setAlignment(Element.ALIGN_RIGHT);

        celdaRef.addElement(pRefLabel);
        celdaRef.addElement(pRefNum);
        celdaRef.addElement(pRefFecha);
        franjaTitulo.addCell(celdaRef);
        doc.add(franjaTitulo);
    }

    private Paragraph logoFallback() {
        Paragraph p = new Paragraph("SV",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, NARANJA));
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    // ── Cabecera de sección ───────────────────────────────────────────────────

    private void seccion(Document doc, String titulo) throws DocumentException {
        PdfPTable t = anchoCompleto(2);
        t.setWidths(new float[]{1.5f, 98.5f});
        t.setSpacingBefore(14);
        t.setSpacingAfter(4);

        PdfPCell acento = new PdfPCell(new Phrase(""));
        acento.setBackgroundColor(NARANJA);
        acento.setBorder(Rectangle.NO_BORDER);
        acento.setFixedHeight(26f);
        t.addCell(acento);

        PdfPCell etiqueta = new PdfPCell(new Phrase("   " + titulo,
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXTO_OSCURO)));
        etiqueta.setBackgroundColor(NARANJA_LIGHT);
        etiqueta.setBorder(Rectangle.NO_BORDER);
        etiqueta.setPaddingTop(7);
        etiqueta.setPaddingBottom(7);
        etiqueta.setPaddingLeft(4);
        etiqueta.setVerticalAlignment(Element.ALIGN_MIDDLE);
        t.addCell(etiqueta);

        doc.add(t);
    }

    // ── Tablas de datos ───────────────────────────────────────────────────────

    private void tablaDetalles(Document doc, Cita cita) throws DocumentException {
        PdfPTable t = tabla2Col(32f, 68f);
        fila(t, "Número de cita",   "#" + cita.getIdCita());
        fila(t, "Tipo de consulta", cita.getTipoCita().name());
        fila(t, "Fecha y hora",     cita.getFechaHora().format(FMT));
        fila(t, "Estado",           cita.getEstado().name());
        doc.add(t);
    }

    private void tablaPaciente(Document doc, Cita cita) throws DocumentException {
        Mascota m    = cita.getMascota();
        Usuario prop = m.getPropietario();

        PdfPTable t = tabla2Col(32f, 68f);
        fila(t, "Mascota",     m.getNombre() + " (" + m.getEspecie() + ")");
        fila(t, "Raza",        m.getRaza() != null ? m.getRaza() : "—");
        fila(t, "Sexo",        m.getSexo() != null ? m.getSexo().name() : "—");
        fila(t, "Propietario", prop.getNombres() + " " + prop.getApellidos());
        fila(t, "Correo",      prop.getEmail());
        fila(t, "Teléfono",    noBlank(prop.getTelefono()) ? prop.getTelefono() : "—");
        doc.add(t);
    }

    private void tablaVeterinario(Document doc, Cita cita) throws DocumentException {
        PdfPTable t = tabla2Col(32f, 68f);
        fila(t, "Profesional asignado",
                cita.getVeterinario().getUsuario().getNombres()
                        + " " + cita.getVeterinario().getUsuario().getApellidos());
        doc.add(t);
    }

    private void bloqueTexto(Document doc, String texto) throws DocumentException {
        PdfPTable t = anchoCompleto(1);
        t.setSpacingAfter(4);

        PdfPCell cell = new PdfPCell(new Phrase(
                texto != null ? texto : "—",
                FontFactory.getFont(FontFactory.HELVETICA, 10, TEXTO_MEDIO)));
        cell.setBackgroundColor(BLANCO);
        cell.setBorderColor(GRIS_BORDE);
        cell.setPaddingLeft(14);
        cell.setPaddingRight(14);
        cell.setPaddingTop(10);
        cell.setPaddingBottom(10);
        t.addCell(cell);
        doc.add(t);
    }

    // ── Pie de página ─────────────────────────────────────────────────────────

    private void pie(Document doc) throws DocumentException {
        Paragraph espaciado = new Paragraph(" ");
        espaciado.setSpacingBefore(32f);
        doc.add(espaciado);

        PdfPTable tAviso = anchoCompleto(1);
        tAviso.setSpacingAfter(8);
        PdfPCell cAviso = new PdfPCell(new Phrase(
                "Por favor asista 10 minutos antes de su cita.   |   " +
                "Consultas al WhatsApp: +51 999 888 777",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, NARANJA_OSCURO)));
        cAviso.setBackgroundColor(NARANJA_LIGHT);
        cAviso.setBorderColor(GRIS_BORDE);
        cAviso.setPaddingLeft(14);
        cAviso.setPaddingRight(14);
        cAviso.setPaddingTop(9);
        cAviso.setPaddingBottom(9);
        cAviso.setHorizontalAlignment(Element.ALIGN_CENTER);
        tAviso.addCell(cAviso);
        doc.add(tAviso);

        doc.add(franjaColor(NARANJA, 3f));

        PdfPTable t = anchoCompleto(2);
        Font fPie  = FontFactory.getFont(FontFactory.HELVETICA, 7, GRIS_PIE);
        Font fPieB = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 7, TEXTO_MEDIO);

        PdfPCell cIzq = new PdfPCell();
        cIzq.setBackgroundColor(GRIS_FONDO);
        cIzq.setBorder(Rectangle.NO_BORDER);
        cIzq.setPaddingLeft(10);
        cIzq.setPaddingTop(8);
        cIzq.setPaddingBottom(8);
        Paragraph pIzq = new Paragraph();
        pIzq.add(new Chunk("Veterinaria Santa Victoria", fPieB));
        pIzq.add(new Chunk("  ·  Lima, Perú  ·  Tel. (01) 234-5678", fPie));
        cIzq.addElement(pIzq);

        PdfPCell cDer = new PdfPCell();
        cDer.setBackgroundColor(GRIS_FONDO);
        cDer.setBorder(Rectangle.NO_BORDER);
        cDer.setPaddingRight(10);
        cDer.setPaddingTop(8);
        cDer.setPaddingBottom(8);
        Paragraph pDer = new Paragraph(
                "Generado el " + LocalDateTime.now().format(FMT) + "   |   SmartVet", fPie);
        pDer.setAlignment(Element.ALIGN_RIGHT);
        cDer.addElement(pDer);

        t.addCell(cIzq);
        t.addCell(cDer);
        doc.add(t);
    }

    // ── Helpers de construcción ───────────────────────────────────────────────

    private PdfPTable anchoCompleto(int cols) {
        PdfPTable t = new PdfPTable(cols);
        t.setWidthPercentage(100);
        return t;
    }

    private PdfPTable tabla2Col(float pctLabel, float pctValor) throws DocumentException {
        PdfPTable t = anchoCompleto(2);
        t.setWidths(new float[]{pctLabel, pctValor});
        t.setSpacingAfter(4);
        return t;
    }

    private void fila(PdfPTable t, String etiqueta, String valor) {
        Font fE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXTO_MEDIO);
        Font fV = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXTO_OSCURO);

        PdfPCell cE = new PdfPCell(new Phrase(etiqueta, fE));
        cE.setBackgroundColor(GRIS_FONDO);
        cE.setBorderColor(GRIS_BORDE);
        cE.setPaddingLeft(10);
        cE.setPaddingTop(7);
        cE.setPaddingBottom(7);

        PdfPCell cV = new PdfPCell(new Phrase(valor, fV));
        cV.setBackgroundColor(BLANCO);
        cV.setBorderColor(GRIS_BORDE);
        cV.setPaddingLeft(10);
        cV.setPaddingTop(7);
        cV.setPaddingBottom(7);

        t.addCell(cE);
        t.addCell(cV);
    }

    private PdfPTable franjaColor(Color color, float altura) {
        PdfPTable t = anchoCompleto(1);
        t.setSpacingBefore(0);
        t.setSpacingAfter(0);
        PdfPCell c = new PdfPCell(new Phrase(""));
        c.setBackgroundColor(color);
        c.setBorder(Rectangle.NO_BORDER);
        c.setFixedHeight(altura);
        t.addCell(c);
        return t;
    }

    private boolean noBlank(String s) {
        return s != null && !s.isBlank();
    }
}
