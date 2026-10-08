package com.eventsApp.integration.mail;

import com.eventsApp.configuration.AsyncConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Wysyła wiadomości z konta samej aplikacji, a nie ze skrzynki tenanta — alerty mają działać
 * również wtedy, gdy tenant nie podłączył Google.
 */
@Service
@Slf4j
public class SystemMailSender {

    /**
     * Spring tworzy JavaMailSender dopiero, gdy ustawiony jest SPRING_MAIL_HOST. Na środowisku bez
     * skonfigurowanego SMTP beana nie ma i wysyłka jest po prostu pomijana, zamiast wywracać wywołanie.
     */
    private final ObjectProvider<JavaMailSender> mailSender;
    private final String from;

    public SystemMailSender(ObjectProvider<JavaMailSender> mailSender,
                            @Value("${app.mail.alert-from:}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public boolean isConfigured() {
        return mailSender.getIfAvailable() != null && !from.isBlank();
    }

    /**
     * Wysyła na osobnym wątku, żeby żądanie, które wywołało wiadomość, nie czekało na serwer pocztowy.
     * Wyjątek nie ma dokąd wrócić (wołający już dawno odpowiedział), więc błąd jest logowany tutaj.
     */
    @Async(AsyncConfiguration.MAIL_EXECUTOR)
    public void sendAsync(String to, String subject, String body) {
        try {
            send(to, subject, body);
        } catch (Exception e) {
            log.warn("Nie udało się wysłać wiadomości systemowej do {}: {}", to, e.getMessage());
        }
    }

    public void send(String to, String subject, String body) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (sender == null || from.isBlank()) {
            log.warn("Pominięto wiadomość systemową do {} — brak konfiguracji SMTP (SPRING_MAIL_HOST / APP_MAIL_ALERT_FROM).", to);
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
