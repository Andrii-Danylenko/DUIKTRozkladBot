package org.rozkladbot.telegramservice;

import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.handlers.ResponseHandler;
import org.rozkladbot.DBControllers.UserDB;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.telegram.abilitybots.api.bot.AbilityBot;
import org.telegram.abilitybots.api.bot.BaseAbilityBot;
import org.telegram.abilitybots.api.objects.Ability;
import org.telegram.abilitybots.api.objects.Flag;
import org.telegram.abilitybots.api.objects.Reply;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.function.BiConsumer;

import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Privacy.PUBLIC;
import static org.telegram.abilitybots.api.util.AbilityUtils.getChatId;

@Component("RozkladBot")
public class RozkladBot extends AbilityBot {
    private final ResponseHandler responseHandler;
    @Autowired
    public RozkladBot(Environment environment) {
        super(environment.getProperty("botApiKey"), "RozkladBot");
        GroupDB.fetchGroups();
        UserDB.updateUsersFromFile();
        responseHandler = new ResponseHandler(silent);
        // TODO: сделать админ-панель тоже через бота.
//        AdminCommandHandler adminCommandHandler = new AdminCommandHandler();
//        new Thread(adminCommandHandler).start();
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
                .action(ctx -> responseHandler.replyToStart(ctx.chatId()))
                .build();
    }

    public Reply replyToButtons() {
        BiConsumer<BaseAbilityBot, Update> action = (abilityBot, upd) -> responseHandler.replyToButtons(getChatId(upd), upd);
        return Reply.of(action, Flag.TEXT,upd -> responseHandler.userIsActive(getChatId(upd)));
    }
    @Override
    public long creatorId() {
        return 1L;
    }
}