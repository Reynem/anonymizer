package com.reynem.skillset;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Anonymizer {
    //TODO: ИЗМЕНИТЬ НА НОРМАЛЬНУЮ ЛОГИКУ
    private static final Pattern IIN_PATTERN = Pattern.compile("\\b\\d{12}\\b");
    private static final String IIN_REPLACEMENT = "[ИИН]";

    // TODO: ИЗМЕНИТЬ НА НОРМАЛЬНУЮ ЛОГИКУ
    // В реальности для ФИО нужен более сложный подход (NER или более точные регулярные выражения).
    // Этот паттерн может дать много ложных срабатываний.
    private static final Pattern FIO_PATTERN = Pattern.compile("\\b[А-ЯЁ][а-яё]+(\\s+[А-ЯЁ][а-яё]+){1,2}\\b");
    private static final String FIO_REPLACEMENT = "[ФИО]";

    // TODO: Меньший приоритет, но ИЗМЕНИТЬ НА НОРМАЛЬНУЮ ЛОГИКУ
    private static final Pattern BANK_ACCOUNT_PATTERN_KZ_IBAN = Pattern.compile("\\bKZ[A-Z0-9]{18}\\b", Pattern.CASE_INSENSITIVE);
    // Упрощенный вариант для номеров карт (например, 16 цифр, возможно, разделенных пробелами или дефисами)
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");
    private static final String BANK_ACCOUNT_REPLACEMENT = "[НОМЕР СЧЕТА/КАРТЫ]";


    public static String anonymizeText(String inputText) {
        if (inputText == null || inputText.isEmpty()) {
            return "";
        }

        String anonymizedText = inputText;

        // Анонимизация ИИН
        Matcher iinMatcher = IIN_PATTERN.matcher(anonymizedText);
        anonymizedText = iinMatcher.replaceAll(IIN_REPLACEMENT);

        // Анонимизация ФИО (упрощенная)
        Matcher fioMatcher = FIO_PATTERN.matcher(anonymizedText);
        anonymizedText = fioMatcher.replaceAll(FIO_REPLACEMENT);

        // Анонимизация банковских счетов/карт
        Matcher bankAccountMatcherKz = BANK_ACCOUNT_PATTERN_KZ_IBAN.matcher(anonymizedText);
        anonymizedText = bankAccountMatcherKz.replaceAll(BANK_ACCOUNT_REPLACEMENT);

        Matcher bankCardMatcher = BANK_CARD_PATTERN.matcher(anonymizedText);
        anonymizedText = bankCardMatcher.replaceAll(BANK_ACCOUNT_REPLACEMENT);

        return anonymizedText;
    }
}