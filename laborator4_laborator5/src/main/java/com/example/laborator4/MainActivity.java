package com.example.laborator4;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
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
    private Button buttonAdaugaDestinatieAi;

    private ActivityResultLauncher<Intent> launchDestinatieActivity;
    private ActivityResultLauncher<Intent> launchAiActivity;
    private ListView listView;

    private List<Destinatie> destinatii = new ArrayList<>();

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
        buttonAdaugaDestinatieAi = findViewById(R.id.buttonAi);
        listView = findViewById(R.id.listView);

        ArrayAdapter<Destinatie> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                destinatii);

        listView.setAdapter(adapter);

        launchDestinatieActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle bundle = result.getData().getExtras();
                    if (bundle != null) {
                        Destinatie dest = (Destinatie) bundle.getSerializable("destinatie");
                        if (dest != null) {
                            // textViewDestinatie.setText(dest.toString());
                            destinatii.add(dest);
                            adapter.notifyDataSetChanged();
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
                            // textViewDestinatie.setText(dest.toString());
                            destinatii.add(dest);
                            adapter.notifyDataSetChanged();
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

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                Toast.makeText(MainActivity.this, destinatii.get(position).toString(), Toast.LENGTH_LONG).show();
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                destinatii.remove(position);
                adapter.notifyDataSetChanged();
                return true;
            }
        });

    }
}