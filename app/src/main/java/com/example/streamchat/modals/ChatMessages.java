package com.example.streamchat.modals;

import java.util.Date;

public class ChatMessages {
    public String senderId, receiverId, message, dateTime,mediaName;
    public Date dateObject;
    public String conversionId, conversionName, ConversionImage;
    public String type,mediaUrl,mediaType;
    public boolean isRead;

    public String getReceiverId() {
        return this.receiverId;
    }

    public void setReceiverId(final String receiverId) {
        this.receiverId = receiverId;
    }

    public String getSenderId() {
        return this.senderId;
    }

    public void setSenderId(final String senderId) {
        this.senderId = senderId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getDateTime() {
        return this.dateTime;
    }

    public void setDateTime(final String dateTime) {
        this.dateTime = dateTime;
    }

    public String getMediaName() {
        return this.mediaName;
    }

    public void setMediaName(final String mediaName) {
        this.mediaName = mediaName;
    }

    public Date getDateObject() {
        return this.dateObject;
    }

    public void setDateObject(final Date dateObject) {
        this.dateObject = dateObject;
    }

    public String getConversionId() {
        return this.conversionId;
    }

    public void setConversionId(final String conversionId) {
        this.conversionId = conversionId;
    }

    public String getConversionName() {
        return this.conversionName;
    }

    public void setConversionName(final String conversionName) {
        this.conversionName = conversionName;
    }

    public String getConversionImage() {
        return this.ConversionImage;
    }

    public void setConversionImage(final String conversionImage) {
        this.ConversionImage = conversionImage;
    }

    public String getType() {
        return this.type;
    }

    public void setType(final String type) {
        this.type = type;
    }

    public String getMediaUrl() {
        return this.mediaUrl;
    }

    public void setMediaUrl(final String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getMediaType() {
        return this.mediaType;
    }

    public void setMediaType(final String mediaType) {
        this.mediaType = mediaType;
    }

    public boolean isRead() {
        return this.isRead;
    }

    public void setRead(final boolean read) {
        this.isRead = read;
    }
}
