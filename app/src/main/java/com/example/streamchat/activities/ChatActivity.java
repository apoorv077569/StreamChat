package com.example.streamchat.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.streamchat.R;
import com.example.streamchat.adapter.ChatAdapter;
import com.example.streamchat.apiservices.ApiClient;
import com.example.streamchat.apiservices.MediaApi;
import com.example.streamchat.apiservices.MediaApiClient;
import com.example.streamchat.apiservices.NotificationApi;
import com.example.streamchat.databinding.ActivityChatBinding;
import com.example.streamchat.modals.ChatMessages;
import com.example.streamchat.modals.Users;
import com.example.streamchat.model.MediaResponse;
import com.example.streamchat.utills.Constants;
import com.example.streamchat.utills.EncryptionUtil;
import com.example.streamchat.utills.FileUtils;
import com.example.streamchat.utills.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;

import javax.crypto.SecretKey;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {

    private ActivityChatBinding binding;
    private Users receiverUser;
    private PreferenceManager preferenceManager;
    private ChatAdapter chatAdapter;
    private String receiverId;
    private MediaApi mediaApi;
    private List<ChatMessages> chatMessages;
    private FirebaseFirestore database;
    private String conversionId = null;
    private Boolean isReceiverAvailable = false;
    // Launchers
    private ActivityResultLauncher<Intent> galleryLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private ActivityResultLauncher<Intent> fileLauncher;
    private static final int CAMERA_PERMISSION_REQUEST = 101;
    private static final int GALLERY_PERMISSION_REQUEST = 102;
    private static final int FILE_PERMISSION_REQUEST = 103;

    private static final String TAG = "MEDIA_DEBUG";


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        mediaApi = MediaApiClient.getClient().create(MediaApi.class);
        setListener();
        initLaunchers();
        loadReceiverDetail();
    }

    private void initLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == ChatActivity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                Log.d(TAG, "Media selected: " + uri.toString());
                uploadMedia(uri);
            }
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == ChatActivity.RESULT_OK && result.getData() != null) {
                Bitmap bitmap = (Bitmap) result.getData().getExtras().get("data");

                try {
                    File file = bitmapToFile(bitmap);
                    Log.d(TAG, "Media selected: " + file);
                    uploadMedia(Uri.fromFile(file));
                } catch (Exception e) {
                    Toast.makeText(this, e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                }

            }
        });
        fileLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == ChatActivity.RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                Log.d(TAG, "Media selected: " + uri.toString());
                uploadMedia(uri);
            }
        });
    }

    @NonNull
    private File bitmapToFile(@NonNull Bitmap bitmap) throws Exception {
        File file = new File(getCacheDir(), "camera_" + System.currentTimeMillis() + ".jpg");
        FileOutputStream fos = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
        fos.flush();
        fos.close();

        return file;
    }

    private void uploadMedia(Uri uri) {
        FirebaseAuth.getInstance().getCurrentUser().getIdToken(true).addOnSuccessListener(tokenResult -> {
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    String token = tokenResult.getToken();
                    Log.d(TAG, "uploadMedia() called");
                    Log.d(TAG, "ReceiverId: " + receiverUser.id);
                    Log.d(TAG, "Uri: " + uri);
                    Log.d(TAG, "Token (first 50 chars): " + (token != null ? token.substring(0, Math.min(50, token.length())) : "null"));

                    File file;

                    if ("file".equals(uri.getScheme())) {
                        file = new File(uri.getPath());
                    } else {
                        file = FileUtils.uriToFile(this, uri);
                    }

                    if (file == null || !file.exists()) {
                        Log.e(TAG, "File is null or does not exist. Uri = " + uri);
                        runOnUiThread(() -> Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    Log.d(TAG, "File size: " + file.length() + " bytes");
                    Log.d(TAG, "File name: " + file.getName());
                    Log.d(TAG, "File path: " + file.getAbsolutePath());
                    String mime;
                    if ("file".equals(uri.getScheme())) {
                        mime = "image/jpeg";
                    } else {
                        mime = getContentResolver().getType(uri);
                    }

                    if (mime == null) {
                        mime = "application/octet-stream";
                    }

                    String originalName = getOriginalName(uri);
                    RequestBody fileBody = RequestBody.create(file, MediaType.parse(mime));
                    MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", originalName, fileBody);

                    // FIXED: Just pass the receiverId as a String directly
                    // No need for RequestBody.create() - Retrofit handles it!
                    String receiverId = receiverUser.id;

                    Log.d(TAG, "Request prepared. Making API call...");
                    Log.d(TAG, "Authorization: Bearer " + token.substring(0, Math.min(20, token.length())) + "...");
                    Log.d(TAG, "ReceiverId in request: " + receiverId);

                    mediaApi.uploadMedia("Bearer " + token, filePart, receiverId).enqueue(new Callback<MediaResponse>() {
                        @Override
                        public void onResponse(Call<MediaResponse> call, Response<MediaResponse> response) {
                            Log.d(TAG, "=== UPLOAD RESPONSE ===");
                            Log.d(TAG, "Response code: " + response.code());
                            Log.d(TAG, "Response message: " + response.message());
                            Log.d(TAG, "Request URL: " + call.request().url());

                            if (response.isSuccessful() && response.body() != null) {
                                Log.d(TAG, "✓ Upload SUCCESS");
                                Log.d(TAG, "Media URL: " + response.body().getMediaUrl());
                                Log.d(TAG, "Media Type: " + response.body().getMediaType());
                                Log.d(TAG, "Media ID: " + response.body().getMediaId());

                                runOnUiThread(() -> {
                                    saveMediaMessage(response.body());
                                    Toast.makeText(ChatActivity.this, "Media uploaded successfully!", Toast.LENGTH_SHORT).show();
                                });
                            } else {
                                Log.e(TAG, "✗ Upload FAILED");
                                Log.e(TAG, "HTTP Code: " + response.code());

                                try {
                                    if (response.errorBody() != null) {
                                        String error = response.errorBody().string();
                                        Log.e(TAG, "Error body:\n" + error);
                                    } else {
                                        Log.e(TAG, "Error body is null");
                                    }
                                } catch (Exception e) {
                                    Log.e(TAG, "Failed to read error body", e);
                                }

                                Log.e(TAG, "Headers: " + response.headers().toString());
                            }
                        }

                        @Override
                        public void onFailure(Call<MediaResponse> call, Throwable t) {
                            Log.e(TAG, "✗ Upload API FAILURE", t);
                            Log.e(TAG, "Failure message: " + t.getMessage());
                            Log.e(TAG, "Failure cause: " + (t.getCause() != null ? t.getCause().getMessage() : "none"));

                            runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show());
                        }
                    });

                } catch (Exception e) {
                    Log.e(TAG, "Exception in uploadMedia()", e);
                    e.printStackTrace();
                    runOnUiThread(() -> Toast.makeText(ChatActivity.this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to get Firebase ID token", e);
            Toast.makeText(this, "Authentication failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private String getOriginalName(@NonNull Uri uri) {
        String name = null;

        if ("content".equals(uri.getScheme())) {
            Cursor cursor = getContentResolver().query(
                    uri,
                    new String[]{OpenableColumns.DISPLAY_NAME},
                    null,
                    null,
                    null
            );
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    name = cursor.getString(0);
                }
                cursor.close();
            }
        }
        if (name == null) {
            name = uri.getLastPathSegment();
        }
        return name;
    }

//    private void saveMediaMessage(@NonNull MediaResponse media) {
//        HashMap<String, Object> message = new HashMap<>();
//        message.put(Constants.TYPE, Constants.MEDIA);
//        message.put(Constants.MEDIA_TYPE, media.getMediaType());
//        message.put(Constants.MEDIA_NAME, media.getMediaName());
//        message.put(Constants.MEDIA_URL, media.getMediaUrl());
//        message.put(Constants.KEY_SENDER_ID, FirebaseAuth.getInstance().getUid());
//        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
//        message.put(Constants.KEY_TIMESTAMP, new Date());
//        message.put(Constants.IS_READ, false);
//
//        FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CHATS).add(message).addOnSuccessListener(unused -> Toast.makeText(this, "Media Sent", Toast.LENGTH_SHORT).show());
//    }

    private void saveMediaMessage(@NonNull MediaResponse media) {

        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.TYPE, Constants.MEDIA);
        message.put(Constants.MEDIA_TYPE, media.getMediaType());
        message.put(Constants.MEDIA_NAME, media.getMediaName());
        message.put(Constants.MEDIA_URL, media.getMediaUrl());
        message.put(Constants.KEY_SENDER_ID, FirebaseAuth.getInstance().getUid());
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_TIMESTAMP, new Date());
        message.put(Constants.IS_READ, false);

        FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_CHATS)
                .add(message)
                .addOnSuccessListener(unused -> {

                    // ✅ 1️⃣ SEND MEDIA NOTIFICATION
                    String notificationText;

                    if (media.getMediaType().startsWith("image")) {
                        notificationText = "📷 Photo";
                    } else if (media.getMediaType().startsWith("video")) {
                        notificationText = "🎥 Video";
                    } else {
                        notificationText = "📄 File";
                    }

                    sendPushNotification(
                            receiverUser.token,
                            preferenceManager.getString(Constants.KEY_NAME),
                            notificationText
                    );

                    if (conversionId != null) {
                        updateConversion(notificationText);
                    } else {
                        HashMap<String, Object> conversion = new HashMap<>();
                        conversion.put(Constants.KEY_SENDER_ID,
                                preferenceManager.getString(Constants.KEY_USER_ID));
                        conversion.put(Constants.KEY_SENDER_NAME,
                                preferenceManager.getString(Constants.KEY_NAME));
                        conversion.put(Constants.KEY_SENDER_IMAGE,
                                preferenceManager.getString(Constants.KEY_IMAGE));
                        conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                        conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
                        conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
                        conversion.put(Constants.KEY_LAST_MESSAGE, notificationText);
                        conversion.put(Constants.KEY_TIMESTAMP, new Date());
                        conversion.put(Constants.IS_READ, false);

                        addConversion(conversion);
                    }

                    Toast.makeText(this, "Media Sent", Toast.LENGTH_SHORT).show();
                });
    }


    private void showAttachmentOptions() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_attachment, null);
        dialog.setContentView(view);

        view.findViewById(R.id.optionGallery).setOnClickListener(v -> {
            dialog.dismiss();
            openGallery();
        });

        view.findViewById(R.id.optionCamera).setOnClickListener(v -> {
            dialog.dismiss();
            openCamera();
        });
        view.findViewById(R.id.optionFile).setOnClickListener(v -> {
            dialog.dismiss();
            openFilePicker();
        });
        dialog.show();
    }

    private void openGallery() {
        if (hasMediaPermission()) {
            launchGallery();
        } else {
            requestMediaPermission(GALLERY_PERMISSION_REQUEST);
        }
    }

    private void openCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {

            launchCamera();

        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST);
        }
    }
    private void launchCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(intent);
    }
    private void launchGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/* video/*");
        galleryLauncher.launch(intent);
    }
    private void launchFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        });
        fileLauncher.launch(intent);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            return;
        }

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            launchCamera();
        } else if (requestCode == GALLERY_PERMISSION_REQUEST) {
            launchGallery();
        } else if (requestCode == FILE_PERMISSION_REQUEST) {
            launchFilePicker();
        }
    }
    private boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }
    }
    private void requestMediaPermission(int requestCode) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO}, requestCode);

        } else {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, requestCode);
        }
    }
    private void openFilePicker() {
        if (hasMediaPermission()) {
            launchFilePicker();
        } else {
            requestMediaPermission(FILE_PERMISSION_REQUEST);
        }
    }
    //    private void loadReceiverDetail() {
//
//        receiverUser = (Users) getIntent().getSerializableExtra(Constants.KEY_USER);
//
//        // -------- CASE 1: OPENED NORMALLY --------
//        if (receiverUser != null) {
//            binding.txtName.setText(receiverUser.name);
//            return;
//        }
//
//        // -------- CASE 2: OPENED FROM NOTIFICATION --------
//        String id = getIntent().getStringExtra("senderId");
//        if (id == null) {
//            Log.d("CHAT_DEBUG", "senderId NULL → NOT finishing (debug)");
//            return;
//        }
//        Log.d("CHAT_DEBUG", "SenderId: " + id);
//
//        FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_USERS).document(id).get().addOnSuccessListener(doc -> {
//
//            receiverUser = new Users();
//            receiverUser.id = id;
//            receiverUser.name = doc.getString(Constants.KEY_NAME);
//            receiverUser.image = doc.getString(Constants.KEY_IMAGE);
//            receiverUser.token = doc.getString(Constants.KEY_FCM_TOKEN);
//
//            binding.txtName.setText(receiverUser.name);
//
//            // 🔥 Update ChatAdapter with real photo
//            if (chatAdapter != null) {
//                chatAdapter.setReceiverProfileImage(getBitmapEncodedString(receiverUser.image));
//                chatAdapter.notifyDataSetChanged();
//            }
//        });
//    }
    private void loadReceiverDetail() {

        // Case 1: opened from UsersActivity
        receiverUser = (Users) getIntent().getSerializableExtra(Constants.KEY_USER);
        if (receiverUser != null) {
            Log.d("CHAT_DEBUG", "Opened from UsersActivity");
            binding.txtName.setText(receiverUser.name);

            init();
            listenMessage();
            return;
        }

        // Case 2: opened from Notification
        String id = getIntent().getStringExtra("senderId");
        Log.d("CHAT_DEBUG", "Opened from Notification | senderId = " + id);

        if (id == null) return;

        receiverUser = new Users();
        receiverUser.id = id;
        binding.txtName.setText("Chat");

        FirebaseFirestore.getInstance()
                .collection(Constants.KEY_COLLECTION_USERS)
                .document(id)
                .get()
                .addOnSuccessListener(doc -> {

                    receiverUser.name = doc.getString(Constants.KEY_NAME);
                    receiverUser.image = doc.getString(Constants.KEY_IMAGE);
                    receiverUser.token = doc.getString(Constants.KEY_FCM_TOKEN);

                    binding.txtName.setText(receiverUser.name);

                    init();          // ✅ NOW SAFE
                    listenMessage(); // ✅ NOW SAFE
                });
    }

    //    private void init() {
//        preferenceManager = new PreferenceManager(getApplicationContext());
//        chatMessages = new ArrayList<>();
//        chatAdapter = new ChatAdapter(chatMessages, preferenceManager.getString(Constants.KEY_USER_ID), getBitmapEncodedString(receiverUser.image));
//        mediaApi = MediaApiClient.getClient().create(MediaApi.class);
//        binding.chatRecyclerView.setAdapter(chatAdapter);
//        database = FirebaseFirestore.getInstance();
//    }
    private void init() {
        if (receiverUser == null || receiverUser.id == null) {
            Log.d("CHAT_DEBUG", "init aborted: receiverUser null");
            return;
        }

        preferenceManager = new PreferenceManager(getApplicationContext());
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages,
                preferenceManager.getString(Constants.KEY_USER_ID),
                getBitmapEncodedString(receiverUser.image)
        );
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private Bitmap getBitmapEncodedString(@Nullable String encodedImage) {
        if (encodedImage != null && !encodedImage.isEmpty()) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } else {
            return BitmapFactory.decodeResource(getResources(), R.drawable.default_profile);
        }
    }

    private void listenMessage() {
        // Listen for messages sent and received
        database.collection(Constants.KEY_COLLECTION_CHAT).whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID)).whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id).addSnapshotListener(eventListener);

        database.collection(Constants.KEY_COLLECTION_CHAT).whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id).whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID)).addSnapshotListener(eventListener);

        // Listen for media

        database.collection(Constants.KEY_COLLECTION_CHATS).whereEqualTo(Constants.KEY_SENDER_ID,
                preferenceManager.getString(Constants.KEY_USER_ID)).whereEqualTo(Constants.KEY_RECEIVER_ID,
                receiverUser.id).addSnapshotListener(mediaEventListener);

        database.collection(Constants.KEY_COLLECTION_CHATS)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager
                        .getString(Constants.KEY_USER_ID)).addSnapshotListener(mediaEventListener);


    }
    private void sendMessage() {
        try {
            SecretKey key = EncryptionUtil.generateKey("your_secure_password");
            String message = binding.inputMessage.getText().toString();
            if (message.isEmpty()) return;
            String encryptedMessage = EncryptionUtil.encrypt(message, key);

            HashMap<String, Object> messageMap = new HashMap<>();
            messageMap.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            messageMap.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            messageMap.put(Constants.KEY_MESSAGE, encryptedMessage);
            messageMap.put(Constants.KEY_TIMESTAMP, new Date());
            messageMap.put(Constants.IS_READ, false);
            database.collection(Constants.KEY_COLLECTION_CHAT).add(messageMap).addOnSuccessListener(documentReference -> {
                sendPushNotification(receiverUser.token, preferenceManager.getString(Constants.KEY_NAME), message);
            });

            // Handle conversation update
            if (conversionId != null) {
                updateConversion(encryptedMessage);
            } else {
                HashMap<String, Object> conversion = new HashMap<>();
                conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
                conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
                conversion.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
                conversion.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
                conversion.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
                conversion.put(Constants.KEY_LAST_MESSAGE, encryptedMessage);
                conversion.put(Constants.KEY_TIMESTAMP, new Date());
                addConversion(conversion);
            }
            binding.inputMessage.setText(null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void listenAvailabilityOfReceiver() {
        database.collection(Constants.KEY_COLLECTION_USERS).document(receiverUser.id).addSnapshotListener(ChatActivity.this, ((value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABILITY)).intValue();
                    isReceiverAvailable = availability == 1;
                }
                receiverUser.token = value.getString(Constants.KEY_FCM_TOKEN);
                if (receiverUser.image != null) {
                    receiverUser.image = value.getString(Constants.KEY_IMAGE);
                    chatAdapter.setReceiverProfileImage(getBitmapEncodedString(receiverUser.image));
                    chatAdapter.notifyItemRangeChanged(0, chatMessages.size());
                }
            }
            if (isReceiverAvailable) {
                binding.txtAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.txtAvailability.setVisibility(View.GONE);
            }
        }));
    }
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                ChatMessages chatMessage = new ChatMessages();
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    try {
                        SecretKey key = EncryptionUtil.generateKey("your_secure_password");
                        String encryptedMessage = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                        String decryptedMessage = EncryptionUtil.decrypt(encryptedMessage, key);

                        chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                        chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                        chatMessage.message = decryptedMessage;
                        chatMessage.dateTime = getReadableDate(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                        chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                        chatMessage.isRead =
                                documentChange.getDocument().getBoolean(Constants.IS_READ) != null && documentChange.getDocument().getBoolean(Constants.IS_READ);
                        chatMessages.add(chatMessage);


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            Collections.sort(chatMessages, (obj1, obj2) -> obj1.dateObject.compareTo(obj2.dateObject));
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
                scrollToBottomSafe();
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.smoothScrollToPosition(chatMessages.size() - 1);
            }
            binding.chatRecyclerView.setVisibility(View.VISIBLE);
        }
        binding.progressBar.setVisibility(View.GONE);
        if (conversionId == null) {
            checkForConversion();
        }
    };
    private final EventListener<QuerySnapshot> mediaEventListener = (value, error) -> {
        if (error != null) {
            Log.e(TAG, "Media listener error", error);
            return;
        }

        if (value == null) return;

        for (DocumentChange dc : value.getDocumentChanges()) {

            if (dc.getType() == DocumentChange.Type.ADDED) {

                ChatMessages mediaMsg = new ChatMessages();

                mediaMsg.type = "media";
                mediaMsg.senderId = dc.getDocument().getString(Constants.KEY_SENDER_ID);
                mediaMsg.receiverId = dc.getDocument().getString(Constants.KEY_RECEIVER_ID);
                mediaMsg.mediaUrl = dc.getDocument().getString(Constants.MEDIA_URL);
                mediaMsg.mediaType = dc.getDocument().getString(Constants.MEDIA_TYPE);
                mediaMsg.mediaName = dc.getDocument().getString(Constants.MEDIA_NAME);
                mediaMsg.dateObject = dc.getDocument().getDate(Constants.KEY_TIMESTAMP);
                mediaMsg.dateTime = getReadableDate(mediaMsg.dateObject);
                mediaMsg.isRead =
                        dc.getDocument().getBoolean(Constants.IS_READ) != null && dc.getDocument().getBoolean(Constants.IS_READ);

                chatMessages.add(mediaMsg);

                Log.d(TAG, "===== MEDIA RECEIVED =====");
                Log.d(TAG, "URL  : " + mediaMsg.mediaUrl);
                Log.d(TAG, "TYPE : " + mediaMsg.mediaType);
            }
        }

        Collections.sort(chatMessages, (a, b) -> a.dateObject.compareTo(b.dateObject));
        chatAdapter.notifyDataSetChanged();
        scrollToBottomSafe();
    };
    private void scrollToBottomSafe() {
        if (chatAdapter != null && chatAdapter.getItemCount() > 0) {
            binding.chatRecyclerView.post(() ->
                    binding.chatRecyclerView.scrollToPosition(
                            chatAdapter.getItemCount() - 1
                    )
            );
        }
    }
    private void setListener() {
        binding.imgBack.setOnClickListener(view -> getOnBackPressedDispatcher().onBackPressed());
        binding.btnSend.setOnClickListener(view -> sendMessage());
        binding.btnAdd.setOnClickListener(view -> showAttachmentOptions());
    }
    private void addConversion(HashMap<String, Object> conversion) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).add(conversion).addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }
    private void updateConversion(String message) {
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        documentReference.update(Constants.KEY_LAST_MESSAGE, message, Constants.KEY_TIMESTAMP, new Date());
    }
    private void checkForConversion() {
        if (chatMessages.size() != 0) {
            checkConversionRemotely(preferenceManager.getString(Constants.KEY_USER_ID), receiverUser.id);
            checkConversionRemotely(receiverUser.id, preferenceManager.getString(Constants.KEY_USER_ID));
        }
    }
    private void checkConversionRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.KEY_SENDER_ID, senderId).whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId).get().addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            conversionId = documentSnapshot.getId();
            Log.d("MEDIA_DEBUG","ConversionId"+conversionId);
        }
    };
    @NonNull
    private String getReadableDate(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }
    private void sendPushNotification(String receiverToken, String title, String bodyText) {
        NotificationApi api = ApiClient.getClient().create(NotificationApi.class);
        Map<String, Object> data = new HashMap<>();
        data.put("senderId", preferenceManager.getString(Constants.KEY_USER_ID));
        data.put("type", "chat");

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("receiverToken", receiverToken);
        requestBody.put("title", title);
        requestBody.put("body", bodyText);
        requestBody.put("data", data);

        api.sendNotification("mySuperSecretKey12345", requestBody).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                Log.d("FCM", "Notification API Returned: " + response.code() + " " + response.body());
                Log.d("FCM", "Reciever's FCM Token: " + receiverToken);
                Log.d("FCM", "Body : " + bodyText);
                Log.d("FCM", "Data: " + data);

            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Log.e("FCM", "Failure: " + t.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CHAT).whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                        .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
                                .whereEqualTo(Constants.IS_READ,false)
                                        .get()
                                                .addOnSuccessListener(query->{
                                                   for (DocumentSnapshot doc:query.getDocuments()){
                                                       doc.getReference().update(Constants.IS_READ,true);
                                                   }
                                                });
        FirebaseFirestore.getInstance().collection(Constants.KEY_COLLECTION_CHATS).whereEqualTo(Constants.KEY_RECEIVER_ID,preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_SENDER_ID,receiverUser.id)
                .whereEqualTo(Constants.IS_READ,false)
                .get()
                .addOnSuccessListener(query->{
                    for (DocumentSnapshot doc:query.getDocuments()){
                        doc.getReference().update(Constants.IS_READ,true);
                    }
                });
        listenAvailabilityOfReceiver();
        Log.d("CHAT_DEBUG", "ChatActivity is VISIBLE ✅");

    }
}
