package com.example.myapplication.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.myapplication.ui.detail.IMuseumClick;
import com.example.myapplication.ui.detail.ImageSliderAdapter;
import com.example.myapplication.R;
import com.example.myapplication.data.model.Museum;

import java.util.List;

public class MuseumAdapter extends RecyclerView.Adapter<MuseumAdapter.ViewHolder> {
    private final LayoutInflater inflater;
    private final List<Museum> museums;
    private final IMuseumClick click;

    MuseumAdapter(Context context, List<Museum> museums, IMuseumClick click){
        this.inflater = LayoutInflater.from(context);
        this.museums = museums;
        this.click = click;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        View view = inflater.inflate(R.layout.museum_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(MuseumAdapter.ViewHolder holder, int position) {
        Museum museum = museums.get(position);
        holder.name.setText(museum.getName());

        holder.name.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (click != null) {
                    click.onMuseumClick(museum);
                }
            }
        });

        ImageSliderAdapter sliderAdapter = new ImageSliderAdapter(museum.getImageIds(), () -> {
            if (click != null) click.onMuseumClick(museum);
        });
        holder.museumImage.setAdapter(sliderAdapter);
    }

    @Override
    public int getItemCount() {
        return museums.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder{
        TextView name;
        ViewPager2 museumImage;
        public ViewHolder(View itemView) {
            super(itemView);

            name = itemView.findViewById(R.id.name);
            museumImage = itemView.findViewById(R.id.museumImage);
        }
    }
}
