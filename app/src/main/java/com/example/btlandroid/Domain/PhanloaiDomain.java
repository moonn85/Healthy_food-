package com.example.btlandroid.Domain;

public class PhanloaiDomain {
    private String ImagePath;
    private String Name;
    private int Id;
    public PhanloaiDomain() {

    }
    @Override
    public String toString() {
        return Name;
    }

    public String getImagePath() {
        return ImagePath;
    }

    public void setImagePath(String imagePath) {
        ImagePath = imagePath;
    }

    public String getName() {
        return Name;
    }

    public void setName(String name) {
        Name = name;
    }

    public int getId() {
        return Id;
    }

    public void setId(int id) {
        Id = id;
    }
}
