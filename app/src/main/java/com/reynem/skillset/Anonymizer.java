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
    private static final Pattern FIO_ENG_PATTERN = Pattern.compile("\\b[A-Z][a-z]+(\\s+[A-Z][a-z]+){1,2}\\b");
    private static final Pattern BANK_ACCOUNT_PATTERN_KZ_IBAN = Pattern.compile("\\bKZ[A-Z0-9]{18}\\b", Pattern.CASE_INSENSITIVE);
    // Упрощенный вариант для номеров карт (например, 16 цифр, возможно, разделенных пробелами или дефисами)
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");

    private static final String FIO_REPLACEMENT = "[ФИО]";
    private static final String BANK_ACCOUNT_REPLACEMENT = "[НОМЕР СЧЕТА/КАРТЫ]";

    public static String anonymizeText(String inputText) {
        if (inputText == null || inputText.isEmpty()) {
            return "";
        }

        StringBuilder anonymized = new StringBuilder();
        String[] lines = inputText.split("\\r?\\n");

        for (String line : lines) {
            String processedLine = anonymizeLine(line);
            anonymized.append(processedLine).append("\n");
        }

        return anonymized.toString();
    }

    private static String anonymizeLine(String line) {
        String result = line;

        // Обработка ИИН
        result = IIN_PATTERN.matcher(result).replaceAll(IIN_REPLACEMENT);

        // Обработка русскоязычных ФИО
        result = FIO_PATTERN.matcher(result).replaceAll(FIO_REPLACEMENT);

        // Обработка англоязычных ФИО
        result = FIO_ENG_PATTERN.matcher(result).replaceAll(FIO_REPLACEMENT);

        // Обработка банковских счетов
        result = BANK_ACCOUNT_PATTERN_KZ_IBAN.matcher(result).replaceAll(BANK_ACCOUNT_REPLACEMENT);

        // Обработка номеров карт
        result = BANK_CARD_PATTERN.matcher(result).replaceAll(BANK_ACCOUNT_REPLACEMENT);

        return result;
    }
}