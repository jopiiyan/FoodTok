package com.example.foodtok.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodtok.R;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.util.VideoThumbnailLoader;

import java.util.List;

public class ProfileRecipeAdapter extends RecyclerView.Adapter<ProfileRecipeAdapter.ViewHolder> {

    public interface OnRecipeClickListener {
        void onRecipeClick(int position);
    }

    private List<RecipeDto> recipes;
    private OnRecipeClickListener clickListener;

    public ProfileRecipeAdapter(List<RecipeDto> recipes) {
        this.recipes = recipes;
    }

    public void setOnRecipeClickListener(OnRecipeClickListener listener) {
        this.clickListener = listener;
    }

    public void updateData(List<RecipeDto> newRecipes) {
        this.recipes = newRecipes;
        notifyDataSetChanged();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_profile_recipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecipeDto recipe = recipes.get(position);

        if (!TextUtils.isEmpty(recipe.thumbnailUrl)) {
            Glide.with(holder.itemView.getContext())
                    .load(recipe.thumbnailUrl)
                    .centerCrop()
                    .into(holder.ivRecipeThumb);
        } else {
            VideoThumbnailLoader.load(recipe.videoUrl, holder.ivRecipeThumb);
        }

        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null) {
                int adapterPosition = holder.getAdapterPosition();
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    clickListener.onRecipeClick(adapterPosition);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivRecipeThumb;

        ViewHolder(View view) {
            super(view);
            ivRecipeThumb = view.findViewById(R.id.ivRecipeThumb);
        }
    }
}