package com.example.streamchat.apiservices;

import com.example.streamchat.model.ApiResponse;
import com.example.streamchat.model.ResetPasswordRequest;
import com.example.streamchat.model.ResetPasswordResponse;
import com.example.streamchat.model.SendOtpRequest;
import com.example.streamchat.model.VerifyOtpRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ResetPasswordApi {
    @POST("api/auth/send-otp")
    Call<ApiResponse> sendOtp(@Body SendOtpRequest request);

    @POST("api/auth/verify-otp")
    Call<ApiResponse> verifyOtp(@Body VerifyOtpRequest request);

    @POST("api/auth/reset-password")
    Call<ResetPasswordResponse> resetPassword(@Body ResetPasswordRequest request);

}
