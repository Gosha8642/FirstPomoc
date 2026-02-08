package com.example.sosapplication.ui.training;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.sosapplication.R;

import java.util.List;

public class TrainingCategoryAdapter extends RecyclerView.Adapter<TrainingCategoryAdapter.ViewHolder> {

    private final List<TrainingCategory> categories;
    private final OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(TrainingCategory category);
    }

    public TrainingCategoryAdapter(List<TrainingCategory> categories, OnCategoryClickListener listener) {
        this.categories = categories;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_training_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TrainingCategory category = categories.get(position);
        holder.icon.setImageResource(category.iconRes);
        holder.title.setText(category.title);
        holder.description.setText(category.description);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onCategoryClick(category);
            }
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView title;
        TextView description;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.trainingIcon);
            title = itemView.findViewById(R.id.trainingTitle);
            description = itemView.findViewById(R.id.trainingDescription);
        }
    }
}
