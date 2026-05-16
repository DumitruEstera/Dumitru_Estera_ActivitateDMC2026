package com.example.laborator11;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    public static final String EXTRA_NAMES = "extra_names";
    public static final String EXTRA_VALUES = "extra_values";

    private EditText[] etNames;
    private EditText[] etValues;

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

        etNames = new EditText[]{
                findViewById(R.id.etName1),
                findViewById(R.id.etName2),
                findViewById(R.id.etName3),
                findViewById(R.id.etName4),
                findViewById(R.id.etName5)
        };
        etValues = new EditText[]{
                findViewById(R.id.etValue1),
                findViewById(R.id.etValue2),
                findViewById(R.id.etValue3),
                findViewById(R.id.etValue4),
                findViewById(R.id.etValue5)
        };

        Button btnShow = findViewById(R.id.btnShow);
        btnShow.setOnClickListener(v -> openChart());
    }

    private void openChart() {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Float> values = new ArrayList<>();

        for (int i = 0; i < etNames.length; i++) {
            String name = etNames[i].getText().toString().trim();
            String valStr = etValues[i].getText().toString().trim();
            if (name.isEmpty() || valStr.isEmpty()) continue;
            try {
                float val = Float.parseFloat(valStr);
                if (val <= 0) continue;
                names.add(name);
                values.add(val);
            } catch (NumberFormatException ignored) {
            }
        }

        if (names.isEmpty()) {
            Toast.makeText(this, R.string.error_invalid, Toast.LENGTH_SHORT).show();
            return;
        }

        float[] valuesArr = new float[values.size()];
        for (int i = 0; i < values.size(); i++) valuesArr[i] = values.get(i);

        Intent intent = new Intent(this, ChartActivity.class);
        Bundle bundle = new Bundle();
        bundle.putStringArrayList(EXTRA_NAMES, names);
        bundle.putFloatArray(EXTRA_VALUES, valuesArr);
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
