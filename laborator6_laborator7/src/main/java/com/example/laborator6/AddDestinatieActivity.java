package com.example.laborator6;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;


public class AddDestinatieActivity extends AppCompatActivity {

    private TextView tvNumeDestinatie, tvTipDestinatie, tvDistantaDestinatie, tvVizitat, tvDurataZile, tvRating, tvToggle;
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
        tvNumeDestinatie = findViewById(R.id.textViewNumeDestinatie);
        tvTipDestinatie = findViewById(R.id.textViewTipDestinatie);
        tvDistantaDestinatie = findViewById(R.id.textViewDistantaDestinatie);
        tvVizitat = findViewById(R.id.textViewVizitat);
        tvDurataZile = findViewById(R.id.textViewDurataZile);
        tvRating     = findViewById(R.id.textViewRating);
        tvToggle     = findViewById(R.id.textViewToggle);

        aplicaPreferinte();

        Date dd=new Date();
        datePickerDest.setMaxDate(dd.getTime());

        ArrayAdapter<TipDestinatie> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_spinner_item,
                TipDestinatie.values()
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTipDestinatie.setAdapter(adapter);

        Destinatie destinatieVeche = getIntent().getParcelableExtra("destinatie");
        initializeazaActivitate(destinatieVeche);


        buttonTrimiteDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Destinatie dest = SalveazaDatele();
                //SalveazaInFisier(dest);
                salveazaInFisier(dest);
                if(dest != null){
                    Intent intent = new Intent(AddDestinatieActivity.this, MainActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putParcelable("destinatie", dest);
                    intent.putExtras(bundle);

                    setResult(RESULT_OK, intent);
                    finish();
                }
            }
        });

    }

    private void salveazaInFisier(Destinatie dest){
        try {
            File file = new File(getFilesDir(), "fisier_destinatii");
            boolean exista = file.exists() && file.length() > 0;
            FileOutputStream fos = new FileOutputStream(file, true);
            ObjectOutputStream oos;
            if(exista){
                oos = new ObjectOutputStream(fos){
                    @Override
                    protected void writeStreamHeader() throws IOException{
                        reset();
                    }
                };
            }else{
                oos = new ObjectOutputStream(fos);
            }
            oos.writeObject(dest);
            oos.close();
            fos.close();

        }catch(FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }

    private void aplicaPreferinte() {
        SharedPreferences sp = getSharedPreferences("fisier_setari_preferinte", MODE_PRIVATE);
        String marimeStr = sp.getString("dimensiune_text", "18");
        String culoareStr = sp.getString("culoare_text", "#000000");

        try {
            float dimensiune = Float.parseFloat(marimeStr);
            int culoare = Color.parseColor(culoareStr);
            tvNumeDestinatie.setTextSize(dimensiune);
            tvNumeDestinatie.setTextColor(culoare);

            editTextNumeDestinatie.setTextSize(dimensiune);
            editTextNumeDestinatie.setTextColor(culoare);

            tvTipDestinatie.setTextSize(dimensiune);
            tvTipDestinatie.setTextColor(culoare);

            tvDistantaDestinatie.setTextSize(dimensiune);
            tvDistantaDestinatie.setTextColor(culoare);

            editTextDistantaDestinatie.setTextSize(dimensiune);
            editTextDistantaDestinatie.setTextColor(culoare);

            tvVizitat.setTextSize(dimensiune);
            tvVizitat.setTextColor(culoare);

            checkBoxDa.setTextSize(dimensiune);
            checkBoxDa.setTextColor(culoare);

            tvDurataZile.setTextSize(dimensiune);
            tvDurataZile.setTextColor(culoare);

            radioButton1zi.setTextSize(dimensiune);
            radioButton1zi.setTextColor(culoare);

            radioButton2zi.setTextSize(dimensiune);
            radioButton2zi.setTextColor(culoare);

            radioButton3zi.setTextSize(dimensiune);
            radioButton3zi.setTextColor(culoare);

            radioButton4zi.setTextSize(dimensiune);
            radioButton4zi.setTextColor(culoare);

            radioButton5zi.setTextSize(dimensiune);
            radioButton5zi.setTextColor(culoare);

            tvRating.setTextSize(dimensiune);
            tvRating.setTextColor(culoare);

            switch1.setTextSize(dimensiune);
            switch1.setTextColor(culoare);

            tvToggle.setTextSize(dimensiune);
            tvToggle.setTextColor(culoare);

            toggleButton.setTextSize(dimensiune);
            toggleButton.setTextColor(culoare);

            buttonTrimiteDate.setTextSize(dimensiune);
            buttonTrimiteDate.setTextColor(culoare);

        } catch (Exception e) {
            Toast.makeText(this, "Eroare la aplicarea preferintelor", Toast.LENGTH_SHORT).show();
        }
    }

    /*
    private void SalveazaInFisier(Destinatie dest) {
        try {
            FileOutputStream fos;
            fos = openFileOutput("destinatii", MODE_APPEND);
            Parcel p = Parcel.obtain();
            dest.writeToParcel(p, 0);
            byte []vector = p.marshall();
            fos.write(vector);
            p.recycle();
            fos.close();
        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }

    }
    */

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

    private void initializeazaActivitate(Destinatie destinatie){
        if(destinatie != null){
            editTextNumeDestinatie.setText(destinatie.getNume());
            editTextDistantaDestinatie.setText(String.valueOf(destinatie.getDistanta()));
            checkBoxDa.setChecked(destinatie.getVizitat());
            ratingBar.setRating(destinatie.getRating());
            toggleButton.setChecked(destinatie.getAmFostSingur());
            switch1.setChecked(destinatie.getAmInchiriatMasina());
            spinnerTipDestinatie.setSelection(destinatie.getTip().ordinal());

            switch(destinatie.getNrZile()){
                case 1:
                    radioButton1zi.setChecked(true);
                    break;
                case 2:
                    radioButton2zi.setChecked(true);
                    break;
                case 3:
                    radioButton3zi.setChecked(true);
                    break;
                case 4:
                    radioButton4zi.setChecked(true);
                    break;
                case 5:
                    radioButton5zi.setChecked(true);
                    break;
            }

            Calendar cal = Calendar.getInstance();
            cal.setTime(destinatie.getDataDestinatiei());
            datePickerDest.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));


        }
    }


}