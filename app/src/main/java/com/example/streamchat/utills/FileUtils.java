package com.example.streamchat.utills;


import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class FileUtils {

    public static File uriToFile(Context context, Uri uri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File file = new File(
                    context.getCacheDir(),
                    "upload_" + System.currentTimeMillis()
            );

            FileOutputStream outputStream = new FileOutputStream(file);

            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }

            outputStream.flush();
            outputStream.close();
            inputStream.close();

            return file;

        } catch (Exception e) {
            Log.e("FileUtils", "uriToFile failed for uri: " + uri, e);
            return null;
        }
    }
}

