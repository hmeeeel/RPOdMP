package com.example.myapplication;

import android.content.Context;
import android.content.pm.LabeledIntent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MuseumAdapter extends RecyclerView.Adapter<MuseumAdapter.ViewHolder> {
    private LayoutInflater inflater;
    private List<Museum> museums;

    MuseumAdapter(Context context, List<Museum> museums){
        this.inflater = LayoutInflater.from(context);
        this.museums = museums;
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
    }

    @Override
    public int getItemCount() {
        return museums.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        ImageView museumImage;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            museumImage = itemView.findViewById(R.id.museumImage);
        }
    }
}
