package com.example.streamchat.apiservices;

import com.example.streamchat.model.MediaResponse;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface MediaApi {
    @Multipart
    @POST("api/media/upload")
    Call<MediaResponse> uploadMedia(
            @Header("Authorization") String token,
            @Part MultipartBody.Part file,
            @Part("receiverId") String receiverId
    );
}
