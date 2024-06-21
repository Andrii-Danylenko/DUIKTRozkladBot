package org.rozkladbot.telegramservice;

import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.handlers.ResponseHandler;
import org.rozkladbot.DBControllers.UserDB;
import org.rozkladbot.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cglib.core.Local;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.function.BiConsumer;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

@Component("RozkladBot")
public class RozkladBot extends AbilityBot {
    private final ResponseHandler responseHandler;
    @Autowired
    public RozkladBot(Environment environment) {
        super(environment.getProperty("botApiKey"), environment.getProperty("bot.name"));
        GroupDB.fetchGroups();
        UserDB.updateUsersFromFile();
        System.out.printf("""
                          Час на сервері: %s
                          Час у Києві: %s
                          Сьогодні: %s
                          """, LocalDateTime.now(), DateUtils.timeOfNow(), DateUtils.getDayOfWeek(DateUtils.getTodayDateString()));
        responseHandler = new ResponseHandler(silent);
    }
    @Bean
    public SilentSender silentSender() {
        return silent;
    }
    public Ability startBot() {
        return Ability
                .builder()
                .name("start")
                .info("RozkladBot")
                .locality(ALL)
                .privacy(PUBLIC)
                .action(ctx -> {
                    responseHandler.replyToStart(ctx.chatId());
                })
                .build();
    }

    public Reply replyToButtons() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) -> responseHandler.replyToButtons(getChatId(upd), upd);
        return Reply.of(action, upd -> {
            long chatId = getChatId(upd);
            return responseHandler.userIsActive(chatId) && (upd.hasMessage() || upd.hasCallbackQuery());
        });
    }
    @Override
    public long creatorId() {
        return 1L;
    }
}