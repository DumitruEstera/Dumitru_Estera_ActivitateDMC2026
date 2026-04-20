package com.example.laborator9;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ListaImaginiActivity extends AppCompatActivity {

    private ListView listView;
    private ImagineAdapter adapter;
    private List<ImagineDestinatie> imagini = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lista_imagini);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        listView = findViewById(R.id.listViewImagini);

        populeazaListaImagini();

        adapter = new ImagineAdapter(this, imagini);
        listView.setAdapter(adapter);

        incarcaImaginiAsync();

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ImagineDestinatie imagine = imagini.get(position);
                Intent intent = new Intent(ListaImaginiActivity.this, WebViewActivity.class);
                intent.putExtra("url", imagine.getUrlSite());
                startActivity(intent);
            }
        });
    }

    private void populeazaListaImagini(){
        imagini.add(new ImagineDestinatie(
                "https://travelator.ro/wp-content/uploads/2018/10/kotor_main.jpg",
                "Kotor, Muntenegru - Colt de paradis european",
                "https://travelator.ro/15-destinatii-europene-care-arata-ca-un-mic-colt-de-paradis/"
        ));
        imagini.add(new ImagineDestinatie(
                "https://alinainwonderland.ro/wp-content/uploads/2020/03/Italia_amalfi-2-1024x576.jpg",
                "Coasta Amalfi, Italia - Destinatie de vara",
                "https://alinainwonderland.ro/europa-5-tari-10-destinatii-de-incercat-vara-asta/"
        ));
        imagini.add(new ImagineDestinatie(
                "https://travelator.ro/wp-content/uploads/2017/03/8-Navagio-Zakynthos-850x546.jpg",
                "Plaja Navagio, Zakynthos - Cele mai frumoase plaje",
                "https://travelator.ro/cele-mai-frumoase-plaje-din-europa/"
        ));
        imagini.add(new ImagineDestinatie(
                "https://www.destinatii.info/wp-content/themes/destinatii/images/patterns/pattern_1.jpg",
                "Ghid de destinatii turistice",
                "https://www.destinatii.info/"
        ));
        imagini.add(new ImagineDestinatie(
                "https://www.manager.ro/dbimg/articole/top-destinatii-vacanta-2022_110475.jpg",
                "Top 5 destinatii de vacanta 2022",
                "https://www.manager.ro/articole/turism-141/top-5-cele-mai-bune-destinatii-de-vacanta-pentru-2022-110475.html"
        ));
    }

    private void incarcaImaginiAsync(){
        ExecutorService executor = Executors.newFixedThreadPool(5);
        Handler handler = new Handler(Looper.getMainLooper());

        for (ImagineDestinatie imagine : imagini){
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try{
                        URL url = new URL(imagine.getUrlImagine());
                        InputStream is = url.openStream();
                        Bitmap bmp = BitmapFactory.decodeStream(is);
                        is.close();

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                imagine.setBitmap(bmp);
                                adapter.notifyDataSetChanged();
                            }
                        });
                    }catch(Exception e){
                        e.printStackTrace();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(ListaImaginiActivity.this,
                                        "Eroare la incarcarea imaginii: " + imagine.getDescriere(),
                                        Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }

        executor.shutdown();
    }
}