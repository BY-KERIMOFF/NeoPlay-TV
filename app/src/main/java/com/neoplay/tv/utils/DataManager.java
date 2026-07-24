package com.neoplay.tv.utils;

import com.neoplay.tv.models.Category;
import com.neoplay.tv.models.Channel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DataManager {
    private static List<Channel> currentChannelList = new ArrayList<>();
    private static List<Category> currentCategoryList = new ArrayList<>();
    private static Map<String, List<Channel>> currentChannelMap = new HashMap<>();
    private static String globalEpgUrl = "";
    private static Map<String, String> xmltvCache = new HashMap<>();
    private static String adminAnnouncement = "";
    
    public static void setGlobalEpgUrl(String url) {
        globalEpgUrl = url;
    }

    public static String getGlobalEpgUrl() {
        return globalEpgUrl;
    }

    public static void setXmltvCache(Map<String, String> cache) {
        xmltvCache = new HashMap<>(cache);
    }

    public static void mergeXmltvCache(Map<String, String> cache) {
        if (xmltvCache == null) xmltvCache = new HashMap<>();
        xmltvCache.putAll(cache);
    }

    public static Map<String, String> getXmltvCache() {
        return xmltvCache;
    }

    public static void setAdminAnnouncement(String announcement) {
        adminAnnouncement = announcement;
    }

    public static String getAdminAnnouncement() {
        return adminAnnouncement;
    }

    public static void setCurrentChannelList(List<Channel> list) {
        currentChannelList = new ArrayList<>(list);
    }
    
    public static List<Channel> getCurrentChannelList() {
        return currentChannelList;
    }

    public static void setCurrentCategoryList(List<Category> list) {
        currentCategoryList = new ArrayList<>(list);
    }

    public static List<Category> getCurrentCategoryList() {
        return currentCategoryList;
    }

    public static void setCurrentChannelMap(Map<String, List<Channel>> map) {
        currentChannelMap = new HashMap<>(map);
    }

    public static Map<String, List<Channel>> getCurrentChannelMap() {
        return currentChannelMap;
    }
    
    public static void clear() {
        currentChannelList.clear();
        currentCategoryList.clear();
        currentChannelMap.clear();
    }
}
