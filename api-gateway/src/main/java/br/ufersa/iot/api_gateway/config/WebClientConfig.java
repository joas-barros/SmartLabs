package br.ufersa.iot.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final int MAX_IN_MEMORY_SIZE = 10 * 1024 * 1024;

    private ExchangeStrategies createExchangeStrategies() {
        return ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(MAX_IN_MEMORY_SIZE))
                .build();
    }

    @Bean
    public WebClient digitalTwinsClient(
            @Value("${services.digital-twins.url}") String url) {
        return WebClient.builder()
                .baseUrl(url)
                .exchangeStrategies(createExchangeStrategies())
                .build();
    }

    @Bean
    public WebClient laboratorioClient(
            @Value("${services.lab.url}") String url) {
        return WebClient.builder()
                .baseUrl(url)
                .exchangeStrategies(createExchangeStrategies())
                .build();
    }
}
