package com.example.streamchat.activities;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.streamchat.utills.Constants;
import com.example.streamchat.utills.PreferenceManager;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class BaseActivity extends AppCompatActivity {

    private DocumentReference documentReference;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        PreferenceManager preferenceManager = new PreferenceManager(getApplicationContext());
        String userId = preferenceManager.getString(Constants.KEY_USER_ID);

        if (userId != null && !userId.isEmpty()) {
            FirebaseFirestore database = FirebaseFirestore.getInstance();
            documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                    .document(userId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        updateAvailabilityStatus(0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateAvailabilityStatus(1);
    }

    private void updateAvailabilityStatus(int status) {
        if (documentReference != null) {
            documentReference.update(Constants.KEY_AVAILABILITY, status);
        }
    }
}


