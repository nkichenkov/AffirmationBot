package io.proj3ct.AffirmationBot.config;

import io.proj3ct.AffirmationBot.Bot.TelegramBot;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@AllArgsConstructor
public class BotInitializer {

    @Autowired
    TelegramBot bot;
}