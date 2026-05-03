package com.example.laborator6;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
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

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Button buttonAdaugaDestinatie;
    private Button buttonAdaugaDestinatieAi;

    private ActivityResultLauncher<Intent> launchDestinatieActivity;
    private ActivityResultLauncher<Intent> launchAiActivity;
    private ListView listView;

    private int amModificatDestinatia = -1;

    private List<Destinatie> destinatii = new ArrayList<>();
    private Button ButtonSetariPreferinte;

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
        ButtonSetariPreferinte = findViewById(R.id.btmSetariPreferinte);

        DestinatieAdapter adapter = new DestinatieAdapter(this, destinatii);
        listView= findViewById(R.id.listView);
        listView.setAdapter(adapter);

        //citesteDinFisier();
        citesteDinFisier();

        launchDestinatieActivity = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle bundle = result.getData().getExtras();
                    if (bundle != null) {
                        Destinatie dest = bundle.getParcelable("destinatie");
                        if (dest != null) {
                            if(amModificatDestinatia >= 0){
                                destinatii.set(amModificatDestinatia, dest);
                                amModificatDestinatia = -1;
                            }
                            else{
                                destinatii.add(dest);
                            }
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
                        Destinatie dest = bundle.getParcelable("destinatie");
                        if (dest != null) {
                            if(amModificatDestinatia >= 0){
                                destinatii.set(amModificatDestinatia, dest);
                                amModificatDestinatia = -1;
                            }
                            else{
                                destinatii.add(dest);
                            }
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        });

        ButtonSetariPreferinte.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, SetariPreferinte.class);
                startActivity(intent);
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
                amModificatDestinatia = position;
                Intent intent = new Intent(MainActivity.this, AddDestinatieActivity.class);
                intent.putExtra("destinatie", (Parcelable) destinatii.get(position));
                launchDestinatieActivity.launch(intent);

            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //destinatii.remove(position);
                //adapter.notifyDataSetChanged();
                //return true;
                SalveazaObiectFavoritInFisier(destinatii.get(position));
                return true;
            }
        });
    }

    private void citesteDinFisier(){
        File file =new File(getFilesDir(), "fisier_destinatii");
        if(!file.exists()) return;
        try{
            FileInputStream fis = new FileInputStream(file);
            ObjectInputStream ois = new ObjectInputStream(fis);

            while(fis.available() > 0){
                Destinatie dest = (Destinatie) ois.readObject();
                destinatii.add(dest);
            }
            ois.close();
            fis.close();
        }catch(EOFException e){
            e.printStackTrace();
        }catch(IOException e){
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    /*
    private void citesteDinFisier(){
        try{
            FileInputStream fis = openFileInput("destinatii");
            byte []bytes = new byte[fis.available()];
            fis.read(bytes);
            fis.close();
            Parcel parcel = Parcel.obtain();
            parcel.unmarshall(bytes, 0, bytes.length);
            parcel.setDataPosition(0);
            Destinatie dest = Destinatie.CREATOR.createFromParcel(parcel);
            parcel.recycle();

            destinatii.add(dest);

        }catch (FileNotFoundException e){
            e.printStackTrace();
        }catch (IOException e){
            e.printStackTrace();
        }
    }
     */

    private void SalveazaObiectFavoritInFisier(Destinatie dest){
        try{
            File file = new File(getFilesDir(), "fisier_destinatii_favorite");
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

            Toast.makeText(this, "Destinatie salvata in favorite!", Toast.LENGTH_LONG).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e){
            e.printStackTrace();
        }
    }
}