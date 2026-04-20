package com.example.laborator8;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class DestinatieDBHelper extends SQLiteOpenHelper {

    private static final String NUME_BAZA_DATE = "destinatii.db";
    private static final int VERSIUNE_BAZA_DATE = 1;

    private static final String TABEL = "destinatii";
    private static final String COL_ID = "id";
    private static final String COL_NUME = "nume";
    private static final String COL_DISTANTA = "distanta";
    private static final String COL_VIZITATA = "vizitata";
    private static final String COL_NR_ZILE = "nrZile";
    private static final String COL_RATING = "rating";
    private static final String COL_TIP = "tip";
    private static final String COL_AM_FOST_SINGUR = "amFostSingur";
    private static final String COL_AM_INCHIRIAT_MASINA = "amInchiriatMasina";
    private static final String COL_DATA = "dataDestinatiei";

    public DestinatieDBHelper(Context context){
        super(context, NUME_BAZA_DATE, null, VERSIUNE_BAZA_DATE);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "CREATE TABLE " + TABEL + " (" +
                COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NUME + " TEXT, " +
                COL_DISTANTA + " REAL, " +
                COL_VIZITATA + " INTEGER, " +
                COL_NR_ZILE + " INTEGER, " +
                COL_RATING + " REAL, " +
                COL_TIP + " TEXT, " +
                COL_AM_FOST_SINGUR + " INTEGER, " +
                COL_AM_INCHIRIAT_MASINA + " INTEGER, " +
                COL_DATA + " INTEGER" +
                ");";
        db.execSQL(sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABEL);
        onCreate(db);
    }

    // Metoda 1: Inserare in baza de date a unei destinatii
    public long inserareDestinatie(Destinatie d){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NUME, d.getNume());
        cv.put(COL_DISTANTA, d.getDistanta());
        cv.put(COL_VIZITATA, d.getVizitat() ? 1 : 0);
        cv.put(COL_NR_ZILE, d.getNrZile());
        cv.put(COL_RATING, d.getRating());
        cv.put(COL_TIP, d.getTip() != null ? d.getTip().name() : TipDestinatie.ORAS.name());
        cv.put(COL_AM_FOST_SINGUR, d.getAmFostSingur() ? 1 : 0);
        cv.put(COL_AM_INCHIRIAT_MASINA, d.getAmInchiriatMasina() ? 1 : 0);
        cv.put(COL_DATA, d.getDataDestinatiei() != null ? d.getDataDestinatiei().getTime() : -1);

        long id = db.insert(TABEL, null, cv);
        d.setId(id);
        db.close();
        return id;
    }

    // Metoda 2: Selectia tuturor inregistrarilor
    public List<Destinatie> selectareToate(){
        List<Destinatie> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABEL, null, null, null, null, null, null);
        while(c.moveToNext()){
            lista.add(cursorLaDestinatie(c));
        }
        c.close();
        db.close();
        return lista;
    }

    // Metoda 3: Selectia destinatiilor cu valoarea string egala cu parametru (nume)
    public List<Destinatie> selectareDupaNume(String nume){
        List<Destinatie> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABEL, null, COL_NUME + " = ?", new String[]{nume}, null, null, null);
        while(c.moveToNext()){
            lista.add(cursorLaDestinatie(c));
        }
        c.close();
        db.close();
        return lista;
    }

    // Metoda 4: Selectia destinatiilor cu nrZile intr-un interval [min, max]
    public List<Destinatie> selectareDupaNrZile(int min, int max){
        List<Destinatie> lista = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABEL, null,
                COL_NR_ZILE + " BETWEEN ? AND ?",
                new String[]{String.valueOf(min), String.valueOf(max)},
                null, null, null);
        while(c.moveToNext()){
            lista.add(cursorLaDestinatie(c));
        }
        c.close();
        db.close();
        return lista;
    }

    // Metoda 5: Stergerea inregistrarilor cu distanta mai mare decat parametru
    public int stergereDupaDistanta(double distanta){
        SQLiteDatabase db = getWritableDatabase();
        int nrSterse = db.delete(TABEL, COL_DISTANTA + " > ?", new String[]{String.valueOf(distanta)});
        db.close();
        return nrSterse;
    }

    // Metoda 6: Cresterea nrZile cu o unitate pentru destinatiile al caror nume incepe cu litera data
    public int crestereNrZileDupaLitera(char litera){
        SQLiteDatabase db = getWritableDatabase();
        String pattern = litera + "%";
        String sql = "UPDATE " + TABEL + " SET " + COL_NR_ZILE + " = " + COL_NR_ZILE + " + 1 " +
                "WHERE " + COL_NUME + " LIKE ? OR " + COL_NUME + " LIKE ?";

        db.execSQL(sql, new Object[]{String.valueOf(litera).toUpperCase() + "%",
                String.valueOf(litera).toLowerCase() + "%"});


        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TABEL + " WHERE " + COL_NUME + " LIKE ? OR " + COL_NUME + " LIKE ?",
                new String[]{String.valueOf(litera).toUpperCase() + "%",
                        String.valueOf(litera).toLowerCase() + "%"});
        int nrModificate = 0;
        if(c.moveToFirst()){
            nrModificate = c.getInt(0);
        }
        c.close();
        db.close();
        return nrModificate;
    }

    // metoda ajutatoare de conversie din cursor in obiect Destinatie
    @NonNull
    private Destinatie cursorLaDestinatie(Cursor c){
        Destinatie d = new Destinatie();
        d.setId(c.getLong(c.getColumnIndexOrThrow(COL_ID)));
        d.setNume(c.getString(c.getColumnIndexOrThrow(COL_NUME)));
        d.setDistanta(c.getDouble(c.getColumnIndexOrThrow(COL_DISTANTA)));
        d.setVizitat(c.getInt(c.getColumnIndexOrThrow(COL_VIZITATA)) == 1);
        d.setNrZile(c.getInt(c.getColumnIndexOrThrow(COL_NR_ZILE)));
        d.setRating(c.getFloat(c.getColumnIndexOrThrow(COL_RATING)));
        String tipStr = c.getString(c.getColumnIndexOrThrow(COL_TIP));
        d.setTip(tipStr != null ? TipDestinatie.valueOf(tipStr) : TipDestinatie.ORAS);
        d.setAmFostSingur(c.getInt(c.getColumnIndexOrThrow(COL_AM_FOST_SINGUR)) == 1);
        d.setAmInchiriatrMasina(c.getInt(c.getColumnIndexOrThrow(COL_AM_INCHIRIAT_MASINA)) == 1);
        long tmpDate = c.getLong(c.getColumnIndexOrThrow(COL_DATA));
        d.setData(tmpDate == -1 ? null : new Date(tmpDate));
        return d;
    }
}