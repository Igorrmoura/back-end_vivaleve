package vivaleve.proj.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
@EnableConfigurationProperties(AiProperties.class)
public class AiConfig {

    /** RestClient do Ollama Cloud, com timeout de conexão e de leitura. */
    @Bean
    public RestClient ollamaRestClient(AiProperties props) {
        return restClient(props.baseUrl(), props.timeout());
    }

    /** RestClient do Groq, com o mesmo timeout. */
    @Bean
    public RestClient groqRestClient(AiProperties props) {
        return restClient(props.groq().baseUrl(), props.timeout());
    }

    private static RestClient restClient(String baseUrl, Duration timeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(timeout)
                .build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(timeout);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
