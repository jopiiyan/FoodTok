package com.example.foodtok.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.foodtok.R;
import com.example.foodtok.models.Recipe;

import java.util.List;

/** Adapter for the 2-column recipe thumbnail grid in GridFragment. */
public class GridAdapter extends RecyclerView.Adapter<GridAdapter.ViewHolder> {

  public interface OnItemClickListener {
    void onItemClick(int position);
  }

  private final List<Recipe> recipes;
  private final OnItemClickListener listener;

  public GridAdapter(List<Recipe> recipes, OnItemClickListener listener) {
    this.recipes = recipes;
    this.listener = listener;
  }

  @NonNull
  @Override
  public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.item_grid_recipe, parent, false);
    return new ViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Recipe recipe = recipes.get(position);
    Glide.with(holder.ivRecipeThumb)
        .load(recipe.getThumbnailUrl())
        .centerCrop()
        .into(holder.ivRecipeThumb);
    holder.itemView.setOnClickListener(v -> listener.onItemClick(position));
  }

  @Override
  public int getItemCount() {
    return recipes.size();
  }

  static class ViewHolder extends RecyclerView.ViewHolder {
    final ImageView ivRecipeThumb;

    ViewHolder(@NonNull View itemView) {
      super(itemView);
      ivRecipeThumb = itemView.findViewById(R.id.ivRecipeThumb);
    }
  }
}
