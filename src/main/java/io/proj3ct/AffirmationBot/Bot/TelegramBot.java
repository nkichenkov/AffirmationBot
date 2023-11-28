package io.proj3ct.AffirmationBot.Bot;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.AffirmationBot.config.BotConfig;
import io.proj3ct.AffirmationBot.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.*;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdsRepository adsRepository;

    @Autowired
    private RussianRouletteRepository RussianRouletteRepository;

    @Autowired
    private AffirmationRepository affirmationRepository;

    final BotConfig config;

    static final String HELP_TEXT = "Я создан для того, чтобы дарить добро.\n\n" +
            "Вот небольшой перечень того, что я умею:\n\n" +
            "Воспользуйтесь командой /start и познакомьтесь со мной.\n\n" +
            "Воспользуйтесь командой /affirmation и получите аффирмацию на день.\n\n" +
            "Воспользуйтесь командой /RussianRoulette и сыграйте в русскую рулетку.\n\n" +
            "Воспользуйтесь командой /rate и узнайте актуальный курс валют(USD/EUR/CNY).\n\n" +
            "Воспользуйтесь командой /help для того, чтобы ознакомиться с моими возможностями.";

    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";

    static final String ERROR_TEXT = "Возникла ошибка: ";

    static final int MAX_RussianRoulette_ID_MINUS_ONE = 7;

    static final int MAX_AFFIRMATION_ID_MINUS_ONE = 217;

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Приветственное сообщение"));
        listofCommands.add(new BotCommand("/help", "Как пользоваться ботом?"));
        listofCommands.add(new BotCommand("/RussianRoulette", "Русская рулетка"));
        listofCommands.add(new BotCommand("/affirmation", "Аффирмация на день"));
        listofCommands.add(new BotCommand("/rate", "Курс валют"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(Arrays.toString(e.getStackTrace()));
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/send") && config.getOwnerId() == chatId) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for (User user : users) {
                    prepareAndSendMessage(user.getChatId(), textToSend);
                }
            } else {

                switch (messageText) {

                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;

                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;

                    case "/register":
                        register(chatId);
                        break;

                    case "/RussianRoulette":
                        try {
                            ObjectMapper objectMapper = new ObjectMapper();
                            TypeFactory typeFactory = objectMapper.getTypeFactory();
                            List<RussianRoulette> RussianRouletteList = objectMapper.readValue(new File("bd/RussianRoulette.json"),
                                    typeFactory.constructCollectionType(List.class, RussianRoulette.class));
                            RussianRouletteRepository.saveAll(RussianRouletteList);
                        } catch (Exception e) {
                            log.error(Arrays.toString(e.getStackTrace()));
                        }

                        var r = new Random();
                        var randomId = r.nextInt(MAX_RussianRoulette_ID_MINUS_ONE) + 1;
                        var RussianRoulette = RussianRouletteRepository.findById(randomId);

                        System.out.println("Timber RussianRoulette = " + RussianRoulette);
                        RussianRoulette.ifPresent(randomRussianRoulette -> sendMessage(chatId, randomRussianRoulette.getBody()));
                        break;

                    case "/affirmation":
                        try {
                            ObjectMapper objectMapper = new ObjectMapper();
                            TypeFactory typeFactory = objectMapper.getTypeFactory();
                            List<Affirmation> affirmationList = objectMapper.readValue(new File("bd/affirmation.json"),
                                    typeFactory.constructCollectionType(List.class, Affirmation.class));
                            affirmationRepository.saveAll(affirmationList);
                        } catch (Exception e) {
                            log.error(Arrays.toString(e.getStackTrace()));
                        }

                        var rr = new Random();
                        var rrandomId = rr.nextInt(MAX_AFFIRMATION_ID_MINUS_ONE) + 1;

                        var affirmation = affirmationRepository.findById(rrandomId);
                        System.out.println("Timber affirmation = " + affirmation);
                        affirmation.ifPresent(randomAffirmation -> sendMessage(chatId, randomAffirmation.getBody()));
                        break;

                    default:
                        prepareAndSendMessage(chatId, "Попробуйте ввести другую команду.");

                }
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackData.equals(YES_BUTTON)) {
                String text = "Вы нажали кнопку ДА";
                executeEditMessageText(text, chatId, messageId);

            } else if (callbackData.equals(NO_BUTTON)) {
                String text = "Вы нажали кнопку НЕТ";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private Optional<RussianRoulette> getRandomRussianRoulette() {
        var r = new Random();
        var randomId = r.nextInt(MAX_RussianRoulette_ID_MINUS_ONE) + 1;

        return RussianRouletteRepository.findById(randomId);
    }

    private void register(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы действительно хотите зарегистрироваться?");

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var yesbutton = new InlineKeyboardButton();

        yesbutton.setText("Да");
        yesbutton.setCallbackData(YES_BUTTON);

        var nobutton = new InlineKeyboardButton();

        nobutton.setText("Нет");
        nobutton.setCallbackData(NO_BUTTON);

        rowInline.add(yesbutton);
        rowInline.add(nobutton);

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        executeMessage(message);

    }

    private void registerUser(Message msg) {

        if (userRepository.findById(msg.getChatId()).isEmpty()) {

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {

        String answer = EmojiParser.parseToUnicode("Привет, " + name + ", приятно познакомиться! " + "Переходи в раздел /help и ознакомься с моими возможностями" + " :blush:");
        log.info("Replied to user " + name);

        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("Узнать погоду");
        row.add("Русская рулетка");
        row.add("Аффирмация на день");

        keyboardRows.add(row);

        row = new KeyboardRow();

        row.add("Раздел находится в разработке");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);

        executeMessage(message);

    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int) messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    @Scheduled(cron = "${cron.scheduler}")
    //Секунда минута час дата день недели (это для автоматической отправки сообщений). Реклама запускается через прогу MySQL
    private void sendAds() {

        var ads = adsRepository.findAll();
        var users = userRepository.findAll();

        for (Ads ad : ads) {
            for (User user : users) {
                prepareAndSendMessage(user.getChatId(), ad.getAd());
            }
        }
    }
}