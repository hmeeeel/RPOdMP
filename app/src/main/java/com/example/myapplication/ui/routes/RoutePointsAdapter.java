package com.example.myapplication.ui.routes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;

import java.util.Collections;
import java.util.List;

public class RoutePointsAdapter extends RecyclerView.Adapter<RoutePointsAdapter.ViewHolder> {

    private final Context context;
    private final List<RoutePoint> routePoints;
    private OnItemActionListener listener;
    private OnStartDragListener dragListener;
    private boolean isDragging = false;
    public interface OnItemActionListener {
        void onRemovePoint(int position);
    }

    public interface OnStartDragListener {
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public RoutePointsAdapter(Context context, List<RoutePoint> routePoints) {
        this.context = context;
        this.routePoints = routePoints;
    }

    public void setOnItemActionListener(OnItemActionListener listener) {
        this.listener = listener;
    }

    public void setOnStartDragListener(OnStartDragListener listener) {
        this.dragListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_route_point, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RoutePoint point = routePoints.get(position);

        holder.textPointOrder.setText(String.valueOf(position + 1));

        if (point.getPlace() != null) {
            holder.textPlaceName.setText(point.getPlace().getName());

            String address = point.getPlace().getAddress();
            if (address != null && !address.isEmpty()) {
                holder.textPlaceAddress.setVisibility(View.VISIBLE);
                holder.textPlaceAddress.setText(address);
            } else {
                holder.textPlaceAddress.setVisibility(View.GONE);
            }
        }

        holder.btnRemovePoint.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRemovePoint(holder.getAdapterPosition());
            }
        });

        holder.dragHandle.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                if (dragListener != null) {
                    dragListener.onStartDrag(holder);
                }
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return routePoints.size();
    }

    // эл-т пересекает границу соседнего - 2 карточки
    public void onItemMove(int fromPosition, int toPosition) {
        // Меняются местами
        Collections.swap(routePoints, fromPosition, toPosition);

        // Обн point_order
        updatePointOrders();

        // Анимация перемещения карточки
        notifyItemMoved(fromPosition, toPosition);

        int min = Math.min(fromPosition, toPosition);
        int max = Math.max(fromPosition, toPosition);

        // перерисовка номеров у затронутых элементов
        notifyItemRangeChanged(min, max - min + 1);
    }

    private void updatePointOrders() {
        for (int i = 0; i < routePoints.size(); i++) {
            routePoints.get(i).setPointOrder(i + 1);
        }
    }

    public void removeItem(int position) {
        routePoints.remove(position);
        updatePointOrders();
        notifyItemRemoved(position);

        // Обно нумерацию оставшихся элементов
        notifyItemRangeChanged(position, routePoints.size());
    }

    public List<RoutePoint> getRoutePoints() {
        return routePoints;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView dragHandle;
        TextView textPointOrder;
        TextView textPlaceName;
        TextView textPlaceAddress;
        ImageButton btnRemovePoint;

        ViewHolder(View itemView) {
            super(itemView);
            dragHandle = itemView.findViewById(R.id.dragHandle);
            textPointOrder = itemView.findViewById(R.id.textPointOrder);
            textPlaceName = itemView.findViewById(R.id.textPlaceName);
            textPlaceAddress = itemView.findViewById(R.id.textPlaceAddress);
            btnRemovePoint = itemView.findViewById(R.id.btnRemovePoint);
        }
    }
}