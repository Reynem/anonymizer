package com.reynem.skillset;

import android.net.Uri;
import android.os.Environment;
import android.util.Log;

import com.tom_roush.pdfbox.pdmodel.PDDocument;
import android.graphics.RectF;


import com.tom_roush.pdfbox.pdmodel.PDPage;
import com.tom_roush.pdfbox.pdmodel.PDPageContentStream;
import com.tom_roush.pdfbox.pdmodel.common.PDRectangle;
import com.tom_roush.pdfbox.pdmodel.font.PDFont;
import com.tom_roush.pdfbox.pdmodel.font.PDType1Font;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.text.PDFTextStripperByArea;
import com.tom_roush.pdfbox.text.TextPosition;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PDFAnonymizer {
    private static final String TAG = "PDFAnonymizer";

    // Replacement constants (same as in your code)
    private static final String IIN_REPLACEMENT = "[ИИН]";
    private static final String PHONE_REPLACEMENT = "[ТЕЛЕФОН]";
    private static final String EMAIL_REPLACEMENT = "[EMAIL]";
    private static final String PASSPORT_REPLACEMENT = "[ПАСПОРТ]";
    private static final String DRIVER_LICENSE_REPLACEMENT = "[ВОД.УДОСТ]";
    private static final String DATE_REPLACEMENT = "[ДАТА]";
    private static final String FIO_REPLACEMENT = "[ФИО]";

    // Pattern for identifying personal data
    private static final Pattern IIN_PATTERN = Pattern.compile("\\d{12}");
    private static final Pattern PHONE_PATTERN = Pattern.compile("(\\+7|8)[\\s\\-]?\\(?\\d{3}\\)?[\\s\\-]?\\d{3}[\\s\\-]?\\d{2}[\\s\\-]?\\d{2}");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern PASSPORT_PATTERN = Pattern.compile("[A-Z0-9]{2}\\s?\\d{7}|\\d{2}\\s?\\d{2}\\s?\\d{6}");
    private static final Pattern DATE_PATTERN = Pattern.compile("(0[1-9]|[12][0-9]|3[01])[/.\\-](0[1-9]|1[012])[/.\\-](19|20)\\d\\d");
    private static final Pattern FIO_PATTERN = Pattern.compile("[А-ЯЁ][а-яё]+\\s+[А-ЯЁ][а-яё]+(?:\\s+[А-ЯЁ][а-яё]+)?");

    /**
     * Main method to anonymize a PDF file
     * @param context The MainActivity context
     * @param inputUri URI of the input PDF file
     * @return URI of the anonymized PDF file or null if an error occurred
     */
    public static Uri anonymizePDF(MainActivity context, Uri inputUri) {
        try {
            // Create a temporary file to store the anonymized PDF
            File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File outputFile = new File(downloads, "anonymized_" + System.currentTimeMillis() + ".pdf");

            // Open input stream from URI
            InputStream inputStream = context.getContentResolver().openInputStream(inputUri);
            if (inputStream == null) {
                Log.e(TAG, "Failed to open input stream for URI: " + inputUri);
                return null;
            }

            // Load the PDF document
            PDDocument document = PDDocument.load(inputStream);

            // Process each page
            int pageCount = document.getNumberOfPages();
            Log.d(TAG, "Processing PDF with " + pageCount + " pages");

            // Use custom text stripper to find and replace personal data
            AnonymizerTextStripper textStripper = new AnonymizerTextStripper();

            for (int i = 0; i < pageCount; i++) {
                PDPage page = document.getPage(i);
                textStripper.processPage(document, page, i);
            }

            // Save the modified document
            document.save(outputFile);
            document.close();
            inputStream.close();

            // Return the URI for the output file
            return Uri.fromFile(outputFile);

        } catch (IOException e) {
            Log.e(TAG, "Error anonymizing PDF: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Custom PDFTextStripper that identifies and replaces personal data
     */
    private static class AnonymizerTextStripper extends PDFTextStripper {
        private final List<TextReplacement> replacements = new ArrayList<>();

        public AnonymizerTextStripper() throws IOException {
            super();
        }

        public void processPage(PDDocument document, PDPage page, int pageIndex) throws IOException {
            setStartPage(pageIndex + 1);
            setEndPage(pageIndex + 1);

            // Extract text and find personal data
            String pageText = getText(document);
            findPersonalData(pageText, pageIndex);

            // Apply replacements for this page
            if (!replacements.isEmpty()) {
                applyReplacements(document, page, pageIndex);
            }
        }

        private void findPersonalData(String text, int pageIndex) {
            // Find IIN
            Matcher iinMatcher = IIN_PATTERN.matcher(text);
            while (iinMatcher.find()) {
                replacements.add(new TextReplacement(pageIndex, iinMatcher.group(), IIN_REPLACEMENT));
            }

            // Find phone numbers
            Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
            while (phoneMatcher.find()) {
                replacements.add(new TextReplacement(pageIndex, phoneMatcher.group(), PHONE_REPLACEMENT));
            }

            // Find emails
            Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
            while (emailMatcher.find()) {
                replacements.add(new TextReplacement(pageIndex, emailMatcher.group(), EMAIL_REPLACEMENT));
            }

            // Find passport numbers
            Matcher passportMatcher = PASSPORT_PATTERN.matcher(text);
            while (passportMatcher.find()) {
                replacements.add(new TextReplacement(pageIndex, passportMatcher.group(), PASSPORT_REPLACEMENT));
            }

            // Find dates
            Matcher dateMatcher = DATE_PATTERN.matcher(text);
            while (dateMatcher.find()) {
                replacements.add(new TextReplacement(pageIndex, dateMatcher.group(), DATE_REPLACEMENT));
            }

            // Find names
            Matcher fioMatcher = FIO_PATTERN.matcher(text);
            while (fioMatcher.find()) {
                replacements.add(new TextReplacement(pageIndex, fioMatcher.group(), FIO_REPLACEMENT));
            }
        }

        private void applyReplacements(PDDocument document, PDPage page, int pageIndex) throws IOException {
            PDRectangle pageSize = page.getMediaBox();

            // Используемый шрифт
            PDFont font = PDType1Font.HELVETICA;
            float fontSize = 8;

            // Сначала найдём все позиции ЗАРАНЕЕ (до открытия contentStream)
            List<TextPositionSequence> allSequences = new ArrayList<>();
            List<String> allReplacementTexts = new ArrayList<>();

            for (TextReplacement replacement : replacements) {
                if (replacement.pageIndex == pageIndex) {
                    List<TextPositionSequence> sequences = findTextPositionSequence(document, page, replacement.originalText);
                    for (TextPositionSequence seq : sequences) {
                        allSequences.add(seq);
                        allReplacementTexts.add(replacement.replacementText);
                    }
                }
            }

            // Теперь открываем поток для записи
            PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true);

            for (int i = 0; i < allSequences.size(); i++) {
                TextPositionSequence sequence = allSequences.get(i);
                String replacementText = allReplacementTexts.get(i);

                // Заливка белым
                contentStream.setNonStrokingColor(255f, 255f, 255f);
                float x = sequence.getX();
                float y = sequence.getY();
                float width = sequence.getWidth();
                float height = sequence.getHeight();

                contentStream.addRect(x, y, width, height);
                contentStream.fill();

                // Новый текст
                contentStream.beginText();
                contentStream.setFont(font, fontSize);
                contentStream.setNonStrokingColor(0f, 0f, 0f);
                contentStream.newLineAtOffset(x, y + height - fontSize);
                contentStream.showText(replacementText);
                contentStream.endText();
            }

            contentStream.close();
        }


        private List<TextPositionSequence> findTextPositionSequence(PDDocument document, PDPage page, String searchText) throws IOException {
            final List<TextPositionSequence> result = new ArrayList<>();
            PDFTextStripperByArea stripperByArea = new PDFTextStripperByArea() {
                @Override
                protected void processTextPosition(TextPosition text) {
                    super.processTextPosition(text);
                    String textStr = text.getUnicode();

                    if (textStr != null && textStr.contains(searchText)) {
                        float x = text.getX();
                        float y = text.getY();
                        float width = text.getWidth();
                        float height = text.getHeight();

                        result.add(new TextPositionSequence(x, y, width, height));
                    }
                }
            };

            PDRectangle cropBox = page.getCropBox();
            RectF region = new RectF(
                    cropBox.getLowerLeftX(),
                    cropBox.getLowerLeftY(),
                    cropBox.getLowerLeftX() + cropBox.getWidth(),
                    cropBox.getLowerLeftY() + cropBox.getHeight()
            );

            stripperByArea.addRegion("region", region);
            stripperByArea.extractRegions(page);

            return result;
        }
    }

    /**
     * Helper class to store text replacements
     */
    private static class TextReplacement {
        int pageIndex;
        String originalText;
        String replacementText;

        public TextReplacement(int pageIndex, String originalText, String replacementText) {
            this.pageIndex = pageIndex;
            this.originalText = originalText;
            this.replacementText = replacementText;
        }
    }

    /**
     * Helper class to store text position information
     */
    private static class TextPositionSequence {
        private final float x;
        private final float y;
        private final float width;
        private final float height;

        public TextPositionSequence(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public float getX() { return x; }
        public float getY() { return y; }
        public float getWidth() { return width; }
        public float getHeight() { return height; }
    }
}