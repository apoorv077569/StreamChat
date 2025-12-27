/*
package com.example.streamchat.adapter;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.streamchat.databinding.ItemPdfPageBinding;

import java.util.List;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageVH> {

    private final List<Bitmap> pages;

    public PdfPageAdapter(List<Bitmap> pages) {
        this.pages = pages;
    }

    @NonNull
    @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PageVH(
                ItemPdfPageBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH holder, int position) {
        holder.binding.imgPdfPage.setImageBitmap(pages.get(position));
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    static class PageVH extends RecyclerView.ViewHolder {
        ItemPdfPageBinding binding;

        PageVH(ItemPdfPageBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }
}
 */

package com.example.streamchat.adapter;

import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.streamchat.databinding.ItemPdfPageBinding;

public class PdfPageAdapter extends RecyclerView.Adapter<PdfPageAdapter.PageVH> {

    private static final String TAG = "PdfPageAdapter";
    private final PdfRenderer pdfRenderer;
    private final int pageCount;

    public PdfPageAdapter(PdfRenderer pdfRenderer, int pageCount) {
        this.pdfRenderer = pdfRenderer;
        this.pageCount = pageCount;
    }

    @NonNull
    @Override
    public PageVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PageVH(
                ItemPdfPageBinding.inflate(
                        LayoutInflater.from(parent.getContext()),
                        parent,
                        false
                )
        );
    }

    @Override
    public void onBindViewHolder(@NonNull PageVH holder, int position) {
        long startTime = System.currentTimeMillis();

        try {
            // Render page on-demand
            PdfRenderer.Page page = pdfRenderer.openPage(position);

            // Scale down for better performance (adjust multiplier as needed)
            int width = page.getWidth() * 2; // 2x resolution
            int height = page.getHeight() * 2;

            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

            holder.binding.imgPdfPage.setImageBitmap(bitmap);
            page.close();

            long renderTime = System.currentTimeMillis() - startTime;
            Log.d(TAG, "Page " + (position + 1) + " rendered in " + renderTime + " ms");

        } catch (Exception e) {
            Log.e(TAG, "Error rendering page " + position, e);
        }
    }

    @Override
    public int getItemCount() {
        return pageCount;
    }

    static class PageVH extends RecyclerView.ViewHolder {
        ItemPdfPageBinding binding;

        PageVH(ItemPdfPageBinding b) {
            super(b.getRoot());
            binding = b;
        }
    }
}
