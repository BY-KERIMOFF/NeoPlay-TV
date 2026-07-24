package com.neoplay.tv.models;

public class Category {
    private String id;
    private String name;
    private int channelCount;

    public Category(String id, String name, int channelCount) {
        this.id = id;
        this.name = name;
        this.channelCount = channelCount;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getChannelCount() { return channelCount; }
}
