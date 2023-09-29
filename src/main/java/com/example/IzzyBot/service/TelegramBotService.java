package com.example.IzzyBot.service;

import com.example.IzzyBot.config.BotConfig;
import com.example.IzzyBot.entity.User;
import com.example.IzzyBot.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBotService extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    final BotConfig config;


    static final String HELP_TEXT = "This bot is created in Spring to help you get in a good mood.\n\n " +
            "You can execute commands from the menu on the left upside or by typing a command. \n\n" +
            "Type /start to see welcome message \n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /setting to set your info preferences\n\n" +
            "Type /help to see this message again";


    public TelegramBotService(BotConfig config) {
        this.config = config;
        List<BotCommand> listCommands = new ArrayList<>();
        listCommands.add(new BotCommand("/start", "get a welcome message"));
        listCommands.add(new BotCommand("/mydata", "get your data stored"));
        listCommands.add(new BotCommand("/deletedata", "delete my data"));
        listCommands.add(new BotCommand("/help", "info how to us this bot"));
        listCommands.add(new BotCommand("/settings", "set your preferences"));

        try {
            this.execute(new SetMyCommands(listCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bots command list: " + e.getMessage());

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

    public void sendAvatar(String chatId) {
        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(chatId);
        sendPhoto.setPhoto(new InputFile("https://i.imgur.com/gKtwd1U.jpeg"));
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.contains("/send")) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(" ".indexOf(messageText)));
                var users = userRepository.findAll();
                for (User user : users) {
                    sendMessage(user.getChatId(), textToSend);

                }
            }

            if (update.hasMessage() && update.getMessage().hasText())
                if (messageText.equals("/sendAvatar")) {
                    sendAvatar(String.valueOf(chatId));
                }


            switch (messageText) {
                case "/start" -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/help" -> {
                    try {
                        sendMessage(chatId, HELP_TEXT);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }


                default -> {
                    try {
                        sendMessage(chatId, "Sorry,at the moment we are work to repair the functions");
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }


            }


        }
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

    private String startCommandReceived(long chatId, String name) throws TelegramApiException {

        /*String answer = EmojiParser.parseToUnicode(" Hi, " + name + " I'm glad to see you." + " :blush: ");*/
        String answer = " Hi, " + name + " I'm glad to see you." +
                " What you're confused about? " + name +
                " ,  Feel free to ask me anything..." + " Nice to meet you";

        log.info("Replied to user" + name);

        sendMessage(chatId, answer);

        return answer;
    }


    private void sendMessage(long chatId, String textToSend) throws TelegramApiException {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();

        row.add("weather");
        row.add("get random joke");

        keyboardRows.add(row);
        row = new KeyboardRow();

        row.add("register");
        row.add("check my data");
        row.add("delete my data");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);
        message.setReplyMarkup(keyboardMarkup);


        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred" + e.getMessage());
            throw new RuntimeException(e);

        }


    }
}





