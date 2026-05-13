package com.example.myapplication.ui.admin;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ModerationAdapter extends RecyclerView.Adapter<ModerationAdapter.ViewHolder> {

    public interface ModerationListener {
        void onModerate(ModerationRoute route, String action, String note);
    }

    private final Context context;
    private final List<ModerationRoute> items;
    private final ModerationListener listener;

    public ModerationAdapter(Context context, List<ModerationRoute> items, ModerationListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.item_moderation_route, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int position) {
        Log.d("ModerationAdapter", "=== onBindViewHolder position=" + position + " ===");
        ModerationRoute route = items.get(position);
        Log.d("ModerationAdapter", "  route: " + route.getTitle() +
                ", author=" + route.getAuthorNickname() +
                ", points=" + route.getPointsCount());

        h.textTitle.setText(route.getTitle());
        h.textAuthor.setText(context.getString(R.string.author_format, route.getAuthorNickname()));
        h.textPoints.setText(context.getString(R.string.points_format, route.getPointsCount()));

        // Дата создания
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            Date date = sdf.parse(route.getCreatedAt().replace('T', ' ').substring(0, 16));
            h.textDate.setText(context.getString(R.string.created_format,
                    sdf.format(date != null ? date : new Date())));
        } catch (Exception e) {
            h.textDate.setText(route.getCreatedAt());
        }

        // Описание
        if (route.getDescription() != null && !route.getDescription().isEmpty()) {
            h.textDescription.setVisibility(View.VISIBLE);
            h.textDescription.setText(route.getDescription());
        } else {
            h.textDescription.setVisibility(View.GONE);
        }

        // Превью мест
        if (route.getPointsPreview() != null && !route.getPointsPreview().isEmpty()) {
            h.textPreview.setVisibility(View.VISIBLE);
            h.textPreview.setText(context.getString(R.string.places_preview, route.getPointsPreview()));
        } else {
            h.textPreview.setVisibility(View.GONE);
        }

        // Предыдущая заметка админа
        if (route.getPreviousAdminNote() != null && !route.getPreviousAdminNote().isEmpty()) {
            h.textPreviousNote.setVisibility(View.VISIBLE);
            h.textPreviousNote.setText(context.getString(R.string.previous_note, route.getPreviousAdminNote()));
        } else {
            h.textPreviousNote.setVisibility(View.GONE);
        }

        h.btnApprove.setOnClickListener(v -> showNoteDialog(route, "approve"));
        h.btnReject.setOnClickListener(v -> showNoteDialog(route, "reject"));
    }

    private void showNoteDialog(ModerationRoute route, String action) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_admin_note, null);
        EditText editNote = dialogView.findViewById(R.id.editAdminNote);

        String title = "approve".equals(action)
                ? context.getString(R.string.approve_route_title)
                : context.getString(R.string.reject_route_title);

        new AlertDialog.Builder(context)
                .setTitle(title)
                .setView(dialogView)
                .setPositiveButton("approve".equals(action) ? R.string.approve : R.string.reject,
                        (dialog, which) -> {
                            String note = editNote.getText().toString().trim();
                            if (listener != null) listener.onModerate(route, action, note);
                        })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textAuthor, textPoints, textDate, textDescription, textPreview, textPreviousNote;
        Button btnApprove, btnReject;

        ViewHolder(View v) {
            super(v);
            textTitle = v.findViewById(R.id.textModerationTitle);
            textAuthor = v.findViewById(R.id.textModerationAuthor);
            textPoints = v.findViewById(R.id.textModerationPoints);
            textDate = v.findViewById(R.id.textModerationDate);
            textDescription = v.findViewById(R.id.textModerationDescription);
            textPreview = v.findViewById(R.id.textModerationPreview);
            textPreviousNote = v.findViewById(R.id.textPreviousAdminNote);
            btnApprove = v.findViewById(R.id.btnApproveRoute);
            btnReject = v.findViewById(R.id.btnRejectRoute);
        }
    }
}