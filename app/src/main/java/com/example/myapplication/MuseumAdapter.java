package com.example.myapplication;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MuseumAdapter extends RecyclerView.Adapter<MuseumAdapter.ViewHolder> {
    private LayoutInflater inflater;
    private List<Museum> museums;
    private IMuseumClick click;

    MuseumAdapter(Context context, List<Museum> museums, IMuseumClick click){
        this.inflater = LayoutInflater.from(context);
        this.museums = museums;
        this.click = click;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.museum_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MuseumAdapter.ViewHolder holder, int position) {
        Museum museum = museums.get(position);
        holder.name.setText(museum.getName());
        holder.museumImage.setImageResource(museum.getImageId());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (click != null) {
                    click.onMuseumClick(museum);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return museums.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        ImageView museumImage;
        public ViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            museumImage = itemView.findViewById(R.id.museumImage);
        }
    }
}
