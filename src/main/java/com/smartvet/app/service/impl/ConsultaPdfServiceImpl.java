package com.smartvet.app.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.smartvet.app.exception.RecursoNoEncontradoException;
import com.smartvet.app.model.*;
import com.smartvet.app.repository.ConsultaRepository;
import com.smartvet.app.service.ConsultaPdfService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
public class ConsultaPdfServiceImpl implements ConsultaPdfService {

    // ── Paleta corporativa ────────────────────────────────────────────────────
    private static final Color NARANJA        = new Color(0xFF, 0x98, 0x00); // #FF9800
    private static final Color NARANJA_OSCURO = new Color(0xF5, 0x7C, 0x00); // #F57C00
    private static final Color NARANJA_LIGHT  = new Color(0xFF, 0xF3, 0xE0); // #FFF3E0
    private static final Color CHARCOAL       = new Color(0x26, 0x32, 0x38); // #263238
    private static final Color TEXTO_OSCURO   = new Color(0x1A, 0x1A, 0x1A);
    private static final Color TEXTO_MEDIO    = new Color(0x3D, 0x3D, 0x3D);
    private static final Color GRIS_FONDO     = new Color(0xF8, 0xF9, 0xFA); // label cells
    private static final Color GRIS_FONDO_ALT = new Color(0xF0, 0xF4, 0xF7); // alternate rows
    private static final Color GRIS_BORDE     = new Color(0xDE, 0xE2, 0xE6);
    private static final Color GRIS_PIE       = new Color(0x9E, 0x9E, 0x9E);
    private static final Color BLANCO         = new Color(0xFF, 0xFF, 0xFF);

    private static final DateTimeFormatter FMT       = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ConsultaRepository consultaRepository;

    public ConsultaPdfServiceImpl(ConsultaRepository consultaRepository) {
        this.consultaRepository = consultaRepository;
    }

    // ── Punto de entrada ──────────────────────────────────────────────────────

    @Override
    public byte[] generarPdf(Integer idConsulta) {
        Consulta c = consultaRepository.findById(idConsulta)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Consulta con id=" + idConsulta + " no encontrada"));

        // Inicializar asociaciones lazy dentro de la transacción
        Usuario propietario = c.getMascota().getPropietario();
        propietario.getNombres();
        c.getVeterinario().getUsuario().getNombres();
        c.getCita().getTipoCita();
        List<Receta> recetas = c.getRecetas();
        if (recetas != null) {
            recetas.forEach(r -> {
                if (r.getDetalles() != null) {
                    r.getDetalles().forEach(d -> d.getProducto().getNombre());
                }
            });
        }

        byte[] logoBytes = cargarLogo();

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 48, 48, 48, 52);
            PdfWriter.getInstance(doc, baos);
            doc.open();

            cabecera(doc, c, logoBytes);
            seccion(doc, "DATOS DE LA CITA");
            tablaDatos2Col(doc, c);
            seccion(doc, "PACIENTE Y PROPIETARIO");
            tablaCompacta4Col(doc, c);
            seccion(doc, "VETERINARIO RESPONSABLE");
            tablaVeterinario(doc, c);
            seccion(doc, "SIGNOS VITALES");
            tarjetasSignosVitales(doc, c);
            seccion(doc, "DIAGNÓSTICO");
            bloqueTexto(doc, c.getDiagnostico());
            if (noBlank(c.getTratamiento())) {
                seccion(doc, "TRATAMIENTO");
                bloqueTexto(doc, c.getTratamiento());
            }
            if (noBlank(c.getObservaciones())) {
                seccion(doc, "OBSERVACIONES");
                bloqueTexto(doc, c.getObservaciones());
            }
            if (recetas != null && !recetas.isEmpty()) {
                seccion(doc, "RECETA MÉDICA");
                recetas.forEach(r -> {
                    try { tablaReceta(doc, r); }
                    catch (DocumentException e) { throw new RuntimeException(e); }
                });
            }
            pie(doc);

