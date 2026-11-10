package vivaleve.proj.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;
import vivaleve.proj.dto.AiDtos.AiResponse;
import vivaleve.proj.dto.AiDtos.ChatRequest;
import vivaleve.proj.model.enums.AiInteractionStatus;
import vivaleve.proj.model.enums.AiUseCase;
import vivaleve.proj.service.ai.AiService;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Contrato HTTP da camada de IA (rotas, validação e formato do JSON). */
@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    AiService aiService;

    @Test
    void chatDevolveEnvelopePadrao() throws Exception {
        when(aiService.chat(any(ChatRequest.class))).thenReturn(new AiResponse(
                10L, 21L, AiUseCase.CHAT_APOIO, AiInteractionStatus.SUCCESS, "Estou aqui com você.",
                false, List.of(), "openai/gpt-oss-20b", LocalDateTime.of(2026, 11, 10, 12, 0)));

        mvc.perform(post("/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "message": "Estou triste hoje", "userName": "Ana"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value(10))
                .andExpect(jsonPath("$.messageId").value(21))
                .andExpect(jsonPath("$.useCase").value("CHAT_APOIO"))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.reply").value("Estou aqui com você."))
                .andExpect(jsonPath("$.crisisDetected").value(false))
                .andExpect(jsonPath("$.helpResources").isArray());
    }

    @Test
    void chatSemMensagemDa400() throws Exception {
        mvc.perform(post("/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"userId": 1, "message": "   "}
                                """))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(aiService);
    }

    @Test
    void chatSemUserIdDa400() throws Exception {
        mvc.perform(post("/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message": "oi"}
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void mensagemGrandeDemaisDa400() throws Exception {
        String grande = "a".repeat(2001);
        mvc.perform(post("/v1/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1, \"message\": \"" + grande + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void conversaInexistenteDa404() throws Exception {
        when(aiService.listMessages(99L, 1L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa não encontrada"));

        mvc.perform(get("/v1/ai/conversations/99/messages").param("userId", "1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void apagarConversaDa204() throws Exception {
        mvc.perform(delete("/v1/ai/conversations/5").param("userId", "1"))
                .andExpect(status().isNoContent());
        verify(aiService).deleteConversation(5L, 1L);
    }

    @Test
    void reflexaoUsaIdDoPath() throws Exception {
        when(aiService.entryReflection(eq(7L), any())).thenReturn(new AiResponse(
                3L, 4L, AiUseCase.REFLEXAO_REGISTRO, AiInteractionStatus.SUCCESS, "ok",
                false, List.of(), "m", LocalDateTime.now()));

        mvc.perform(post("/v1/ai/entries/7/reflection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\": 1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.useCase").value("REFLEXAO_REGISTRO"));
    }
}
