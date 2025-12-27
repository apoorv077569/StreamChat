/*
package com.example.streamchat.activities;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.streamchat.adapter.PdfPageAdapter;
import com.example.streamchat.databinding.ActivityPdfViewerBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class PdfViewerActivity extends AppCompatActivity {

    public static final String TAG = "PdfViewerActivity";
    public static final String EXTRA_PDF_URL = "pdf_url";

    private ActivityPdfViewerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG,"onCreate: PdfViewerActivity started");

        binding.pdfRecyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        String pdfUrl = getIntent().getStringExtra(EXTRA_PDF_URL);
        Log.d(TAG,"PDF URL = "+pdfUrl);
        if (pdfUrl != null) {
            downloadAndRenderPdf(pdfUrl);
        }else{
            Log.e(TAG,"PDF url is null");
        }
    }

    // ===============================
    // DOWNLOAD → RENDER PDF
    // ===============================
    private void downloadAndRenderPdf(String url) {
        Log.d(TAG,"Starting download pdf");
        runOnUiThread(() -> binding.progressBar.setVisibility(View.VISIBLE));
        new Thread(() -> {
            try {
                Log.d(TAG,"Downloading.....");
                // 1️⃣ Download PDF
                File pdfFile = new File(getCacheDir(), "temp.pdf");
                Log.d(TAG,"Downloading to :"+pdfFile.getAbsolutePath());
                InputStream inputStream = new URL(url).openStream();
                FileOutputStream outputStream = new FileOutputStream(pdfFile);

                byte[] buffer = new byte[4096];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }

                inputStream.close();
                outputStream.close();

                // 2️⃣ Open PdfRenderer
                ParcelFileDescriptor pfd =
                        ParcelFileDescriptor.open(pdfFile,
                                ParcelFileDescriptor.MODE_READ_ONLY);

                PdfRenderer renderer = new PdfRenderer(pfd);

                List<Bitmap> pages = new ArrayList<>();

                // 3️⃣ Render each page
                for (int i = 0; i < renderer.getPageCount(); i++) {
                    PdfRenderer.Page page = renderer.openPage(i);

                    Bitmap bitmap = Bitmap.createBitmap(
                            page.getWidth(),
                            page.getHeight(),
                            Bitmap.Config.ARGB_8888
                    );

                    page.render(bitmap, null, null,
                            PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    pages.add(bitmap);
                    page.close();
                }

                renderer.close();
                pfd.close();
                Log.d(TAG, "downloadAndRenderPdf: All pages rendered successfully!");


                // 4️⃣ Show in UI
                runOnUiThread(() -> {
                            binding.progressBar.setVisibility(View.GONE);
                            binding.pdfRecyclerView.setAdapter(new PdfPageAdapter(pages));
                            Log.d(TAG, "downloadAndRenderPdf: PDF loaded successfully!");
                }
                );

            } catch (Exception e) {
                Log.e(TAG, "downloadAndRenderPdf: ERROR - " + e.getMessage(), e);
                binding.progressBar.setVisibility(View.GONE);
                Toast.makeText(this,"Failed to load due to: "+e.getMessage(),Toast.LENGTH_SHORT).show();
            }
        }).start();
    }
}
 */

package com.example.streamchat.activities;

import android.graphics.pdf.PdfRenderer;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.streamchat.adapter.PdfPageAdapter;
import com.example.streamchat.databinding.ActivityPdfViewerBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;

public class PdfViewerActivity extends AppCompatActivity {

    private static final String TAG = "PdfViewerActivity";
    public static final String EXTRA_PDF_URL = "pdf_url";

    private ActivityPdfViewerBinding binding;
    private PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPdfViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        Log.d(TAG, "onCreate: PdfViewerActivity started");

        binding.pdfRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        String pdfUrl = getIntent().getStringExtra(EXTRA_PDF_URL);
        Log.d(TAG, "onCreate: PDF URL = " + pdfUrl);

        if (pdfUrl != null) {
            downloadAndSetupPdf(pdfUrl);
        } else {
            Log.e(TAG, "onCreate: PDF URL is null!");
        }
    }

    private void downloadAndSetupPdf(String url) {
        Log.d(TAG, "downloadAndSetupPdf: Starting download...");
        binding.progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                long startTime = System.currentTimeMillis();

                // Download PDF
                File pdfFile = new File(getCacheDir(), "temp.pdf");
                Log.d(TAG, "Downloading PDF...");

                InputStream inputStream = new URL(url).openStream();
                FileOutputStream outputStream = new FileOutputStream(pdfFile);

                byte[] buffer = new byte[8192]; // Increased buffer size
                int read;
                long totalBytes = 0;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                    totalBytes += read;
                }

                inputStream.close();
                outputStream.close();

                long downloadTime = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Download complete! Size: " + (totalBytes / 1024) + " KB, Time: " + downloadTime + " ms");

                // Open PDF Renderer (DON'T render pages yet)
                parcelFileDescriptor = ParcelFileDescriptor.open(
                        pdfFile,
                        ParcelFileDescriptor.MODE_READ_ONLY
                );
                pdfRenderer = new PdfRenderer(parcelFileDescriptor);

                int pageCount = pdfRenderer.getPageCount();
                Log.d(TAG, "PDF opened with " + pageCount + " pages");

                // Setup adapter with lazy loading
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.pdfRecyclerView.setAdapter(
                            new PdfPageAdapter(pdfRenderer, pageCount)
                    );
                    Log.d(TAG, "PDF ready! Pages will load as you scroll.");
                    Toast.makeText(this, "PDF loaded: " + pageCount + " pages", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                Log.e(TAG, "Error loading PDF: " + e.getMessage(), e);
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load PDF", Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (pdfRenderer != null) {
                pdfRenderer.close();
            }
            if (parcelFileDescriptor != null) {
                parcelFileDescriptor.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error closing PDF renderer", e);
        }
    }
}