package com.appsinnova.admin.business.common.utils;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

/**
 * 中文拼音首字母工具（基于 pinyin4j）。
 */
public final class PinyinUtil {

    private static final char FALLBACK = '#';
    private static final HanyuPinyinOutputFormat FORMAT = new HanyuPinyinOutputFormat();

    static {
        FORMAT.setCaseType(HanyuPinyinCaseType.UPPERCASE);
        FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    private PinyinUtil() {
    }

    /**
     * 名称第一个有效字符的首字母：{@code A-Z}，字母/数字取其大写形式；无法识别则 {@code #}。
     */
    public static String firstInitial(String text) {
        if (text == null || text.isEmpty()) {
            return String.valueOf(FALLBACK);
        }
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            Character initial = initialOfCodePoint(cp);
            if (initial != null) {
                return String.valueOf(initial);
            }
        }
        return String.valueOf(FALLBACK);
    }

    /**
     * 名称每个有效字符的首字母拼接（大写）；无有效字符时返回空串。
     */
    public static String nameInitials(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); ) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            Character initial = initialOfCodePoint(cp);
            if (initial != null) {
                sb.append(initial);
            }
        }
        return sb.toString();
    }

    /**
     * 是否为可用于品牌排序的字母首字母（{@code A-Z}）。
     */
    public static boolean isLetterInitial(String initial) {
        if (initial == null || initial.length() != 1) {
            return false;
        }
        char c = initial.charAt(0);
        return c >= 'A' && c <= 'Z';
    }

    private static Character initialOfCodePoint(int cp) {
        if (isCjkUnifiedIdeograph(cp)) {
            try {
                String[] pinyin = PinyinHelper.toHanyuPinyinStringArray((char) cp, FORMAT);
                if (pinyin != null && pinyin.length > 0 && pinyin[0] != null && !pinyin[0].isEmpty()) {
                    return Character.toUpperCase(pinyin[0].charAt(0));
                }
            } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                // fall through
            }
            return null;
        }
        if (Character.isLetterOrDigit(cp)) {
            return Character.toUpperCase((char) cp);
        }
        return null;
    }

    private static boolean isCjkUnifiedIdeograph(int cp) {
        return cp >= 0x4E00 && cp <= 0x9FA5;
    }
}
