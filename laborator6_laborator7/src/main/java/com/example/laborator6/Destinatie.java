package com.example.laborator6;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Destinatie implements Parcelable, Serializable {
    private static final long serialVersionUID = 1L;
    private String nume;
    private double distanta;
    private boolean vizitata;
    private int nrZile;
    private float rating;
    private TipDestinatie tip;
    private boolean amFostSingur;
    private boolean amInchiriatMasina;
    private Date dataDestinatiei;

    public Destinatie(){}

    public Destinatie(String nume, double distanta, boolean vizitat, int nrZile, float reating, TipDestinatie tip, boolean amFostSingur, boolean amInchiriatMasina, Date dataDestinatie){
        this.nume = nume;
        this.distanta = distanta;
        this.vizitata = vizitat;
        this.nrZile = nrZile;
        this.rating = reating;
        this.tip = tip;
        this.amInchiriatMasina = amInchiriatMasina;
        this.amFostSingur = amFostSingur;
        this.dataDestinatiei = dataDestinatie;
    }

    protected Destinatie(Parcel in) {
        nume = in.readString();
        distanta = in.readDouble();
        vizitata = in.readByte() != 0;
        nrZile = in.readInt();
        rating = in.readFloat();
        tip = TipDestinatie.valueOf(in.readString());
        amFostSingur = in.readByte() != 0;
        amInchiriatMasina = in.readByte() != 0;
        long tmpDate = in.readLong();
        dataDestinatiei = tmpDate == -1 ? null : new Date(tmpDate);
    }

    public static final Creator<Destinatie> CREATOR = new Creator<Destinatie>() {
        @Override
        public Destinatie createFromParcel(Parcel in) {
            return new Destinatie(in);
        }

        @Override
        public Destinatie[] newArray(int size) {
            return new Destinatie[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(nume);
        dest.writeDouble(distanta);
        dest.writeByte((byte) (vizitata ? 1 : 0));
        dest.writeInt(nrZile);
        dest.writeFloat(rating);
        dest.writeString(tip.name());
        dest.writeByte((byte) (amFostSingur ? 1 : 0));
        dest.writeByte((byte) (amInchiriatMasina ? 1 : 0));
        dest.writeLong(dataDestinatiei != null ? dataDestinatiei.getTime() : -1);
    }

    public int getImage(){
        switch (this.tip){
            case PLAJA:
                return R.drawable.plaja;
            case MUNTE:
                return R.drawable.munte;
            case ORAS:
                return R.drawable.oras;
            case RURAL:
                return R.drawable.rural;
            case INSULA:
                return R.drawable.insula;
            default:
                return 0;
        }
    }

    public String getNume() {return this.nume;}
    public void setNume(String nume) {this.nume = nume;}
    public double getDistanta() {return this.distanta;}
    public void setDistanta(double distanta) {this.distanta = distanta;}
    public boolean getVizitat() {return this.vizitata;}
    public void setVizitat(boolean vizitata) {this.vizitata = vizitata;}
    public int getNrZile() {return this.nrZile;}
    public void setNrZile(int nrZile){this.nrZile = nrZile;}
    public float getRating() {return this.rating;}
    public void setRating(float reating) {this.rating = reating;}
    public TipDestinatie getTip(){return this.tip;}
    public void setTip(TipDestinatie tip) {this.tip = tip;}
    public boolean getAmFostSingur(){return this.amFostSingur;}
    public void setAmFostSingur(boolean amFostSingur) {this.amFostSingur = amFostSingur;}
    public boolean getAmInchiriatMasina(){return this.amInchiriatMasina;}
    public void setAmInchiriatrMasina(boolean amInchiriatMasina){this.amInchiriatMasina = amInchiriatMasina;}
    public Date getDataDestinatiei(){ return this.dataDestinatiei; }
    public void setData(Date dataDestinatiei) { this.dataDestinatiei = dataDestinatiei; }

    @Override
    public String toString(){
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/YYYY");
        return "Destinatie: " + this.nume +
                "\nDistanta: " + this.distanta +
                "\nVizitat: " + this.vizitata +
                "\nNr zile: " + this.nrZile +
                "\nRating: " + this.rating +
                "\nTip: " + this.tip +
                "\nAm fost singur:" + this.amFostSingur +
                "\nAm inchiriat masina: " + this.amInchiriatMasina +
                "\nAm fost in data de: " + sdf.format(this.dataDestinatiei) + "\n";
    }
}