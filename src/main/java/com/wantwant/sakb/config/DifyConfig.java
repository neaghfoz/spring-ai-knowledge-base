package com.wantwant.sakb.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(DifyProperties.class)
public class DifyConfig {

    @Bean
    @ConditionalOnProperty(value = "dify.enabled", havingValue = "true")
    public WebClient difyWebClient(DifyProperties props) {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(4 * 1024 * 1024))
                .build();

        return WebClient.builder()
                .baseUrl(props.getBaseUrl())
                .defaultHeaders(h -> h.setBearerAuth(props.getApiKey() == null ? "" : props.getApiKey()))
                .exchangeStrategies(strategies)
                .build();
    }
}
