package com.energyapp.model;

public class Insight {
    private String title;
    private String description;
    private String iconPath;
    private double priority;

    public Insight(String title, String description, String iconPath, double priority) {
        this.title = title;
        this.description = description;
        this.iconPath = iconPath;
        this.priority = priority;
    }

    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getIconPath() { return iconPath; }
}