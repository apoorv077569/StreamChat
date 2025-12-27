package com.example.streamchat.adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.example.streamchat.activities.ImageViewerActivity;
import com.example.streamchat.activities.PdfViewerActivity;
import com.example.streamchat.databinding.LayoutSentMessageBinding;
import com.example.streamchat.databinding.ReceivedMessagesBinding;
import com.example.streamchat.modals.ChatMessages;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public ChatAdapter(List<ChatMessages> chatMessages, String senderId, Bitmap receiverProfileImage) {
        this.chatMessages = chatMessages;
        this.senderId = senderId;
        this.receiverProfileImage = receiverProfileImage;
    }

    private final List<ChatMessages> chatMessages;
    private final String senderId;
    private Bitmap receiverProfileImage;

    public final int VIEW_TYPE_SENT = 1;
    public final int VIEW_TYPE_RECEIVED = 2;
    private static final int MEDIA_SENDER = 1;
    private static final int MEDIA_RECEIVER = 2;


    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SENT) {
            return new SentMessageViewHolder(
                    LayoutSentMessageBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        } else {
            return new ReceiverMessageViewHolder(
                    ReceivedMessagesBinding.inflate(
                            LayoutInflater.from(parent.getContext()),
                            parent,
                            false
                    )
            );
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (chatMessages.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SENT;
        } else {
            return VIEW_TYPE_RECEIVED;
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SENT) {
            ((SentMessageViewHolder) holder).setData(chatMessages.get(position));
        } else {
            ((ReceiverMessageViewHolder) holder).setData(chatMessages.get(position), receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        return chatMessages.size();
    }


    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final LayoutSentMessageBinding binding;

        SentMessageViewHolder(LayoutSentMessageBinding messageBinding) {
            super(messageBinding.getRoot());
            binding = messageBinding;

        }
        void setData(@NonNull ChatMessages chatMessages) {

            // RESET VISIBILITY (VERY IMPORTANT)
            binding.txtMessage.setVisibility(View.GONE);
            binding.imgMedia.setVisibility(View.GONE);
            binding.layoutFile.setVisibility(View.GONE);
            binding.progressMessage.setVisibility(View.VISIBLE);

            Log.d("MEDIA_DEBUG", "----- setData() called -----");
            Log.d("MEDIA_DEBUG", "Type      : " + chatMessages.type);
            Log.d("MEDIA_DEBUG", "MediaType : " + chatMessages.mediaType);
            Log.d("MEDIA_DEBUG", "MediaName : " + chatMessages.mediaName);
            Log.d("MEDIA_DEBUG", "MediaUrl  : " + chatMessages.mediaUrl);

            // =========================
            // MEDIA MESSAGE
            // =========================
            if ("media".equals(chatMessages.type)) {

                // IMAGE
                if (chatMessages.mediaType != null &&
                        chatMessages.mediaType.startsWith("image")) {

                    binding.imgMedia.setVisibility(View.VISIBLE);

                    Log.d("MEDIA_DEBUG", "Loading IMAGE via Glide");

                    Glide.with(binding.getRoot().getContext())
                            .load(chatMessages.mediaUrl)
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(
                                        @Nullable GlideException e,
                                        Object model,
                                        Target<Drawable> target,
                                        boolean isFirstResource) {

                                    Log.e("MEDIA_DEBUG", "❌ IMAGE LOAD FAILED");
                                    Log.e("MEDIA_DEBUG", "Model: " + model);

                                    if (e != null) {
                                        Log.e("MEDIA_DEBUG", "Exception:", e);
                                    }
                                    binding.progressMessage.setVisibility(View.GONE);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(
                                        Drawable resource,
                                        Object model,
                                        Target<Drawable> target,
                                        DataSource dataSource,
                                        boolean isFirstResource) {

                                    Log.d("MEDIA_DEBUG", "✅ IMAGE LOAD SUCCESS");
                                    Log.d("MEDIA_DEBUG", "Source: " + dataSource);
                                    binding.progressMessage.setVisibility(View.GONE);
                                    return false;
                                }
                            })
                            .into(binding.imgMedia);
                    binding.imgMedia.setOnClickListener( v-> {
                        Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
                        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL,chatMessages.mediaUrl);
                        v.getContext().startActivity(intent);
                    });
                }
                else {
                    binding.layoutFile.setVisibility(View.VISIBLE);
                    binding.txtFileName.setText(chatMessages.mediaName);
                    Log.d("MEDIA_DEBUG", "Showing FILE layout");
                    binding.progressMessage.setVisibility(View.GONE);
                    binding.layoutFile.setOnClickListener(v -> {
                        Context context = v.getContext();
                        Intent intent;

                        if (chatMessages.mediaType != null &&
                                chatMessages.mediaType.equals("application/pdf")) {
                            // Open PDF in your custom viewer
                            intent = new Intent(context, PdfViewerActivity.class);
                            intent.putExtra(
                                    PdfViewerActivity.EXTRA_PDF_URL,
                                    chatMessages.mediaUrl
                            );
                        }
                        else {
                            // Open other files in default app
                            intent = new Intent(Intent.ACTION_VIEW);
                            intent.setDataAndType(Uri.parse(chatMessages.mediaUrl), chatMessages.mediaType);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                            // Check if there's an app that can handle this file type
                            if (intent.resolveActivity(context.getPackageManager()) == null) {
                                // No app found, show error
                                Toast.makeText(context, "No app found to open this file", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }

                        try {
                            context.startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(context, "Unable to open file", Toast.LENGTH_SHORT).show();
                            e.printStackTrace();
                        }
                    });
                }

            }
            // =========================
            // TEXT MESSAGE
            // =========================
            else {

                binding.txtMessage.setVisibility(View.VISIBLE);
                binding.txtMessage.setText(chatMessages.message);

                Log.d("MEDIA_DEBUG", "Showing TEXT message");
                binding.progressMessage.setVisibility(View.GONE);
            }

            // DATE TIME (ALWAYS AT BOTTOM)
            binding.txtDateTime.setText(chatMessages.dateTime);
        }
    }

    static class ReceiverMessageViewHolder extends RecyclerView.ViewHolder {
        private final ReceivedMessagesBinding binding;

        ReceiverMessageViewHolder(ReceivedMessagesBinding receivedMessagesBinding) {
            super(receivedMessagesBinding.getRoot());
            binding = receivedMessagesBinding;
        }
        void setData(@NonNull ChatMessages chatMessages, Bitmap receiverProfileImage) {
            binding.txtMessage.setVisibility(View.INVISIBLE);
            binding.imgMedia.setVisibility(View.GONE);

            if (chatMessages.type==null || !"media".equals(chatMessages.type)){
                binding.txtMessage.setVisibility(View.VISIBLE);
                binding.txtMessage.setText(chatMessages.message);
                binding.txtDateTime.setText(chatMessages.dateTime);
                if (!chatMessages.isRead){
                    binding.txtMessage.setTypeface(null, Typeface.BOLD);
                }else{
                    binding.txtMessage.setTypeface(null,Typeface.NORMAL);
                }
            }else{
                if (chatMessages.mediaType != null && chatMessages.mediaType.startsWith("image")){
                    binding.imgMedia.setVisibility(View.VISIBLE);

                    Glide.with(binding.getRoot().getContext())
                            .load(chatMessages.mediaUrl)
                            .into(binding.imgMedia);
                }else{
                    binding.txtMessage.setVisibility(View.VISIBLE);
                    binding.txtMessage.setText(chatMessages.mediaName);

                    binding.txtMessage.setOnClickListener( v->{
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(chatMessages.mediaUrl));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        binding.getRoot().getContext().startActivity(intent);
                    });
                    binding.imgMedia.setOnClickListener( v-> {
                        Intent intent = new Intent(v.getContext(), ImageViewerActivity.class);
                        intent.putExtra(ImageViewerActivity.EXTRA_IMAGE_URL,chatMessages.mediaUrl);
                        v.getContext().startActivity(intent);
                    });

                }
                binding.txtDateTime.setText(chatMessages.dateTime);
            }
            if (receiverProfileImage != null && binding.imgProfile != null){
                binding.imgProfile.setImageBitmap(receiverProfileImage);
            }
        }
    }
}


