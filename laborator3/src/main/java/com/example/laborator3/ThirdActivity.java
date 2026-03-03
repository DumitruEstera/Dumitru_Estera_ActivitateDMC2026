package com.example.laborator3;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ThirdActivity extends AppCompatActivity {

    private int numar1, numar2;
    private String mesajPrimit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_third);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Bundle bundle = getIntent().getExtras();
        if(bundle != null){
            mesajPrimit = bundle.getString("mesaj", "");
            numar1 = bundle.getInt("int1", 0);
            numar2 = bundle.getInt("int2", 0);

            String toastMessage = "Mesaj primit: " + mesajPrimit + " nr1: " + numar1 + " nr2: " + numar2;
            Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();

            Button btnSendSum = findViewById(R.id.btnSendSum);
            btnSendSum.setOnClickListener(v -> {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("mesajRaspuns", "Raspuns din ThirdActivity");
                resultIntent.putExtra("suma", numar1+numar2);

                setResult(RESULT_OK, resultIntent);
                finish();
            });

        }
    }
}