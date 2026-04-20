package com.example.laborator8;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button buttonAdaugaDestinatie;
    private Button buttonAfiseazaToate;
    private Button buttonCautaNume;
    private Button buttonFiltreazaNrZile;
    private Button buttonStergeDistanta;
    private Button buttonCresteLitera;

    private EditText editTextCautaNume;
    private EditText editTextNrZileMin;
    private EditText editTextNrZileMax;
    private EditText editTextDistantaMax;
    private EditText editTextLitera;

    private ListView listView;

    private List<Destinatie> destinatii = new ArrayList<>();
    private DestinatieAdapter adapter;
    private DestinatieDBHelper dbHelper;

    private ActivityResultLauncher<Intent> launchDestinatieActivity;

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

        buttonAdaugaDestinatie = findViewById(R.id.buttonAdaugaDestinatie);
        buttonAfiseazaToate = findViewById(R.id.buttonAfiseazaToate);
        buttonCautaNume = findViewById(R.id.buttonCautaNume);
        buttonFiltreazaNrZile = findViewById(R.id.buttonFiltreazaNrZile);
        buttonStergeDistanta = findViewById(R.id.buttonStergeDistanta);
        buttonCresteLitera = findViewById(R.id.buttonCresteLitera);

        editTextCautaNume = findViewById(R.id.editTextCautaNume);
        editTextNrZileMin = findViewById(R.id.editTextNrZileMin);
        editTextNrZileMax = findViewById(R.id.editTextNrZileMax);
        editTextDistantaMax = findViewById(R.id.editTextDistantaMax);
        editTextLitera = findViewById(R.id.editTextLitera);

        listView = findViewById(R.id.listView);

        dbHelper = new DestinatieDBHelper(this);
        adapter = new DestinatieAdapter(this, destinatii);
        listView.setAdapter(adapter);

        incarcaToateDestinatiile();

        launchDestinatieActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle bundle = result.getData().getExtras();
                    if (bundle != null) {
                        Destinatie dest = bundle.getParcelable("destinatie");
                        if (dest != null) {
                            long id = dbHelper.inserareDestinatie(dest);
                            if(id != -1){
                                Toast.makeText(MainActivity.this, "Destinatie inserata cu id: " + id, Toast.LENGTH_SHORT).show();
                                incarcaToateDestinatiile();
                            }else{
                                Toast.makeText(MainActivity.this, "Eroare la inserare!", Toast.LENGTH_SHORT).show();
                            }
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

        buttonAfiseazaToate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                incarcaToateDestinatiile();
                Toast.makeText(MainActivity.this, "Afisate toate destinatiile (" + destinatii.size() + ")", Toast.LENGTH_SHORT).show();
            }
        });

        buttonCautaNume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nume = editTextCautaNume.getText().toString().trim();
                if(nume.isEmpty()){
                    Toast.makeText(MainActivity.this, "Introdu un nume!", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<Destinatie> rezultat = dbHelper.selectareDupaNume(nume);
                destinatii.clear();
                destinatii.addAll(rezultat);
                adapter.notifyDataSetChanged();
                Toast.makeText(MainActivity.this, "Gasite " + rezultat.size() + " destinatii", Toast.LENGTH_SHORT).show();
            }
        });

        buttonFiltreazaNrZile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String minStr = editTextNrZileMin.getText().toString().trim();
                String maxStr = editTextNrZileMax.getText().toString().trim();
                if(minStr.isEmpty() || maxStr.isEmpty()){
                    Toast.makeText(MainActivity.this, "Introdu ambele valori!", Toast.LENGTH_SHORT).show();
                    return;
                }
                try{
                    int min = Integer.parseInt(minStr);
                    int max = Integer.parseInt(maxStr);
                    List<Destinatie> rezultat = dbHelper.selectareDupaNrZile(min, max);
                    destinatii.clear();
                    destinatii.addAll(rezultat);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, "Gasite " + rezultat.size() + " destinatii", Toast.LENGTH_SHORT).show();
                }catch(NumberFormatException e){
                    Toast.makeText(MainActivity.this, "Valori invalide!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonStergeDistanta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String distStr = editTextDistantaMax.getText().toString().trim();
                if(distStr.isEmpty()){
                    Toast.makeText(MainActivity.this, "Introdu o distanta!", Toast.LENGTH_SHORT).show();
                    return;
                }
                try{
                    double dist = Double.parseDouble(distStr);
                    int nrSterse = dbHelper.stergereDupaDistanta(dist);
                    Toast.makeText(MainActivity.this, "Sterse " + nrSterse + " destinatii", Toast.LENGTH_SHORT).show();
                    incarcaToateDestinatiile();
                }catch(NumberFormatException e){
                    Toast.makeText(MainActivity.this, "Valoare invalida!", Toast.LENGTH_SHORT).show();
                }
            }
        });

        buttonCresteLitera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String liteStr = editTextLitera.getText().toString().trim();
                if(liteStr.isEmpty()){
                    Toast.makeText(MainActivity.this, "Introdu o litera!", Toast.LENGTH_SHORT).show();
                    return;
                }
                char litera = liteStr.charAt(0);
                int nrModificate = dbHelper.crestereNrZileDupaLitera(litera);
                Toast.makeText(MainActivity.this, "Modificate " + nrModificate + " destinatii", Toast.LENGTH_SHORT).show();
                incarcaToateDestinatiile();
            }
        });
    }

    private void incarcaToateDestinatiile(){
        List<Destinatie> toate = dbHelper.selectareToate();
        destinatii.clear();
        destinatii.addAll(toate);
        adapter.notifyDataSetChanged();
    }
}