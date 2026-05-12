package com.example.myapplication.ui.routes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.List;

public class RouteAdapter extends RecyclerView.Adapter<RouteAdapter.ViewHolder> {

    public interface OnRouteClickListener {
        void onRouteClick(RouteCard card);
        void onSaveClick(RouteCard card);
    }

    private final Context              context;
    private final List<RouteCard>      items;
    private final String               currentUserId;
    private final OnRouteClickListener listener;

    public RouteAdapter(Context context, List<RouteCard> items,
                        String currentUserId, OnRouteClickListener listener) {
        this.context       = context;
        this.items         = items;
        this.currentUserId = currentUserId;
        this.listener      = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_route_card, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        RouteCard card = items.get(position);

        h.textTitle.setText(card.getTitle());
        h.textAuthor.setText(card.getAuthorNickname());
        h.textPoints.setText(context.getString(R.string.route_points_count, card.getPointsCount()));

        // Рейтинг
        if (card.getReviewsCount() > 0) {
            h.textRating.setVisibility(View.VISIBLE);
            h.textRating.setText(String.format("★ %.1f (%d)", card.getAverageRating(), card.getReviewsCount()));
        } else {
            h.textRating.setVisibility(View.GONE);
        }

        // Статус-бейдж
        h.textStatus.setText(getStatusLabel(card));
        h.textStatus.setTextColor(getStatusColor(card));

        // Admin note
        if (card.getAdminNote() != null && !card.getAdminNote().isEmpty()) {
            h.textAdminNote.setVisibility(View.VISIBLE);
            h.textAdminNote.setText("💡 " + card.getAdminNote());
        } else {
            h.textAdminNote.setVisibility(View.GONE);
        }

        // Кнопка сохранения (только для чужих маршрутов)
        boolean isMyRoute = currentUserId != null && currentUserId.equals(card.getAuthorId());
        if (isMyRoute || !"published".equals(card.getStatusCode())) {
            h.btnSave.setVisibility(View.GONE);
        } else {
            h.btnSave.setVisibility(View.VISIBLE);
            h.btnSave.setImageResource(card.isSaved()
                    ? R.drawable.ic_bookmark_filled
                    : R.drawable.ic_bookmark_border);

            h.btnSave.setOnClickListener(v -> {
                if (listener != null) listener.onSaveClick(card);
            });
        }

        h.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRouteClick(card);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    private String getStatusLabel(RouteCard card) {
        boolean isMyRoute = currentUserId != null && currentUserId.equals(card.getAuthorId());
        if (card.isSaved() && !isMyRoute) return context.getString(R.string.status_saved);

        switch (card.getStatusCode() != null ? card.getStatusCode() : "") {
            case "published":   return isMyRoute ? context.getString(R.string.status_my)   : context.getString(R.string.status_public);
            case "pending":     return context.getString(R.string.status_pending);
            case "draft":       return context.getString(R.string.status_draft);
            case "unavailable": return context.getString(R.string.status_unavailable);
            default:            return "";
        }
    }

    private int getStatusColor(RouteCard card) {
        switch (card.getStatusCode() != null ? card.getStatusCode() : "") {
            case "published":   return context.getColor(android.R.color.holo_green_dark);
            case "pending":     return context.getColor(android.R.color.holo_orange_dark);
            case "unavailable": return context.getColor(R.color.error_red);
            default:            return context.getColor(android.R.color.darker_gray);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView    textTitle, textAuthor, textPoints, textRating, textStatus, textAdminNote;
        ImageButton btnSave;

        ViewHolder(View v) {
            super(v);
            textTitle     = v.findViewById(R.id.textRouteTitle);
            textAuthor    = v.findViewById(R.id.textRouteAuthor);
            textPoints    = v.findViewById(R.id.textRoutePoints);
            textRating    = v.findViewById(R.id.textRouteRating);
            textStatus    = v.findViewById(R.id.textRouteStatus);
            textAdminNote = v.findViewById(R.id.textAdminNote);
            btnSave       = v.findViewById(R.id.btnSaveRoute);
        }
    }
}