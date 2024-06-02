package org.rozkladbot.factories;

import org.rozkladbot.DBControllers.GroupDB;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public final class KeyBoardFactory {
    public static ReplyKeyboardMarkup getYesOrNo() {
        List<KeyboardRow> keyboardRows = new ArrayList<>() {{
            add(new KeyboardRow(){{
                add("Так");
            }});
            add(new KeyboardRow(){{
                add("Ні");
            }});
        }};
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        return replyKeyboardMarkup;
    }
    public static ReplyKeyboardRemove deleteKeyBoard() {
        return new ReplyKeyboardRemove(true);
    }
    public static ReplyKeyboardMarkup changeGroup() {
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(new ArrayList<>(){{
            add(new KeyboardRow(){{
                add(new KeyboardButton("Змінити групу"));
            }});
        }});
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        return replyKeyboardMarkup;
    }

    public static ReplyKeyboardMarkup getGroupsKeyboard() {
        List<String> groups = GroupDB.getGroups().keySet().stream().sorted().toList();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        for (String group : groups) {
            keyboardRows.add(new KeyboardRow() {{
                add(new KeyboardButton(group));
            }});
        }
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        return replyKeyboardMarkup;
    }

    public static ReplyKeyboardMarkup getCommandsList() {
        List<KeyboardRow> keyboardRows = new ArrayList<>(){{
            add(new KeyboardRow() {{
                add(new KeyboardButton("/menu"));
            }});
            add(new KeyboardRow() {{
                add(new KeyboardButton("/day"));
            }});
            add(new KeyboardRow() {{
                add(new KeyboardButton("/nextDay"));
            }});
            add(new KeyboardRow() {{
                add(new KeyboardButton("/week"));
            }});
            add(new KeyboardRow() {{
                add(new KeyboardButton("/nextWeek"));
            }});
            add(new KeyboardRow() {{
                add(new KeyboardButton("/custom"));
            }});
            add(new KeyboardRow() {{
                add(new KeyboardButton("/settings"));
            }});
        }};
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup(keyboardRows);
        replyKeyboardMarkup.setOneTimeKeyboard(true);
        replyKeyboardMarkup.setSelective(true);
        replyKeyboardMarkup.setResizeKeyboard(true);
        return replyKeyboardMarkup;
    }
}
