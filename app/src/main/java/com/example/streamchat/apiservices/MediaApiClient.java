package com.example.streamchat.apiservices;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MediaApiClient {
    private static Retrofit retrofit;

    public static Retrofit getClient(){
        if (retrofit == null){
            retrofit = new Retrofit.Builder()
                    .baseUrl("https://streamchat-media-services.onrender.com/")
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}
