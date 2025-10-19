package com.example.sportadministrationsystem.config;

import com.example.sportadministrationsystem.service.TelegramService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

/**
 * Реєструє наш TelegramService як LongPolling-бота.
 * Тут НІДЕ не створюється AbsSender, тож циклу немає.
 */
@Configuration
@RequiredArgsConstructor
public class TelegramBotConfig {

    private final TelegramService telegramService;

    @Bean
    public TelegramBotsApi telegramBotsApi() throws TelegramApiException {
        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(telegramService);
        return api;
    }
}
