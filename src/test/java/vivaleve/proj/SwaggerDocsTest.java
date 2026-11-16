package vivaleve.proj;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Swagger (springdoc): a documentação abre sem token e lista as rotas da API. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(locations = "classpath:application-test.properties")
class SwaggerDocsTest {

    @Autowired MockMvc mvc;

    @Test
    void apiDocsAbreSemTokenEListaAsRotas() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/v1/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/v1/ai/chat']").exists());
    }

    @Test
    void swaggerUiAbreSemToken() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void restoDaApiContinuaProtegido() throws Exception {
        mvc.perform(get("/v1/users/me")).andExpect(status().isUnauthorized());
    }
}
