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
        };
    }

    private String buildUserPrompt(AiTask task, AiAssistRequest.AiContext ctx) {
        if (ctx == null) throw new ControlTowerException("El contexto es requerido", HttpStatus.BAD_REQUEST);

        return switch (task) {
            case GENERATE_CARD_PROMPT -> buildCardPrompt(ctx);
            case IMPROVE_TICKET_REPLY -> buildImprovePrompt(ctx);
            case QUICK_REPLY          -> buildQuickReplyPrompt(ctx);
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
            sb.append("Descripción del ticket: ").append(ctx.ticketDescription()).append("\n\n");
        sb.append("Borrador a mejorar:\n").append(ctx.draftReply());
        return sb.toString();
    }

    private String buildQuickReplyPrompt(AiAssistRequest.AiContext ctx) {
        if (ctx.quickReplyType() == null)
            throw new ControlTowerException("Se requiere el tipo de respuesta rápida", HttpStatus.BAD_REQUEST);

        String base = "Genera una respuesta profesional en español para un ticket de soporte";
        if (hasText(ctx.ticketSubject()))
            base += " con asunto: \"" + ctx.ticketSubject() + "\"";
        base += ".\n\n";

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

        return base + instruction + "\n\nLa respuesta debe ser de 2-4 párrafos cortos, profesional y empática.";
    }

    private String safe(String s) { return s != null ? s : ""; }
    private boolean hasText(String s) { return s != null && !s.isBlank(); }
}
