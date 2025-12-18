package com.example.streamchat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.streamchat.apiservices.ResetPasswordApi;
import com.example.streamchat.apiservices.ResetPasswordApiClient;
import com.example.streamchat.databinding.ActivityOtpVerificationBinding;
import com.example.streamchat.model.ApiResponse;
import com.example.streamchat.model.SendOtpRequest;
import com.example.streamchat.model.VerifyOtpRequest;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class OtpVerificationActivity extends AppCompatActivity {

    private ActivityOtpVerificationBinding binding;
    private CountDownTimer timer;
    private String email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            binding = ActivityOtpVerificationBinding.inflate(getLayoutInflater());
            setContentView(binding.getRoot());

            email = getIntent().getStringExtra("email");
            Log.d("OTP_SCREEN", "Email received: " + email);
            binding.emailText.setText(email);
            setListener();
            startResendTimer();

        } catch (Exception e) {
            Log.e("OTP_CRASH", e.getMessage());
        }
    }

    private void verifyOtp() {
        String otp = binding.otp1.getText().toString()
                + binding.otp2.getText().toString()
                + binding.otp3.getText().toString()
                + binding.otp4.getText().toString();

        if (otp.length() < 4) {
            showToast("Please enter valid otp");
            return;
        }
        ResetPasswordApi api = ResetPasswordApiClient.getClient().create(ResetPasswordApi.class);
        api.verifyOtp(
                new VerifyOtpRequest(email, otp)
        ).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    showToast("otp verified successfully");
                    Intent intent = new Intent(OtpVerificationActivity.this, ResetPasswordActivity.class);
                    intent.putExtra("email", email);
                    startActivity(intent);
                } else {
                    showToast("Invalid Otp");
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                showToast("API Failure: " + throwable.getLocalizedMessage());
            }
        });
    }

    private void setListener() {
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnVerify.setOnClickListener(v -> verifyOtp());
        binding.resend.setOnClickListener(v -> resendOtp());
    }

    private void resendOtp() {
        binding.resend.setEnabled(false);
        ResetPasswordApi api = ResetPasswordApiClient.getClient().create(ResetPasswordApi.class);
        api.sendOtp(new SendOtpRequest(email)).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {

                if (response.isSuccessful() && response.body() != null) {

                    ApiResponse apiResponse = response.body();

                    if (apiResponse.message != null
                            && apiResponse.message.equals("OTP sent successfully")) {

                        showToast("Otp resend successfully");

                    } else {
                        showToast(apiResponse.message);
                        Log.e("OTP", apiResponse.message);
                    }

                } else {
                    showToast("Server error: " + response.code());
                    Log.e("OTP", "Error Code: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse> call, Throwable throwable) {
                showToast("Network Failed: " + throwable.getMessage());
                Log.e("OTP", throwable.getMessage());
            }
        });

    }

    private void startResendTimer() {
        binding.resend.setEnabled(false);
        timer = new CountDownTimer(56000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                binding.resend.setText("Resend - 00:" + millisUntilFinished / 1000);
            }

            @Override
            public void onFinish() {
                binding.resend.setText("Resend");
                binding.resend.setEnabled(true);
            }
        }.start();

    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
