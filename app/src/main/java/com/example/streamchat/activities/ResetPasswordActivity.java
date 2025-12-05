package com.example.streamchat.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.streamchat.databinding.ActivityResetPasswordBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class ResetPasswordActivity extends AppCompatActivity {

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

        binding.btnBack.setOnClickListener(v -> getOnBackPressedDispatcher());
        binding.btnReset.setOnClickListener(V -> sendResetEmail());

    }

    private void sendResetEmail() {
        String email = binding.inputEmail.getText().toString();

        if (email.isEmpty()) {
            binding.inputEmail.setError("Enter email");
            return;
        }
        firestore.collection("users")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(query -> {
                    if (query.isEmpty()) {
                        showToast("Email doesn't exist");
                        return;
                    }
                    auth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(u -> {
                                showToast("Reset Password send to your email");
                            })
                            .addOnFailureListener(e -> {
                                showToast("Error due to: " + e.getLocalizedMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    showToast(e.getLocalizedMessage());
                });

    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}