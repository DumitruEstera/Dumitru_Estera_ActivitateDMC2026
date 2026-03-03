package com.example.laborator3;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.text.util.LocalePreferences;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SecondActivity extends AppCompatActivity {

    private ActivityResultLauncher<Intent> activityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_second);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>(){
                    @Override
                    public void onActivityResult(ActivityResult result){
                        if(result.getResultCode() == RESULT_OK && result.getData() != null){
                            Intent data = result.getData();
                            String mesajRaspuns = data.getStringExtra("mesajRaspuns");
                            int suma = data.getIntExtra("suma", 0);
                            String toastMessage = "Mesaj primit: " + mesajRaspuns + "\nSuma calculata: " + suma;

                            Toast.makeText(SecondActivity.this, toastMessage, Toast.LENGTH_LONG).show();
                        }
                    }

                }
        );

        Button btnOpenThird = findViewById(R.id.buttonOpenThird);
        btnOpenThird.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent = new Intent(SecondActivity.this, ThirdActivity.class);

                Bundle bundle = new Bundle();
                bundle.putString("mesaj", "Mesaj din SecondActivity");
                bundle.putInt("int1", 17);
                bundle.putInt("int2", 18);

                intent.putExtras(bundle);

                activityResultLauncher.launch(intent);
            }
        });

        Button btnBackToFirst = findViewById(R.id.button2);
        btnBackToFirst.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                Intent intent2 = new Intent(SecondActivity.this, MainActivity.class);
                startActivity(intent2);
            }
        });
    }
}