package com.example.laborator6;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class DestinatieAdapter extends BaseAdapter {
    private Context ctx;
    private List<Destinatie> destinatii;

    public DestinatieAdapter(Context ctx, List<Destinatie> destinatii){
        this.ctx = ctx;
        this.destinatii = destinatii;
    }

    @Override
    public int getCount() {
        return this.destinatii.size();
    }

    @Override
    public Object getItem(int position) {
        return this.destinatii.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(ctx);
        View v = inflater.inflate(R.layout.item_layout, parent, false);
        ImageView imagineDestinatie = v.findViewById(R.id.imagineDestinatie);
        TextView descriere = v.findViewById(R.id.descriere);
        Destinatie d = (Destinatie) getItem(position);
        imagineDestinatie.setImageResource(d.getImage());
        descriere.setText(d.toString());
        return v;
    }
}
