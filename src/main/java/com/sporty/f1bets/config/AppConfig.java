package com.sporty.f1bets.config;

import com.sporty.f1bets.shared.odds.OddsGenerator;
import com.sporty.f1bets.shared.odds.RandomOddsGenerator;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import java.net.http.HttpClient;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class AppConfig {

    @Bean
    public OpenAPI f1BetsOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("F1 Betting Service API")
                        .description("List F1 events, place bets on a driver to win, and settle event outcomes.")
                        .version("v1"));
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public OddsGenerator oddsGenerator() {
        return new RandomOddsGenerator();
    }

    @Bean
    public RestClient openF1RestClient(OpenF1Properties properties) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.readTimeout());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }
}
