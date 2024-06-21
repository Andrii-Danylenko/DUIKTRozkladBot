package org.rozkladbot.constants;

public final class EmojiList {
    public static final String TRUE = "✅";
    public static final String FALSE = "❌";
    public static final String HAPPY = "\uD83E\uDD73";
    public static final String BEER = "\uD83C\uDF7B";
    public static final String SUBJECT = "\uD83D\uDCDA";
    public static final String CLOCK = "\uD83D\uDD51";
    public static final String CALENDAR = "\uD83D\uDCC5";
    public static final String LECTOR = "\uD83E\uDDD1\u200D\uD83C\uDFEB";
    public static final String ROOM = "\uD83D\uDEAA";
    public static final String NERD_FACE = "\uD83E\uDD13";
    // TODO: сделать динамические линки на конференции.
    public static final String LINK_TO_ZOOM = "\uD83D\uDD17";
    private static final String PAIR_1 = "1️⃣";
    private static final String PAIR_2 = "2️⃣";
    private static final String PAIR_3 = "3️⃣";
    private static final String PAIR_4 = "4️⃣";
    private static final String PAIR_5 = "5️⃣";
    private static final String PAIR_6 = "6️⃣";

    public static String getPairEmoji(int pairNumber) {
        String pair = PAIR_1;
        switch (pairNumber) {
            case 2 -> pair = PAIR_2;
            case 3 -> pair = PAIR_3;
            case 4 -> pair = PAIR_4;
            case 5 -> pair = PAIR_5;
            case 6 -> pair = PAIR_6;
        }
        return pair;
    }
}
