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

public class TrainingPagerAdapter
        extends RecyclerView.Adapter<TrainingPagerAdapter.ViewHolder> {

    private final List<TrainingStep> steps;

    public TrainingPagerAdapter(List<TrainingStep> steps) {
        this.steps = steps;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_training_page, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(
            @NonNull ViewHolder holder, int position) {

        TrainingStep step = steps.get(position);
        holder.image.setImageResource(step.imageRes);
        holder.title.setText(step.title);
        holder.description.setText(step.description);
    }

    @Override
    public int getItemCount() {
        return steps.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView image;
        TextView title;
        TextView description;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.imageStep);
            title = itemView.findViewById(R.id.textTitle);
            description = itemView.findViewById(R.id.textDescription);
        }
    }
}
