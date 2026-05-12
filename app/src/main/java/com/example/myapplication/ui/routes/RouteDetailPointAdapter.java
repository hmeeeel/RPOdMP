package com.example.myapplication.ui.routes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.model.Place;
import com.example.myapplication.data.serviceImage.ImageLoader;

import java.util.List;

public class RouteDetailPointAdapter
        extends RecyclerView.Adapter<RouteDetailPointAdapter.ViewHolder> {

    public interface OnPointClickListener {
        void onPointClick(Place place);
    }

    private final Context              context;
    private final List<RoutePoint>     items;
    private final OnPointClickListener listener;

    public RouteDetailPointAdapter(Context context,
                                   List<RoutePoint> items,
                                   OnPointClickListener listener) {
        this.context  = context;
        this.items    = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context)
                .inflate(R.layout.item_route_detail_point, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        RoutePoint rp    = items.get(position);
        Place      place = rp.getPlace();

        h.textOrder.setText(String.valueOf(rp.getPointOrder()));

        if (place == null) return;

        h.textName.setText(place.getName());

        if (place.getAddress() != null && !place.getAddress().isEmpty()) {
            h.textAddress.setVisibility(View.VISIBLE);
            h.textAddress.setText(place.getAddress());
        } else {
            h.textAddress.setVisibility(View.GONE);
        }

        if (place.hasCoordinates()) {
            h.textCoords.setVisibility(View.VISIBLE);
            h.textCoords.setText(place.getCoordinatesDisplay());
        } else {
            h.textCoords.setVisibility(View.GONE);
        }

        if (place.isVisited()) {
            h.textVisited.setVisibility(View.VISIBLE);
            h.textVisited.setText(context.getString(R.string.status_visited));
            h.textVisited.setTextColor(context.getColor(android.R.color.holo_green_dark));
        } else {
            h.textVisited.setVisibility(View.GONE);
        }

        List<String> imgs    = place.getImageIds();
        String       imgPath = (imgs != null && !imgs.isEmpty()) ? imgs.get(0) : null;
        ImageLoader.loadImage(context, imgPath, h.imagePlace);

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onPointClick(place);
        });
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView  textOrder, textName, textAddress, textCoords, textVisited;
        ImageView imagePlace;

        ViewHolder(View v) {
            super(v);
            textOrder   = v.findViewById(R.id.textDetailPointOrder);
            textName    = v.findViewById(R.id.textDetailPointName);
            textAddress = v.findViewById(R.id.textDetailPointAddress);
            textCoords  = v.findViewById(R.id.textDetailPointCoords);
            textVisited = v.findViewById(R.id.textDetailPointVisited);
            imagePlace  = v.findViewById(R.id.imageDetailPoint);
        }
    }
}