package com.example.laborator9;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class ImagineAdapter extends BaseAdapter {
    private Context ctx;
    private List<ImagineDestinatie> imagini;

    public ImagineAdapter(Context ctx, List<ImagineDestinatie> imagini){
        this.ctx = ctx;
        this.imagini = imagini;
    }

    @Override
    public int getCount() {
        return this.imagini.size();
    }

    @Override
    public Object getItem(int position) {
        return this.imagini.get(position);
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(ctx);
        View v = inflater.inflate(R.layout.item_imagine_layout, parent, false);
        ImageView imagineView = v.findViewById(R.id.imagineDestinatie);
        TextView descriere = v.findViewById(R.id.descriere);
        ProgressBar progressBar = v.findViewById(R.id.progressBar);

        ImagineDestinatie imagine = (ImagineDestinatie) getItem(position);
        descriere.setText(imagine.getDescriere());

        if(imagine.getBitmap() != null){
            imagineView.setImageBitmap(imagine.getBitmap());
            imagineView.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }else{
            imagineView.setVisibility(View.INVISIBLE);
            progressBar.setVisibility(View.VISIBLE);
        }

        return v;
    }
}