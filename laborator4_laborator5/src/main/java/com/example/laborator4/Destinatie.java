package com.example.laborator4;

import java.io.Serializable;import java.text.SimpleDateFormat;import java.util.Calendar;import java.util.Date;

public class Destinatie implements Serializable {
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

    public Destinatie(String nume, double distanta, boolean vizitat, int nrZile, float reating, TipDestinatie tip, boolean amInchiriatMasina, boolean amFostSingur, Date dataDestinatie){
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
