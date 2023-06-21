package ru.practicum.ewm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import ru.practicum.client.StatsClient;

@SpringBootApplication
public class ExploreWithMeService {
    public static void main(String[] args) {
        SpringApplication.run(ExploreWithMeService.class, args);
    }

    @Bean
    public StatsClient getStatsClient(@Value("${stats-server.url}") String url) {
        return new StatsClient(url);
    }
}
