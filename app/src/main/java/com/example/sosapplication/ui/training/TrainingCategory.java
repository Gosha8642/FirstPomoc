package com.example.sosapplication.ui.training;

public class TrainingCategory {
    public final int id;
    public final int iconRes;
    public final String title;
    public final String description;

    public TrainingCategory(int id, int iconRes, String title, String description) {
        this.id = id;
        this.iconRes = iconRes;
        this.title = title;
        this.description = description;
    }
}
