package com.neoplay.tv.api;

import com.neoplay.tv.models.XtreamCategory;
import com.neoplay.tv.models.XtreamChannel;
import com.neoplay.tv.models.XtreamEpg;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Url;

public interface ApiService {
    @GET
    Call<ApiResponse> checkMac(@Url String url);

    @GET
    Call<List<XtreamCategory>> getXtreamCategories(@Url String url);

    @GET
    Call<List<XtreamChannel>> getXtreamChannels(@Url String url);

    @GET
    Call<List<XtreamChannel>> getXtreamVod(@Url String url);

    @GET
    Call<List<XtreamCategory>> getXtreamVodCategories(@Url String url);

    @GET
    Call<List<XtreamCategory>> getXtreamSeriesCategories(@Url String url);

    @GET
    Call<XtreamEpg> getXtreamEpg(@Url String url);

    @GET
    Call<okhttp3.ResponseBody> getRawResponse(@Url String url);
}
