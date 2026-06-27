package com.controltower.app.ai.application;

import com.controltower.app.ai.api.dto.AiAssistRequest;
import com.controltower.app.ai.api.dto.AiAssistRequest.AiTask;
import com.controltower.app.ai.api.dto.AiAssistRequest.QuickReplyType;
import com.controltower.app.shared.exception.ControlTowerException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AiAssistService {

    private final RestClient restClient;
    private final String model;

    public AiAssistService(
            @Value("${app.ai.base-url}") String baseUrl,
            @Value("${app.ai.api-key}") String apiKey,
            @Value("${app.ai.model:gpt-4o-mini}") String model,
            @Value("${app.ai.timeout-ms:45000}") long timeoutMs
    ) {
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public String assist(AiTask task, AiAssistRequest.AiContext ctx) {
        String systemPrompt = buildSystemPrompt(task);
        String userPrompt   = buildUserPrompt(task, ctx);

        Map<String, Object> body = Map.of(
            "model", model,
            "messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user",   "content", userPrompt)
            ),
            "max_tokens", 1200,
            "temperature", 0.7
        );

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty()) {
                throw new ControlTowerException("AI returned no choices", HttpStatus.BAD_GATEWAY);
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");

        } catch (ControlTowerException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI assist call failed [{}]: {}", e.getClass().getSimpleName(), e.getMessage(), e);
            throw new ControlTowerException("Error al conectar con el servicio de IA: " + e.getMessage(), HttpStatus.BAD_GATEWAY);
        }
    }

    // ── Prompts ───────────────────────────────────────────────────────

    private String buildSystemPrompt(AiTask task) {
        return switch (task) {
            case GENERATE_CARD_PROMPT ->
                "Eres un asistente experto en desarrollo de software. Tu objetivo es generar prompts detallados " +
                "en español para que un desarrollador los copie y pegue en una herramienta de IA y comience " +
                "a implementar una tarea de manera eficiente. El prompt debe ser claro, técnico y accionable.";
            case IMPROVE_TICKET_REPLY ->
                "Eres un agente de soporte técnico profesional de habla hispana. Mejoras respuestas de soporte " +
                "haciéndolas más claras, empáticas y profesionales. Solo devuelves el texto mejorado, " +
                "sin explicaciones, sin comillas, sin prefijos.";
            case QUICK_REPLY ->
                "Eres un agente de soporte técnico profesional de habla hispana. Generas respuestas cortas, " +
                "formales y amigables para tickets de soporte. Solo devuelves el texto de la respuesta, " +
                "sin explicaciones, sin comillas, sin prefijos. Usa 'usted' como tratamiento formal.";
            case GENERATE_KB_CONTENT ->
                "Eres un experto en documentación técnica de habla hispana. Redactas artículos claros, " +
                "estructurados y útiles para bases de conocimiento de soporte técnico. " +
                "El contenido debe estar en HTML limpio usando: <h2>, <h3>, <p>, <ul>, <ol>, <li>, <strong>, <code>. " +
                "No incluyas <html>, <head>, <body>. No incluyas markdown, solo HTML.";
        };
    }

    private String buildUserPrompt(AiTask task, AiAssistRequest.AiContext ctx) {
        if (ctx == null) throw new ControlTowerException("El contexto es requerido", HttpStatus.BAD_REQUEST);

        return switch (task) {
            case GENERATE_CARD_PROMPT  -> buildCardPrompt(ctx);
            case IMPROVE_TICKET_REPLY  -> buildImprovePrompt(ctx);
            case QUICK_REPLY           -> buildQuickReplyPrompt(ctx);
            case GENERATE_KB_CONTENT   -> buildKbContentPrompt(ctx);
        };
    }

    private String buildCardPrompt(AiAssistRequest.AiContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("Genera un prompt detallado para implementar la siguiente tarea de Kanban.\n\n");
        sb.append("**INFORMACIÓN DE LA TAREA:**\n");
        sb.append("- Título: ").append(safe(ctx.cardTitle())).append("\n");
        if (hasText(ctx.boardName()))
            sb.append("- Tablero: ").append(ctx.boardName()).append("\n");
        if (hasText(ctx.cardPriority()))
            sb.append("- Prioridad: ").append(ctx.cardPriority()).append("\n");
        if (hasText(ctx.clientName()))
            sb.append("- Cliente: ").append(ctx.clientName()).append("\n");
        if (hasText(ctx.cardDescription()))
            sb.append("- Descripción: ").append(ctx.cardDescription()).append("\n");
        if (ctx.cardChecklist() != null && !ctx.cardChecklist().isEmpty()) {
            sb.append("- Checklist:\n");
            ctx.cardChecklist().forEach(item -> sb.append("  * ").append(item).append("\n"));
        }
        if (ctx.devNotes() != null && !ctx.devNotes().isEmpty()) {
            sb.append("\n**NOTAS INTERNAS DEL EQUIPO:**\n");
            ctx.devNotes().forEach(note -> sb.append("- ").append(note).append("\n"));
        }
        sb.append("\n**INSTRUCCIONES:**\n");
        sb.append("El prompt debe incluir:\n");
        sb.append("1. Objetivo claro y contexto del problema\n");
        sb.append("2. Requisitos técnicos y criterios de aceptación\n");
        sb.append("3. Consideraciones de implementación (arquitectura, patrones, restricciones)\n");
        sb.append("4. Preguntas clave que el desarrollador debe resolver\n");
        sb.append("5. Ejemplo de estructura de código o pseudocódigo si aplica\n");
        sb.append("\nGenera el prompt listo para copiar y pegar en una IA como Claude o ChatGPT.");
        return sb.toString();
    }

    private String buildImprovePrompt(AiAssistRequest.AiContext ctx) {
        if (!hasText(ctx.draftReply()))
            throw new ControlTowerException("Se requiere el borrador de respuesta", HttpStatus.BAD_REQUEST);
        StringBuilder sb = new StringBuilder();
        sb.append("Mejora el siguiente borrador de respuesta para un ticket de soporte.\n\n");
        if (hasText(ctx.ticketSubject()))
            sb.append("Asunto del ticket: ").append(ctx.ticketSubject()).append("\n");
        if (hasText(ctx.ticketDescription()))
            sb.append("Descripción del ticket: ").append(ctx.ticketDescription()).append("\n");
        if (hasText(ctx.ticketStatus()))
            sb.append("Estado actual: ").append(ctx.ticketStatus()).append("\n");
        if (hasText(ctx.ticketPriority()))
            sb.append("Prioridad: ").append(ctx.ticketPriority()).append("\n");
        if (hasText(ctx.clientName()))
            sb.append("Cliente: ").append(ctx.clientName()).append("\n");
        if (ctx.kbArticles() != null && !ctx.kbArticles().isEmpty()) {
            sb.append("\n**ARTÍCULOS RELEVANTES DE LA BASE DE CONOCIMIENTO:**\n");
            ctx.kbArticles().forEach(a -> sb.append("- ").append(a).append("\n"));
        }
        if (ctx.previousReplies() != null && !ctx.previousReplies().isEmpty()) {
            sb.append("\n**HISTORIAL DE RESPUESTAS PREVIAS:**\n");
            ctx.previousReplies().stream().limit(3).forEach(r -> sb.append("- ").append(r).append("\n"));
        }
        sb.append("\nBorrador a mejorar:\n").append(ctx.draftReply());
        return sb.toString();
    }

    private String buildQuickReplyPrompt(AiAssistRequest.AiContext ctx) {
        if (ctx.quickReplyType() == null)
            throw new ControlTowerException("Se requiere el tipo de respuesta rápida", HttpStatus.BAD_REQUEST);

        StringBuilder sb = new StringBuilder();
        sb.append("Genera una respuesta profesional en español para un ticket de soporte");
        if (hasText(ctx.ticketSubject()))
            sb.append(" con asunto: \"").append(ctx.ticketSubject()).append("\"");
        sb.append(".\n\n");
        if (hasText(ctx.ticketDescription()))
            sb.append("Contexto del problema: ").append(ctx.ticketDescription()).append("\n");
        if (hasText(ctx.clientName()))
            sb.append("Nombre del cliente: ").append(ctx.clientName()).append("\n");
        if (hasText(ctx.ticketSource()))
            sb.append("Origen: ").append(ctx.ticketSource()).append("\n");
        if (ctx.kbArticles() != null && !ctx.kbArticles().isEmpty()) {
            sb.append("\nInformación de la base de conocimiento que puedes referenciar:\n");
            ctx.kbArticles().stream().limit(2).forEach(a -> sb.append("- ").append(a).append("\n"));
        }
        sb.append("\n");

        String instruction = switch (ctx.quickReplyType()) {
            case STARTED_REVIEW ->
                "Informa al cliente que hemos recibido su solicitud y que el equipo ya inició el proceso de revisión. " +
                "Indica que se mantendrá informado sobre el avance.";
            case WAITING_CLIENT ->
                "Informa amablemente que estamos en espera de la información o confirmación del cliente para poder continuar. " +
                "Indica que el ticket quedará en pausa hasta recibir su respuesta.";
            case CLOSE_TICKET ->
                "Informa que el ticket ha sido resuelto satisfactoriamente y agradece al cliente por contactarnos. " +
                "Invita a abrir un nuevo ticket si necesita ayuda adicional.";
            case NEED_INFO ->
                "Solicita amablemente más información o detalles al cliente para poder atender mejor su solicitud. " +
                "Sé específico en que sin esos datos no es posible avanzar.";
            case SCHEDULE_CALL ->
                "Propone una videollamada o reunión para revisar el caso en detalle. " +
                "Indica que el equipo está disponible y pide que el cliente sugiera horario o comparta su disponibilidad.";
        };

        sb.append(instruction);
        sb.append("\n\nLa respuesta debe ser de 2-4 párrafos cortos, profesional, empática y en español. Usa 'usted' como tratamiento formal.");
        return sb.toString();
    }

    private String buildKbContentPrompt(AiAssistRequest.AiContext ctx) {
        if (!hasText(ctx.ticketSubject()) && !hasText(ctx.cardTitle()))
            throw new ControlTowerException("Se requiere el título del artículo", HttpStatus.BAD_REQUEST);

        String title = hasText(ctx.ticketSubject()) ? ctx.ticketSubject() : ctx.cardTitle();
        StringBuilder sb = new StringBuilder();
        sb.append("Genera el contenido completo para un artículo de base de conocimiento con el siguiente título:\n\n");
        sb.append("**Título:** ").append(title).append("\n");
        if (hasText(ctx.ticketDescription()))
            sb.append("**Contexto adicional:** ").append(ctx.ticketDescription()).append("\n");
        if (hasText(ctx.clientName()))
            sb.append("**Audiencia objetivo:** ").append(ctx.clientName()).append("\n");
        sb.append("\nEl artículo debe incluir:\n");
        sb.append("1. Una introducción breve que explique de qué trata el tema\n");
        sb.append("2. Pasos o secciones principales bien estructuradas\n");
        sb.append("3. Notas o advertencias importantes si aplica\n");
        sb.append("4. Una sección de preguntas frecuentes si es relevante\n\n");
        sb.append("Devuelve SOLO el HTML del contenido, sin explicaciones adicionales.");
        return sb.toString();
    }

    private String safe(String s) { return s != null ? s : ""; }
    private boolean hasText(String s) { return s != null && !s.isBlank(); }
}
