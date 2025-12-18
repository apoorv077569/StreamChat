package com.example.streamchat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.streamchat.apiservices.ResetPasswordApi;
import com.example.streamchat.apiservices.ResetPasswordApiClient;
import com.example.streamchat.databinding.ActivityResetPasswordBinding;
import com.example.streamchat.model.ApiResponse;
import com.example.streamchat.model.SendOtpRequest;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SendOtpActivity extends AppCompatActivity {

    private ActivityResetPasswordBinding binding;
    private FirebaseAuth auth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        auth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // FIXED BACK BUTTON HANDLER
        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());

        // RESET BUTTON
        binding.btnReset.setOnClickListener(v -> sendResetEmail());
    }

    private void sendResetEmail() {
        String email = binding.inputEmail.getText().toString().trim();

        if (email.isEmpty()) {
            binding.inputEmail.setError("Please enter email");
            showToast("Please enter email");
            return;
        }

        setLoading(true);

        ResetPasswordApi api = ResetPasswordApiClient
                .getClient()
                .create(ResetPasswordApi.class);

        api.sendOtp(new SendOtpRequest(email)).enqueue(new Callback<ApiResponse>() {
            @Override
            public void onResponse(Call<ApiResponse> call, Response<ApiResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {

                    ApiResponse apiResponse = response.body();

                    if (apiResponse.message != null
                            && apiResponse.message.equals("OTP sent successfully")) {

                        Log.d("OTP", "Redirecting to OTP screen...");
                        showToast(apiResponse.message);

                        Intent intent = new Intent(
                                SendOtpActivity.this,
                                OtpVerificationActivity.class
                        );
                        intent.putExtra("email", email);
                        intent.putExtra("otp", apiResponse.otpCode); // ✅ pass OTP
                        startActivity(intent);
                        finish();

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
                setLoading(false);
                showToast("Network Failed: " + throwable.getMessage());
                Log.e("OTP", throwable.getMessage());
            }
        });
    }


    private void setLoading(boolean loading) {
        if (loading) {
            binding.loading.setVisibility(View.VISIBLE);
            binding.btnReset.setVisibility(View.GONE);
        } else {
            binding.loading.setVisibility(View.GONE);
            binding.btnReset.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
