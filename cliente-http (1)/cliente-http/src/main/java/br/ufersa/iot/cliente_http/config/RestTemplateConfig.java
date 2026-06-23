package br.ufersa.iot.cliente_http.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();

        // Configuração de timeout em milissegundos
        factory.setConnectTimeout(5000); // 5 segundos para conectar
        factory.setReadTimeout(10000);   // 10 segundos para aguardar a resposta

        return new RestTemplate(factory);
    }
}
