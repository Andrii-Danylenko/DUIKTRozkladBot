package org.rozkladbot.factories;

import org.rozkladbot.DBControllers.GroupDB;
import org.rozkladbot.constants.EmojiList;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardRemove;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

public final class KeyBoardFactory {

    public static ReplyKeyboardRemove deleteKeyBoard() {
        return new ReplyKeyboardRemove(true);
    }

    public static InlineKeyboardMarkup getSettings(boolean isInBroadCast) {
        return new InlineKeyboardMarkup(new ArrayList<>() {{
            add(new ArrayList<>() {{
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText("Змінити групу");
                inlineKeyboardButton.setCallbackData("ЗГ");
                add(inlineKeyboardButton);
            }});
            add(new ArrayList<>() {{
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText(isInBroadCast ? "Вимкнути заплановані сповіщення" : "Увімкнути заплановані сповіщення");
                inlineKeyboardButton.setCallbackData(isInBroadCast ? "DIS" : "ENA");
                add(inlineKeyboardButton);
            }});
            add(new ArrayList<>() {{
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText("Повідомити про помилку");
                inlineKeyboardButton.setUrl("https://t.me/optionalOfNullable");
                add(inlineKeyboardButton);
            }});
            add(new ArrayList<>() {{
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText("Назад");
                inlineKeyboardButton.setCallbackData("НАЗАД");
                add(inlineKeyboardButton);
            }});
        }});
    }

    public static InlineKeyboardMarkup getCommandsList() {
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row1 = new ArrayList<>();
        InlineKeyboardButton dayButton = new InlineKeyboardButton();
        dayButton.setText("Розклад на сьогодні");
        dayButton.setCallbackData("DAY");
        row1.add(dayButton);
        buttons.add(row1);
        List<InlineKeyboardButton> row2 = new ArrayList<>();
        InlineKeyboardButton ndayButton = new InlineKeyboardButton();
        ndayButton.setText("Розклад на завтра");
        ndayButton.setCallbackData("NDAY");
        row2.add(ndayButton);
        buttons.add(row2);
        List<InlineKeyboardButton> row3 = new ArrayList<>();
        InlineKeyboardButton weekButton = new InlineKeyboardButton();
        weekButton.setText("Розклад на тиждень");
        weekButton.setCallbackData("WEEK");
        row3.add(weekButton);
        buttons.add(row3);
        List<InlineKeyboardButton> row4 = new ArrayList<>();
        InlineKeyboardButton nweekButton = new InlineKeyboardButton();
        nweekButton.setText("Розклад на наступний тиждень");
        nweekButton.setCallbackData("NWEEK");
        row4.add(nweekButton);
        buttons.add(row4);
        List<InlineKeyboardButton> row5 = new ArrayList<>();
        InlineKeyboardButton customButton = new InlineKeyboardButton();
        customButton.setText("Розклад за власний проміжок часу");
        customButton.setCallbackData("CUSTOM");
        row5.add(customButton);
        buttons.add(row5);
        List<InlineKeyboardButton> row6 = new ArrayList<>();
        InlineKeyboardButton settingsButton = new InlineKeyboardButton();
        settingsButton.setText("Налаштування");
        settingsButton.setCallbackData("НЛ");
        row6.add(settingsButton);
        buttons.add(row6);
        return new InlineKeyboardMarkup(buttons);
    }

    public static InlineKeyboardMarkup getYesOrNoInline() {
        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText(EmojiList.TRUE);
        yesButton.setCallbackData("ТАК");
        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText(EmojiList.FALSE);
        noButton.setCallbackData("НІ");
        InlineKeyboardButton backButton = new InlineKeyboardButton();
        backButton.setText("Назад");
        backButton.setCallbackData("НАЗАД");
        List<InlineKeyboardButton> row = new ArrayList<>() {{
            add(yesButton);
            add(noButton);
        }};
        List<InlineKeyboardButton> row2 = new ArrayList<>() {{
            add(backButton);
        }};
        List<List<InlineKeyboardButton>> rows = new ArrayList<>() {{
            add(row);
            add(row2);
        }};
        return new InlineKeyboardMarkup(rows);
    }
    public static InlineKeyboardMarkup getBackButton() {
        return new InlineKeyboardMarkup(new ArrayList<>() {{
            add(new ArrayList<>() {{
                InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
                inlineKeyboardButton.setText("Назад");
                inlineKeyboardButton.setCallbackData("НАЗАД");
                add(inlineKeyboardButton);
            }});
        }});
    }
    public static InlineKeyboardMarkup getGroupsKeyboardInline() {
        List<String> groups = GroupDB.getGroups().keySet().stream().sorted().toList();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> keyboardRows = new ArrayList<>();
        int i = 0;
        for (String group : groups) {
            if (i > 0 && i % 4 == 0) {
                buttons.add(keyboardRows);
                keyboardRows = new ArrayList<>();
            }
            InlineKeyboardButton inlineKeyboardButton = new InlineKeyboardButton();
            inlineKeyboardButton.setCallbackData(group);
            inlineKeyboardButton.setText(group);
            keyboardRows.add(inlineKeyboardButton);
            i++;
        }
        return new InlineKeyboardMarkup(buttons);
    }
}
