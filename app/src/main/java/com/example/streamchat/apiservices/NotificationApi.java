package com.example.streamchat.apiservices;

import java.util.Map;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface NotificationApi {

    @POST("api/sendNotification")
    Call<Map<String, Object>> sendNotification(
            @Header("x-api-key") String apiKey,
            @Body Map<String, Object> body
    );
}
