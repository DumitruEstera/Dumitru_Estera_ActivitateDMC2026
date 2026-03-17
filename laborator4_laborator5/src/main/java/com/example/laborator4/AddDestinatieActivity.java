package com.example.laborator4;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;


public class AddDestinatieActivity extends AppCompatActivity {

    private EditText editTextNumeDestinatie, editTextDistantaDestinatie;
    private Spinner spinnerTipDestinatie;
    private CheckBox checkBoxDa;
    private RadioGroup radioGroupDurataZile;
    private RatingBar ratingBar;
    private RadioButton radioButton1zi, radioButton2zi, radioButton3zi, radioButton4zi, radioButton5zi;
    private ToggleButton toggleButton;
    private Button buttonTrimiteDate;
    private Switch switch1;
    private DatePicker datePickerDest;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_destinatie);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editTextNumeDestinatie = findViewById(R.id.editTextNumeDestinatie);
        editTextDistantaDestinatie = findViewById(R.id.editTextDistantaDestinatie);
        spinnerTipDestinatie = findViewById(R.id.spinnerTipDestinatie);
        checkBoxDa = findViewById(R.id.checkBoxDa);
        radioButton1zi = findViewById(R.id.radioButton1zi);
        radioButton2zi = findViewById(R.id.radioButton2zi);
        radioButton3zi = findViewById(R.id.radioButton3zi);
        radioButton4zi = findViewById(R.id.radioButton4zi);
        radioButton5zi = findViewById(R.id.radioButton5zi);
        ratingBar = findViewById(R.id.ratingBar);
        switch1 = findViewById(R.id.switch1);
        toggleButton = findViewById(R.id.toggleButton);
        buttonTrimiteDate = findViewById(R.id.buttonTrimiteDate);
        datePickerDest = findViewById(R.id.datePickerDest);

        Date dd=new Date();
        datePickerDest.setMaxDate(dd.getTime());

        ArrayAdapter<TipDestinatie> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TipDestinatie.values()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipDestinatie.setAdapter(adapter);



        buttonTrimiteDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Destinatie dest = SalveazaDatele();
                if(dest != null){
                    Intent intent = new Intent(AddDestinatieActivity.this, MainActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("destinatie", dest);
                    intent.putExtras(bundle);

                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

    }

    private Destinatie SalveazaDatele(){
        String nume = editTextNumeDestinatie.getText().toString().trim();
        String distantaStr = editTextDistantaDestinatie.getText().toString().trim();

        if (nume.isEmpty() || distantaStr.isEmpty()){
            Toast.makeText(this, "Completeaza toate campurile!", Toast.LENGTH_LONG).show();
            return null;
        }

        double distanta = Double.parseDouble(distantaStr);
        TipDestinatie tip = (TipDestinatie) spinnerTipDestinatie.getSelectedItem();
        boolean vizitat = checkBoxDa.isChecked();
        float rating = ratingBar.getRating();
        int nrZile = 0;
        if(radioButton1zi.isChecked()) nrZile = 1;
        if(radioButton2zi.isChecked()) nrZile = 2;
        if(radioButton3zi.isChecked()) nrZile = 3;
        if(radioButton4zi.isChecked()) nrZile = 4;
        if(radioButton5zi.isChecked()) nrZile = 5;

        boolean amFostSingur = toggleButton.isChecked();
        boolean amInchiriatMasina = switch1.isChecked();

        Calendar c = new GregorianCalendar(datePickerDest.getYear(), datePickerDest.getMonth(), datePickerDest.getDayOfMonth());
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYYY");
        Date date = c.getTime();

        Destinatie dest = new Destinatie(nume, distanta, vizitat, nrZile, rating, tip, amFostSingur, amInchiriatMasina, date);

        return  dest;
    }


}