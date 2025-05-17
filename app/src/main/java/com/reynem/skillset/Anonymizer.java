package com.reynem.skillset;

import android.os.Build;

import java.util.regex.Pattern;

public class Anonymizer {
    // Основные паттерны
    private static final Pattern IIN_PATTERN = Pattern.compile("\\b\\d{12}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+7|8)[\\s()-]*(\\d[\\s()-]*){10}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("\\b[NР][\\s-]*\\d{4,9}\\b", Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern DRIVER_LICENSE_PATTERN = Pattern.compile("\\b\\d{10,12}\\b");
    private static final Pattern DATE_PATTERN = Pattern.compile("\\b\\d{2}[./]\\d{2}[./]\\d{4}\\b");

    // Улучшенный паттерн для ФИО (с учетом казахских имен и двойных фамилий)
    private static final Pattern FIO_PATTERN = Pattern.compile(
            "\\b([А-ЯЁӘҒҚҢӨҰҮҺ][а-яёәғқңөұүһі-]+[ -]?){2,3}\\b"
    );

    // Замена для разных типов данных
    private static final String IIN_REPLACEMENT = "[ИИН]";
    private static final String PHONE_REPLACEMENT = "[ТЕЛЕФОН]";
    private static final String EMAIL_REPLACEMENT = "[EMAIL]";
    private static final String PASSPORT_REPLACEMENT = "[ПАСПОРТ]";
    private static final String DRIVER_LICENSE_REPLACEMENT = "[ВОД.УДОСТ]";
    private static final String DATE_REPLACEMENT = "[ДАТА]";
    private static final String FIO_REPLACEMENT = "[ФИО]";

    public static String anonymizeText(String inputText) {
        if (inputText == null || inputText.isEmpty()) return "";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return inputText.lines()
                    .map(Anonymizer::processLine)
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        }
        return inputText;
    }

    private static String processLine(String line) {
        String result = line;

        // Порядок важен! Сначала обрабатываем сложные паттерны
        result = FIO_PATTERN.matcher(result).replaceAll(FIO_REPLACEMENT);
        result = PHONE_PATTERN.matcher(result).replaceAll(PHONE_REPLACEMENT);
        result = EMAIL_PATTERN.matcher(result).replaceAll(EMAIL_REPLACEMENT);
        result = PASSPORT_PATTERN.matcher(result).replaceAll(PASSPORT_REPLACEMENT);
        result = DRIVER_LICENSE_PATTERN.matcher(result).replaceAll(DRIVER_LICENSE_REPLACEMENT);
        result = IIN_PATTERN.matcher(result).replaceAll(IIN_REPLACEMENT);
        result = DATE_PATTERN.matcher(result).replaceAll(DATE_REPLACEMENT);

        return result;
    }

    public static boolean containsPersonalData(String text) {
        return FIO_PATTERN.matcher(text).find() ||
                PHONE_PATTERN.matcher(text).find() ||
                EMAIL_PATTERN.matcher(text).find() ||
                PASSPORT_PATTERN.matcher(text).find() ||
                DRIVER_LICENSE_PATTERN.matcher(text).find() ||
                IIN_PATTERN.matcher(text).find();
    }
}