package vivaleve.proj.service.ai;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/** pluma.ai.provider escolhe qual implementação de AiClient o Spring injeta. */
class AiProviderSelectionTest {

    @Nested
    @SpringBootTest
    @TestPropertySource(locations = "classpath:application-test.properties", properties = "pluma.ai.provider=groq")
    class ComGroq {
        @Autowired AiClient aiClient;

        @Test
        void usaGroqClient() {
            assertThat(aiClient).isInstanceOf(GroqClient.class);
        }
    }

    @Nested
    @SpringBootTest
    @TestPropertySource(locations = "classpath:application-test.properties", properties = "pluma.ai.provider=ollama")
    class ComOllama {
        @Autowired AiClient aiClient;

        @Test
        void usaOllamaCloudClient() {
            assertThat(aiClient).isInstanceOf(OllamaCloudClient.class);
        }
    }
}
