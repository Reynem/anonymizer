package com.reynem.skillset;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas; // Импортируем Canvas
import android.graphics.Color;   // Импортируем Color
import android.graphics.ImageDecoder;
import android.graphics.Paint;  // Импортируем Paint
import android.graphics.Rect;   // Импортируем Rect
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

// import com.google.android.gms.tasks.Task; // Уже импортирован неявно через TextRecognizer
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.reynem.skillset.databinding.ActivityMainBinding;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    TextRecognizer recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    private static final String TAG = "MainActivity";


    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    if (data != null) {
                        Uri uri = data.getData();
                        if (uri != null) {
                            processFile(uri);
                            final int takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION;
                            getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        PDFBoxResourceLoader.init(getApplicationContext());

        binding.AnonFile.setOnClickListener(v -> {
            openFilePicker();
            binding.resultImageView.setVisibility(View.VISIBLE);
            binding.resultText.setText("");
        });

        binding.AnonText.setOnClickListener(v -> {
            String _text = Objects.requireNonNull(binding.inputText.getText()).toString();
            if (_text.isEmpty()) {
                Toast.makeText(this, "Введите текст для анонимизации", Toast.LENGTH_SHORT).show();
                return;
            }
            String _new_text = Anonymizer.anonymizeText(_text);
            binding.resultText.setText(_new_text);
            binding.resultImageView.setVisibility(View.INVISIBLE);
            binding.inputText.setText("");
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf",
                "image/jpeg",
                "image/png"
        });
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(intent);
    }

    private String getMimeType(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        return contentResolver.getType(uri);
    }

    @SuppressLint("SetTextI18n")
    private void processFile(Uri fileUri) {
        String mimeType = getMimeType(fileUri);
        Log.d(TAG, "Processing file: " + fileUri.toString() + ", MIME type: " + mimeType);

        if (mimeType == null) {
            Toast.makeText(this, "Не удалось определить тип файла.", Toast.LENGTH_SHORT).show();
            binding.resultText.setText("Не удалось определить тип файла.");
            return;
        }

        if (mimeType.startsWith("image/")) {
            final Bitmap originalBitmap;
            try {
                originalBitmap = getBitmapFromUri(fileUri);
            } catch (IOException e) {
                Log.e(TAG, "Ошибка загрузки изображения: " + e.getMessage(), e);
                Toast.makeText(this, "Ошибка загрузки изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                binding.resultText.setText("Ошибка загрузки изображения: " + e.getMessage());
                return;
            }

            InputImage image = InputImage.fromBitmap(originalBitmap, 0);

            Log.d(TAG, "Starting text recognition for image.");
            recognizer.process(image)
                    .addOnSuccessListener(visionText -> {
                        Log.d(TAG, "Text recognition successful.");
                        Bitmap mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true);
                        Canvas canvas = new Canvas(mutableBitmap);
                        Paint paint = new Paint();
                        paint.setColor(Color.BLACK); // Черный цвет для прямоугольников
                        paint.setStyle(Paint.Style.FILL); // Заливка

                        Log.d(TAG, "Распознано слов: " + visionText.getTextBlocks().size());
                        Log.d(TAG, "Полный текст: " + visionText.getText());

                        if (visionText.getTextBlocks().isEmpty()) {
                            Log.d(TAG, "No text blocks found.");
                            Toast.makeText(MainActivity.this, "Текст на изображении не найден.", Toast.LENGTH_SHORT).show();
                            binding.resultImageView.setImageBitmap(originalBitmap); // Показываем оригинал, если текста нет
                            return;
                        }


                        for (Text.TextBlock block : visionText.getTextBlocks()) {
                            for (Text.Line line : block.getLines()) {
                                List<Text.Element> elements = line.getElements();
                                String lineText = null;
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                    lineText = TextUtils.join(" ", elements.stream().map(Text.Element::getText).toList());
                                }

                                if (Anonymizer.containsPersonalData(lineText)) {
                                    Rect lineFrame = line.getBoundingBox();
                                    if (lineFrame != null) {
                                        canvas.drawRect(lineFrame, paint);
                                        Log.d(TAG, "Закрашена строка: " + lineText + " в " + lineFrame.flattenToString());
                                    }
                                }
                            }
                        }

                        // Отображаем измененное изображение
                        binding.resultImageView.setImageBitmap(mutableBitmap);
                        Log.d(TAG, "Displayed modified bitmap.");

                        saveImageToGallery(mutableBitmap);
                    })
                    .addOnFailureListener(
                            e -> {
                                Log.e(TAG, "Text recognition failed.", e);
                                binding.resultText.setText("Ошибка распознавания текста: " + e.getMessage());
                                Toast.makeText(MainActivity.this, "Ошибка распознавания текста: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                binding.resultImageView.setImageBitmap(originalBitmap);
                            });

        } else if (mimeType.equals("application/pdf")) {
            // Implement PDF processing using the new PDFAnonymizer class
            new Thread(() -> {
                Uri resultUri = PDFAnonymizer.anonymizePDF(this, fileUri);

                runOnUiThread(() -> {
                    if (resultUri != null) {
                        binding.resultText.setText("PDF успешно анонимизирован и сохранен: " + resultUri.getPath());
                        Toast.makeText(this, "PDF успешно анонимизирован", Toast.LENGTH_SHORT).show();

                        // Open the anonymized PDF with a PDF viewer app
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setDataAndType(resultUri, "application/pdf");
                        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        try {
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "Не удалось открыть PDF. Файл сохранен по пути: " +
                                    resultUri.getPath(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        binding.resultText.setText("Ошибка при обработке PDF файла.");
                        Toast.makeText(this, "Ошибка при обработке PDF файла", Toast.LENGTH_SHORT).show();
                    }
                });
            }).start();
        } else {
            Toast.makeText(this, "Файлы этого типа пока не поддерживаются: " + mimeType, Toast.LENGTH_SHORT).show();
            binding.resultText.setText("Файлы этого типа пока не поддерживаются: " + mimeType);
        }
    }

    private Bitmap getBitmapFromUri(Uri uri) throws IOException {
        ImageDecoder.Source source = ImageDecoder.createSource(this.getContentResolver(), uri);
        return ImageDecoder.decodeBitmap(source, (decoder, info, s) -> {
            decoder.setTargetSampleSize(1);
            decoder.setMutableRequired(true);
        });
    }

    private void saveImageToGallery(Bitmap bitmap) {
        if (bitmap == null) {
            Log.e(TAG, "Bitmap пустой, нечего сохранять.");
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, "_anonymized_image.png");
        values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
        values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES);

        ContentResolver resolver = getContentResolver();
        Uri uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        try {
            assert uri != null;
            try (OutputStream os = resolver.openOutputStream(uri)) {
                if (os == null) {
                    throw new IOException("Не удалось открыть OutputStream для URI: " + uri);
                }
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                os.flush();
                Log.d(TAG, "Изображение сохранено в галерею: " + uri);
                Toast.makeText(this, "Изображение сохранено в галерею", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка сохранения изображения: " + e.getMessage(), e);
            Toast.makeText(this, "Ошибка сохранения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}