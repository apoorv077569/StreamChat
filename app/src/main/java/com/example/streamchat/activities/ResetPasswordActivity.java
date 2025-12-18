package com.example.streamchat.activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.streamchat.apiservices.ResetPasswordApi;
import com.example.streamchat.apiservices.ResetPasswordApiClient;
import com.example.streamchat.databinding.ActivityResetPassword2Binding;
import com.example.streamchat.model.ResetPasswordRequest;
import com.example.streamchat.model.ResetPasswordResponse;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ResetPasswordActivity extends AppCompatActivity {

    ActivityResetPassword2Binding binding;
    private String email;

    private static final String TAG = "RESET_PASSWORD";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityResetPassword2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        email = getIntent().getStringExtra("email");

        Log.d(TAG, "Email received: " + email);

        binding.btnUpdate.setOnClickListener(v -> resetPassword(email));
    }

    private void resetPassword(String email) {

        String pass = binding.inputPassword.getText().toString().trim();
        String confirm = binding.inputConfirmPassword.getText().toString().trim();

        Log.d(TAG, "Entered Password: " + pass);
        Log.d(TAG, "Entered Confirm Password: " + confirm);

        if (!pass.equals(confirm)) {
            Log.e(TAG, "Password mismatch");
            showToast("Password didn't match");
            return;
        }

        ResetPasswordRequest request = new ResetPasswordRequest(email, pass);

        Log.d(TAG, "Sending request to API...");
        Log.d(TAG, "Email: " + email);
        Log.d(TAG, "New Password: " + pass);

        ResetPasswordApi api = ResetPasswordApiClient.getClient().create(ResetPasswordApi.class);

        api.resetPassword(request).enqueue(new Callback<ResetPasswordResponse>() {
            @Override
            public void onResponse(Call<ResetPasswordResponse> call, Response<ResetPasswordResponse> response) {

                Log.d(TAG, "Response Code: " + response.code());
                Log.d(TAG, "Response Success: " + response.isSuccessful());

                if (response.body() != null) {
                    Log.d(TAG, "Response Message: " + response.body().getMessage());
                } else {
                    Log.e(TAG, "Response Body is NULL");
                }

                if (response.isSuccessful() && response.body() != null) {
                    showToast("Password Updated Successfully");
                    startActivity(new Intent(ResetPasswordActivity.this, SigninActivity.class));
                    finish();
                } else {
                    showToast("Reset Password failed");
                }
            }

            @Override
            public void onFailure(Call<ResetPasswordResponse> call, Throwable throwable) {
                Log.e(TAG, "API Failure: " + throwable.getMessage());
                showToast("Server Error: " + throwable.getLocalizedMessage());
            }
        });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}
