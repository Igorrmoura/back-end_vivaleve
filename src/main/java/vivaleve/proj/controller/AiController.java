package vivaleve.proj.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import vivaleve.proj.dto.AiDtos.*;
import vivaleve.proj.service.ai.AiService;

import java.util.List;

@RestController
@RequestMapping("/v1/ai")
@RequiredArgsConstructor
@Validated
public class AiController {

    private final AiService aiService;

    /** POST /v1/ai/chat — envia uma mensagem para a Pluma (sem conversationId abre conversa nova). */
    @PostMapping("/chat")
    public AiResponse chat(@Valid @RequestBody ChatRequest request) {
        return aiService.chat(request);
    }

    /** POST /v1/ai/insights/mood — leitura gentil das estatísticas de humor (7 ou 30 dias). */
    @PostMapping("/insights/mood")
    public AiResponse moodInsight(@Valid @RequestBody MoodInsightRequest request) {
        return aiService.moodInsight(request);
    }

    /** POST /v1/ai/entries/{entryId}/reflection — comentário acolhedor sobre um registro. */
    @PostMapping("/entries/{entryId}/reflection")
    public AiResponse entryReflection(@PathVariable Long entryId, @Valid @RequestBody EntryReflectionRequest request) {
        return aiService.entryReflection(entryId, request);
    }

    /** GET /v1/ai/conversations?userId=1 — conversas do usuário, da mais recente para a mais antiga. */
    @GetMapping("/conversations")
    public List<ConversationSummary> listConversations(@RequestParam Long userId) {
        return aiService.listConversations(userId);
    }

    /** GET /v1/ai/conversations/{id}/messages?userId=1 — mensagens da conversa em ordem cronológica. */
    @GetMapping("/conversations/{id}/messages")
    public List<MessageResponse> listMessages(@PathVariable Long id, @RequestParam Long userId) {
        return aiService.listMessages(id, userId);
    }

    /** DELETE /v1/ai/conversations/{id}?userId=1 — apaga a conversa e as mensagens (204). */
    @DeleteMapping("/conversations/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteConversation(@PathVariable Long id, @RequestParam Long userId) {
        aiService.deleteConversation(id, userId);
    }
}