            doc.close();
            log.info("PDF premium generado: consulta_id={}", idConsulta);
            return baos.toByteArray();
        } catch (Exception ex) {
            log.error("Error generando PDF consulta_id={}", idConsulta, ex);
            throw new RuntimeException("No se pudo generar el PDF de la consulta.", ex);
        }
    }

    // ── Carga del logo desde classpath ────────────────────────────────────────

    private byte[] cargarLogo() {
        try {
            ClassPathResource resource = new ClassPathResource("static/img/logo.jpg");
            try (InputStream is = resource.getInputStream()) {
                return is.readAllBytes();
            }
        } catch (Exception ex) {
            log.warn("Logo no disponible, se omitirá del PDF: {}", ex.getMessage());
            return null;
        }
    }

    // ── Cabecera premium ──────────────────────────────────────────────────────

    private void cabecera(Document doc, Consulta c, byte[] logoBytes) throws DocumentException {
        /* ── Franja 1: Logo (izquierda) + Datos de la clínica (derecha) ── */
        PdfPTable franjaInfo = anchoCompleto(2);
        franjaInfo.setWidths(new float[]{26f, 74f});
        franjaInfo.setSpacingAfter(0);

        // Celda logo
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
                log.warn("No se pudo insertar el logo: {}", ex.getMessage());
                celdaLogo.addElement(logoFallback());
            }
        } else {
            celdaLogo.addElement(logoFallback());
        }
        franjaInfo.addCell(celdaLogo);

        // Celda datos clínica
        PdfPCell celdaInfo = new PdfPCell();
        celdaInfo.setBackgroundColor(BLANCO);
        celdaInfo.setBorder(Rectangle.NO_BORDER);
        celdaInfo.setPaddingLeft(6);
        celdaInfo.setPaddingRight(12);
        celdaInfo.setPaddingTop(18);
        celdaInfo.setPaddingBottom(14);
        celdaInfo.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Font fNombre   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 15, TEXTO_OSCURO);
        Font fSlogan   = FontFactory.getFont(FontFactory.HELVETICA, 9, NARANJA_OSCURO);
        Font fContacto = FontFactory.getFont(FontFactory.HELVETICA, 8, GRIS_PIE);

        Paragraph pNombre = new Paragraph("VETERINARIA SANTA VICTORIA", fNombre);
        pNombre.setSpacingAfter(3);
        Paragraph pSlogan = new Paragraph("Clínica Veterinaria de Alta Especialidad", fSlogan);
        pSlogan.setSpacingAfter(7);
        Paragraph pContacto = new Paragraph(
                "Lima, Perú   |   Tel. (01) 234-5678   |   info@santavictoria.vet", fContacto);

        celdaInfo.addElement(pNombre);
        celdaInfo.addElement(pSlogan);
        celdaInfo.addElement(pContacto);
        franjaInfo.addCell(celdaInfo);
        doc.add(franjaInfo);

        /* ── Línea de acento naranja (4 px) ── */
        doc.add(franjaColor(NARANJA, 4f));

        /* ── Franja 2: Título del documento (izquierda, charcoal) + Referencia (derecha, naranja) ── */
        PdfPTable fanjaTitulo = anchoCompleto(2);
        fanjaTitulo.setWidths(new float[]{65f, 35f});
        fanjaTitulo.setSpacingAfter(18);

        PdfPCell celdaTitulo = new PdfPCell();
        celdaTitulo.setBackgroundColor(CHARCOAL);
        celdaTitulo.setBorder(Rectangle.NO_BORDER);
        celdaTitulo.setPaddingLeft(18);
        celdaTitulo.setPaddingTop(13);
        celdaTitulo.setPaddingBottom(13);
        celdaTitulo.setVerticalAlignment(Element.ALIGN_MIDDLE);

        Paragraph pTitulo = new Paragraph("INFORME DE CONSULTA MÉDICA",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13, BLANCO));
        pTitulo.setSpacingAfter(3);
        Paragraph pSubtitulo = new Paragraph("Documento médico confidencial",
                FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(0xB0, 0xBE, 0xC5)));
        celdaTitulo.addElement(pTitulo);
        celdaTitulo.addElement(pSubtitulo);
        fanjaTitulo.addCell(celdaTitulo);

        PdfPCell celdaRef = new PdfPCell();
        celdaRef.setBackgroundColor(NARANJA);
        celdaRef.setBorder(Rectangle.NO_BORDER);
        celdaRef.setPaddingRight(18);
        celdaRef.setPaddingLeft(10);
        celdaRef.setPaddingTop(10);
        celdaRef.setPaddingBottom(10);
        celdaRef.setVerticalAlignment(Element.ALIGN_MIDDLE);
        celdaRef.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Paragraph pRefLabel = new Paragraph("CONSULTA N°",
                FontFactory.getFont(FontFactory.HELVETICA, 7, new Color(0xFF, 0xE0, 0xB2)));
        pRefLabel.setAlignment(Element.ALIGN_RIGHT);
        pRefLabel.setSpacingAfter(1);

        Paragraph pRefNum = new Paragraph(String.format("%04d", c.getIdConsulta()),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BLANCO));
        pRefNum.setAlignment(Element.ALIGN_RIGHT);
        pRefNum.setSpacingAfter(3);

        Paragraph pRefFecha = new Paragraph(LocalDateTime.now().format(FMT),
                FontFactory.getFont(FontFactory.HELVETICA, 8, BLANCO));
        pRefFecha.setAlignment(Element.ALIGN_RIGHT);

        celdaRef.addElement(pRefLabel);
        celdaRef.addElement(pRefNum);
        celdaRef.addElement(pRefFecha);
        fanjaTitulo.addCell(celdaRef);
        doc.add(fanjaTitulo);
    }

    private Paragraph logoFallback() {
        Paragraph p = new Paragraph("SV",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 28, NARANJA));
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    // ── Cabeceras de sección con acento izquierdo ─────────────────────────────

    private void seccion(Document doc, String titulo) throws DocumentException {
        PdfPTable t = anchoCompleto(2);
        t.setWidths(new float[]{1.5f, 98.5f});
        t.setSpacingBefore(14);
        t.setSpacingAfter(4);

        // Barra naranja de acento
        PdfPCell acento = new PdfPCell(new Phrase(""));
        acento.setBackgroundColor(NARANJA);
        acento.setBorder(Rectangle.NO_BORDER);
        acento.setFixedHeight(26f);
        t.addCell(acento);

        // Etiqueta de sección
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

    private void tablaDatos2Col(Document doc, Consulta c) throws DocumentException {
        PdfPTable t = tabla2Col(32f, 68f);
        fila(t, "Número de cita",   "#" + c.getCita().getIdCita());
        fila(t, "Tipo",             c.getCita().getTipoCita().name());
        fila(t, "Fecha de la cita", c.getCita().getFechaHora().format(FMT));
        fila(t, "Fecha consulta",   c.getFechaHora().format(FMT));
        if (noBlank(c.getCita().getMotivo())) {
            fila(t, "Motivo", c.getCita().getMotivo());
        }
        doc.add(t);
    }

    private void tablaCompacta4Col(Document doc, Consulta c) throws DocumentException {
        Mascota m = c.getMascota();
        Usuario p = m.getPropietario();

        PdfPTable t = anchoCompleto(4);
        t.setWidths(new float[]{19f, 31f, 19f, 31f});
        t.setSpacingAfter(4);

        fila4(t, "Nombre",   m.getNombre(),
                  "Especie", m.getEspecie());
        fila4(t, "Raza",     m.getRaza() != null ? m.getRaza() : "—",
                  "Sexo",    m.getSexo() != null ? m.getSexo().name() : "—");

        String nacimiento = m.getFechaNacimiento() != null
                ? m.getFechaNacimiento().format(FMT_FECHA) : "—";
        fila4(t, "Nacimiento", nacimiento,
                  "Propietario", p.getNombres() + " " + p.getApellidos());
        fila4(t, "Correo", p.getEmail(),
                  "Teléfono", noBlank(p.getTelefono()) ? p.getTelefono() : "—");
        doc.add(t);
    }

    private void tablaVeterinario(Document doc, Consulta c) throws DocumentException {
        PdfPTable t = tabla2Col(32f, 68f);
        fila(t, "Profesional a cargo",
                c.getVeterinario().getUsuario().getNombres()
                        + " " + c.getVeterinario().getUsuario().getApellidos());
        doc.add(t);
    }

    // ── Tarjetas de signos vitales ────────────────────────────────────────────

    private void tarjetasSignosVitales(Document doc, Consulta c) throws DocumentException {
        PdfPTable t = anchoCompleto(3);
        t.setSpacingAfter(6);

        Font fLabel = FontFactory.getFont(FontFactory.HELVETICA, 7, GRIS_PIE);
        Font fVal   = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, TEXTO_OSCURO);
        Font fUnit  = FontFactory.getFont(FontFactory.HELVETICA, 9, NARANJA);

        tarjeta(t, "TEMPERATURA",
                c.getTemperatura() != null ? c.getTemperatura().toPlainString() : "—",
                c.getTemperatura() != null ? "°C" : "", fLabel, fVal, fUnit);

        tarjeta(t, "PESO",
                c.getPeso() != null ? c.getPeso().toPlainString() : "—",
                c.getPeso() != null ? "kg" : "", fLabel, fVal, fUnit);

        tarjeta(t, "FREC. CARDÍACA",
                c.getFrecuenciaCardiaca() != null ? String.valueOf(c.getFrecuenciaCardiaca()) : "—",
                c.getFrecuenciaCardiaca() != null ? "lpm" : "", fLabel, fVal, fUnit);

        doc.add(t);
    }

    private void tarjeta(PdfPTable t, String label, String valor, String unidad,
                          Font fLabel, Font fVal, Font fUnit) {
        PdfPCell card = new PdfPCell();
        card.setBackgroundColor(GRIS_FONDO);
        card.setBorderColor(GRIS_BORDE);
        card.setPaddingLeft(14);
        card.setPaddingRight(10);
        card.setPaddingTop(11);
        card.setPaddingBottom(11);

        Paragraph pLabel = new Paragraph(label, fLabel);
        pLabel.setSpacingAfter(3);

        Paragraph pVal = new Paragraph();
        pVal.add(new Chunk(valor, fVal));
        if (!unidad.isEmpty()) {
            pVal.add(new Chunk("  " + unidad, fUnit));
        }

        card.addElement(pLabel);
        card.addElement(pVal);
        t.addCell(card);
    }

    // ── Bloque de texto libre ─────────────────────────────────────────────────

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

    // ── Tabla de receta médica ────────────────────────────────────────────────

    private void tablaReceta(Document doc, Receta receta) throws DocumentException {
        List<DetalleReceta> detalles = receta.getDetalles();
        if (detalles != null && !detalles.isEmpty()) {
            Font fHead = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, BLANCO);
            Font fProd = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXTO_OSCURO);
            Font fBody = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXTO_MEDIO);

            PdfPTable t = anchoCompleto(5);
            t.setWidths(new float[]{27f, 11f, 19f, 13f, 30f});
            t.setSpacingAfter(8);
            t.setHeaderRows(1);

            for (String h : new String[]{"MEDICAMENTO", "CANT.", "DOSIS", "DURACIÓN", "INSTRUCCIONES"}) {
                PdfPCell hc = new PdfPCell(new Phrase(h, fHead));
                hc.setBackgroundColor(CHARCOAL);
                hc.setBorderColor(GRIS_BORDE);
                hc.setPaddingLeft(8);
                hc.setPaddingTop(8);
                hc.setPaddingBottom(8);
                t.addCell(hc);
            }

            boolean alt = false;
            for (DetalleReceta d : detalles) {
                Color bg = alt ? GRIS_FONDO_ALT : BLANCO;
                celdaDato(t, d.getProducto().getNombre(), fProd, bg, false);
                celdaDato(t, String.valueOf(d.getCantidad()), fBody, bg, true);
                celdaDato(t, d.getDosis() != null ? d.getDosis() : "—", fBody, bg, false);
                celdaDato(t, d.getDuracionDias() != null ? d.getDuracionDias() + " días" : "—", fBody, bg, true);
                celdaDato(t, d.getInstruccionesAdicionales() != null
                        ? d.getInstruccionesAdicionales() : "—", fBody, bg, false);
                alt = !alt;
            }
            doc.add(t);
        }

        Font fLabel = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, TEXTO_MEDIO);
        Font fTexto = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXTO_MEDIO);

        if (noBlank(receta.getInstrucciones())) {
            Paragraph p = new Paragraph();
            p.add(new Chunk("Instrucciones generales: ", fLabel));
            p.add(new Chunk(receta.getInstrucciones(), fTexto));
            p.setIndentationLeft(14);
            p.setSpacingAfter(3);
            doc.add(p);
        }
        if (receta.getVigenciaDias() != null) {
            Paragraph p = new Paragraph();
            p.add(new Chunk("Vigencia de la receta: ", fLabel));
            p.add(new Chunk(receta.getVigenciaDias() + " días", fTexto));
            p.setIndentationLeft(14);
            doc.add(p);
        }
    }

    // ── Pie de página ─────────────────────────────────────────────────────────

    private void pie(Document doc) throws DocumentException {
        // Spacer de 32 pt para garantizar separación entre el último bloque de contenido
        // y el footer — evita superposición de texto sobre la franja naranja
        Paragraph espaciado = new Paragraph(" ");
        espaciado.setSpacingBefore(32f);
        doc.add(espaciado);

        doc.add(franjaColor(NARANJA, 3f));

        PdfPTable t = anchoCompleto(2);
        t.setSpacingAfter(0);

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
                "Generado el " + LocalDateTime.now().format(FMT) + "   |   Documento confidencial", fPie);
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

    /** Fila clave-valor para tabla de 2 columnas */
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

    /** Fila compacta de 4 columnas (2 pares clave-valor por fila) */
    private void fila4(PdfPTable t,
                        String e1, String v1,
                        String e2, String v2) {
        Font fE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8, TEXTO_MEDIO);
        Font fV = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXTO_OSCURO);

        for (String[] par : new String[][]{{e1, v1}, {e2, v2}}) {
            PdfPCell cE = new PdfPCell(new Phrase(par[0], fE));
            cE.setBackgroundColor(GRIS_FONDO);
            cE.setBorderColor(GRIS_BORDE);
            cE.setPaddingLeft(8);
            cE.setPaddingTop(7);
            cE.setPaddingBottom(7);

            PdfPCell cV = new PdfPCell(new Phrase(par[1], fV));
            cV.setBackgroundColor(BLANCO);
            cV.setBorderColor(GRIS_BORDE);
            cV.setPaddingLeft(8);
            cV.setPaddingTop(7);
            cV.setPaddingBottom(7);

            t.addCell(cE);
            t.addCell(cV);
        }
    }

    private void celdaDato(PdfPTable t, String texto, Font font, Color bg, boolean noWrap) {
        PdfPCell c = new PdfPCell(new Phrase(texto, font));
        c.setBackgroundColor(bg);
        c.setBorderColor(GRIS_BORDE);
        c.setPaddingLeft(8);
        c.setPaddingTop(6);
        c.setPaddingBottom(6);
        c.setPaddingRight(6);
        if (noWrap) c.setNoWrap(true);
        t.addCell(c);
    }

    /** Tabla de una fila con altura fija para crear franjas de color */
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
