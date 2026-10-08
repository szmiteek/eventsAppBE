package com.eventsApp.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfiguration {

    public static final String MAIL_EXECUTOR = "mailExecutor";

    /**
     * Osobna, ograniczona pula na wysyłkę poczty. Zgłoszenia z publicznego formularza nie mogą zakładać
     * wątków bez końca, gdy SMTP stoi: kolejka jest skończona, a po jej zapełnieniu alert jest pomijany
     * z wpisem w logu — oferta jest wtedy i tak zapisana, a nadmiar zgłoszeń tnie już rate limiter formularza.
     */
    @Bean(name = MAIL_EXECUTOR)
    public Executor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mail-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler((task, pool) ->
                log.warn("Kolejka wysyłki poczty jest pełna — wiadomość pominięta."));
        // Wdrożenie restartuje kontener; niech alerty już w kolejce zdążą wyjść, zamiast zniknąć razem z procesem.
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(15);
        return executor;
    }
}
