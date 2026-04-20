package com.example.laborator9;

import android.graphics.Bitmap;

import java.io.Serializable;

public class ImagineDestinatie implements Serializable {
    private String urlImagine;
    private String descriere;
    private String urlSite;
    private transient Bitmap bitmap;

    public ImagineDestinatie(){}

    public ImagineDestinatie(String urlImagine, String descriere, String urlSite){
        this.urlImagine = urlImagine;
        this.descriere = descriere;
        this.urlSite = urlSite;
    }

    public String getUrlImagine() {return this.urlImagine;}
    public void setUrlImagine(String urlImagine) {this.urlImagine = urlImagine;}
    public String getDescriere() {return this.descriere;}
    public void setDescriere(String descriere) {this.descriere = descriere;}
    public String getUrlSite() {return this.urlSite;}
    public void setUrlSite(String urlSite) {this.urlSite = urlSite;}
    public Bitmap getBitmap() {return this.bitmap;}
    public void setBitmap(Bitmap bitmap) {this.bitmap = bitmap;}

    @Override
    public String toString(){
        return this.descriere;
    }
}