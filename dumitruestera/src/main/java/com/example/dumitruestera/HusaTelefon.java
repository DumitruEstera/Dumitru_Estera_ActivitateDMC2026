package com.example.dumitruestera;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.security.PublicKey;

public class HusaTelefon implements Parcelable, Serializable {
    private String material;
    private int lungime;
    private String culoare;
    private String model;

    public HusaTelefon(){}

    public HusaTelefon(String material, int lungime, String culoare, String model){
        this.material = material;
        this.lungime = lungime;
        this.culoare = culoare;
        this.model = model;
    }

    protected HusaTelefon(Parcel in) {
        material = in.readString();
        lungime = in.readInt();
        culoare = in.readString();
        model = in.readString();
    }

    public static final Creator<HusaTelefon> CREATOR = new Creator<HusaTelefon>() {
        @Override
        public HusaTelefon createFromParcel(Parcel in) {
            return new HusaTelefon(in);
        }

        @Override
        public HusaTelefon[] newArray(int size) {
            return new HusaTelefon[size];
        }
    };

    public String getMaterial(){return this.material;}
    public int getLungime(){return this.lungime;}
    public String getCuloare(){return this.culoare;}
    public String getModel(){return this.model;}


    @Override
    public String toString(){
        return "Husa Telefon: "  +
                "\nMaterial: " + this.material +
                "\nLungime: " + this.lungime +
                "\nCuloare: " + this.culoare +
                "\nmodel: " + this.model + "\n";
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeString(material);
        dest.writeInt(lungime);
        dest.writeString(culoare);
        dest.writeString(model);
    }
}
