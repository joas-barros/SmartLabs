package br.ufersa.iot.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient digitalTwinsClient(
            @Value("${services.digital-twins.url}") String url) {
        return WebClient.builder().baseUrl(url).build();
    }

    @Bean
    public WebClient laboratorioClient(
            @Value("${services.lab.url}") String url) {
        return WebClient.builder().baseUrl(url).build();
    }
}
