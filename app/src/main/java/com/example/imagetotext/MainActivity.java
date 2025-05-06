package com.example.imagetotext;

import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

import android.Manifest;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    private TextRecognizer recognizer;
    private ScrollView scrollView;
    private TextView textOutput;
    ActivityResultLauncher<Uri> takePicture;
    ActivityResultLauncher<String> pickImage;
    private File imageFile;
    private Uri imageURI;
    private static final int permissionCode = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        scrollView = findViewById(R.id.scrollView);
        textOutput = findViewById(R.id.textOutput);
        ImageView capturedImageView = findViewById(R.id.capturedImageView);
        Button clearResults = findViewById(R.id.clearResults);
        ImageButton imageButton = findViewById(R.id.imageButton);

        takePicture = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                result -> {
                    if (result) {
                        Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                        capturedImageView.setImageBitmap(bitmap);
                        capturedImageView.setVisibility(View.VISIBLE);
                        clearResults.setVisibility(View.VISIBLE);
                        textOutput.setVisibility(View.VISIBLE);
                        recognizeText(bitmap);
                    } else {
                        textOutput.setText("Failed to capture image");
                    }
                });

        pickImage = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        try {
                            Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(uri));
                            capturedImageView.setImageBitmap(bitmap);
                            capturedImageView.setVisibility(View.VISIBLE);
                            clearResults.setVisibility(View.VISIBLE);
                            textOutput.setVisibility(View.VISIBLE);
                            recognizeText(bitmap);
                        } catch (FileNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });

        imageButton.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, permissionCode);
            } else {
                String[] options = new String[]{"Take a Photo", "Pick from Gallery"};
                new AlertDialog.Builder(this)
                        .setTitle("Select an Option")
                        .setItems(options, (dialog, which) -> {
                            if (which == 0) {
                                try {
                                    launchCamera();
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            } else {
                                pickImageFromGallery();
                            }
                        }).show();
            }
        });

        clearResults.setOnClickListener(v -> {
            capturedImageView.setImageBitmap(null);
            capturedImageView.setVisibility(View.INVISIBLE);
            clearResults.setVisibility(View.INVISIBLE);
            textOutput.setVisibility(View.INVISIBLE);
        });
    }

    private void launchCamera() throws IOException {
        imageFile = File.createTempFile("Photo", ".jpg", getCacheDir());
        imageURI = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
        takePicture.launch(imageURI);
    }

    private void pickImageFromGallery() {
        pickImage.launch("image/*");
    }

    private void recognizeText(Bitmap bitmap) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        recognizer.process(image)
                .addOnSuccessListener(visionText -> processVisionText(visionText))
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                    textOutput.setText("Error recognizing text: " + e.getLocalizedMessage());
                });
    }

    private void processVisionText(Text visionText) {
        StringBuilder stringBuilder = new StringBuilder();
        if (visionText.getTextBlocks().isEmpty()) {
            stringBuilder.append("No Text Detected!");
        } else {
            for (Text.TextBlock block : visionText.getTextBlocks()) {
                String text = block.getText();
                stringBuilder.append(text).append("\n");
            }
        }
        textOutput.setText(stringBuilder.toString());

        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }
}


