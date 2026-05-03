package com.example.dumitruestera;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Switch;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class AdaugaHusa extends AppCompatActivity {

    private RadioGroup dumitru_estera_radio_grup;
    private RadioButton radioBtn_rosu, radioBtn_albastru;
    private Switch estera_sw;
    private Spinner estera_spinner;
    private Button btn_salveaza;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_adauga_husa);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dumitru_estera_radio_grup = findViewById(R.id.dumitru_estera_radioGroup);
        radioBtn_rosu = findViewById(R.id.dumitru_estera_radioBtnRosu);
        radioBtn_albastru = findViewById(R.id.dumitru_estera_radioBtnAlbastru);
        estera_sw = findViewById(R.id.dumitru_estera_switch);
        estera_spinner = findViewById(R.id.dumitru_estera_spinner);
        btn_salveaza = findViewById(R.id.dumitru_estera_button_salveaza);

        ArrayAdapter<CuloareHusa> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                CuloareHusa.values()
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        estera_spinner.setAdapter(adapter);

        btn_salveaza.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String model = "samsung", culoare = "alb";
                if(radioBtn_rosu.isChecked()) model="samsung";
                if(radioBtn_albastru.isChecked()) model="iphone";
                CuloareHusa culoare_enum = (CuloareHusa) estera_spinner.getSelectedItem();
                if(culoare_enum == CuloareHusa.ROSU)
                    culoare = "rosu";
                if(culoare_enum == CuloareHusa.ALBASTRU)
                    culoare = "albastru";
                HusaTelefon husa = new HusaTelefon("plastic", 10, culoare, model);
                Intent intent = new Intent(AdaugaHusa.this, MainActivity.class);
                Bundle bundle = new Bundle();
                bundle.putParcelable("husa", husa);
                intent.putExtras(bundle);
                setResult(RESULT_OK, intent);
                finish();

            }
        });
    }
}