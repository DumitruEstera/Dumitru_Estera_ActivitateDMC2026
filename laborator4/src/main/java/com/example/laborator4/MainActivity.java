package com.example.laborator4;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    private TextView textViewDestinatie;
    private Button buttonAdaugaDestinatie;
    private Button buttonAdaugaDestinatieAi;

    private ActivityResultLauncher<Intent> launchDestinatieActivity;
    private ActivityResultLauncher<Intent> launchAiActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        textViewDestinatie = findViewById(R.id.textViewDestinatie);
        buttonAdaugaDestinatie = findViewById(R.id.buttonAdaugaDestinatie);
        buttonAdaugaDestinatieAi = findViewById(R.id.buttonAi);

        launchDestinatieActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle bundle = result.getData().getExtras();
                    if (bundle != null) {
                        Destinatie dest = (Destinatie) bundle.getSerializable("destinatie");
                        if (dest != null) {
                            textViewDestinatie.setText(dest.toString());
                        }
                    }
                }
            }
        });

        launchAiActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK && result.getData() != null){
                    Bundle bundle = result.getData().getExtras();
                    if (bundle != null) {
                        Destinatie dest = (Destinatie) bundle.getSerializable("destinatie");
                        if (dest != null) {
                            textViewDestinatie.setText(dest.toString());
                        }
                    }
                }
            }
        });

        buttonAdaugaDestinatie.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, AddDestinatieActivity.class);
                launchDestinatieActivity.launch(intent);
            }
        });

        buttonAdaugaDestinatieAi.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(MainActivity.this, AIAddDestinatieActivity.class);
                launchAiActivity.launch(intent);
            }
        });

    }
}