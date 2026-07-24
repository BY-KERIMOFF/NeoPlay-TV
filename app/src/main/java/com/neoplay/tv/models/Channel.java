package com.neoplay.tv.models;

import java.io.Serializable;

public class Channel implements Serializable {
    private String id;
    private String name;
    private String logoUrl;
    private String streamUrl;
    private String categoryName;
    private String tvgId;

    public Channel(String id, String name, String logoUrl, String streamUrl, String categoryName) {
        this(id, name, logoUrl, streamUrl, categoryName, "");
    }

    public Channel(String id, String name, String logoUrl, String streamUrl, String categoryName, String tvgId) {
        this.id = id;
        this.name = name;
        this.logoUrl = logoUrl;
        this.streamUrl = streamUrl;
        this.categoryName = categoryName;
        this.tvgId = tvgId;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getLogoUrl() { return logoUrl; }
    public String getStreamUrl() { return streamUrl; }
    public String getCategoryName() { return categoryName; }
    public String getTvgId() { return tvgId; }
    public void setTvgId(String tvgId) { this.tvgId = tvgId; }
}
