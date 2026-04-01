package com.example.laborator6;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ShareActionProvider;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SetariPreferinte extends AppCompatActivity {

    private EditText marimeText, culoareText;
    private Button btnSalveaza;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_setari_preferinte);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        marimeText = findViewById(R.id.editTextMarime);
        culoareText = findViewById(R.id.editTextCuloare);
        btnSalveaza = findViewById(R.id.buttonSalveaza);

        btnSalveaza.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                SalveazaPreferinteInFisier();
                finish();
            }
        });
    }

    private void SalveazaPreferinteInFisier(){
        String marime = marimeText.getText().toString().trim();
        String culoare = culoareText.getText().toString().trim();
        if(marime.isEmpty() || culoare.isEmpty()){
            return;
        }
        SharedPreferences sp = getSharedPreferences("fisier_setari_preferinte", MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("dimensiune_text", marime);
        editor.putString("culoare_text", culoare);
        editor.commit();
    }
}