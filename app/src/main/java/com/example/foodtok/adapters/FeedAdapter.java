package com.example.foodtok.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.example.foodtok.R;
import com.example.foodtok.models.Recipe;

import java.util.List;

/**
 * Adapter for the outer vertical ViewPager2.
 * Each page is a horizontal ViewPager2 containing 3 sub-pages:
 *   [0] Ingredients  |  [1] Video (center, default)  |  [2] Chat
 */
public class FeedAdapter extends RecyclerView.Adapter<FeedAdapter.FeedPageViewHolder> {

    /**
     * Callback fired whenever the active horizontal sub-page changes.
     * The adapterPosition lets the receiver ignore events from recycled off-screen items.
     */
    public interface OnHorizontalPageChangedListener {
        void onPageChanged(int adapterPosition, int horizontalPage);
    }

    private final List<Recipe> recipes;
    private ViewPager2 parentVerticalPager;
    private OnHorizontalPageChangedListener horizontalPageListener;

    public FeedAdapter(List<Recipe> recipes) {
        this.recipes = recipes;
    }

    /** Call this from HomeFragment so vertical scrolling can be locked on side pages. */
    public void setParentVerticalPager(ViewPager2 pager) {
        this.parentVerticalPager = pager;
    }

    /** Call this from HomeFragment to receive horizontal page change events. */
    public void setOnHorizontalPageChangedListener(OnHorizontalPageChangedListener listener) {
        this.horizontalPageListener = listener;
    }

    /**
     * Navigates the currently visible recipe's horizontal pager to the given page.
     * Safe to call from click handlers in HomeFragment.
     */
    public void navigateCurrentPageTo(int page) {
        if (parentVerticalPager == null) return;
        RecyclerView rv = (RecyclerView) parentVerticalPager.getChildAt(0);
        if (rv == null) return;
        int currentPos = parentVerticalPager.getCurrentItem();
        FeedPageViewHolder vh = (FeedPageViewHolder) rv.findViewHolderForAdapterPosition(currentPos);
        if (vh != null) {
            vh.horizontalPager.setCurrentItem(page, true);
        }
    }

    @NonNull
    @Override
    public FeedPageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_feed_page, parent, false);
        return new FeedPageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FeedPageViewHolder holder, int position) {
        Recipe recipe = recipes.get(position);
        holder.bind(recipe, position, parentVerticalPager, horizontalPageListener);
    }

    @Override
    public int getItemCount() {
        return recipes.size();
    }

    static class FeedPageViewHolder extends RecyclerView.ViewHolder {

        final ViewPager2 horizontalPager;
        private ViewPager2.OnPageChangeCallback pageChangeCallback;

        FeedPageViewHolder(@NonNull View itemView) {
            super(itemView);
            horizontalPager = itemView.findViewById(R.id.recipeHorizontalPager);
        }

        void bind(Recipe recipe, int adapterPosition,
                  ViewPager2 parentVerticalPager,
                  OnHorizontalPageChangedListener listener) {

            RecipePageAdapter pageAdapter = new RecipePageAdapter(recipe);
            horizontalPager.setAdapter(pageAdapter);

            // Always reset to video page (center) when binding.
            // Prevents recycled views from retaining the previous recipe's page.
            horizontalPager.setCurrentItem(1, false);

            // Unregister previous callback to avoid accumulating stale listeners
            // on recycled ViewHolders.
            if (pageChangeCallback != null) {
                horizontalPager.unregisterOnPageChangeCallback(pageChangeCallback);
            }

            pageChangeCallback = new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int page) {
                    // Lock/unlock vertical feed scrolling: only allow it on the video page.
                    if (parentVerticalPager != null) {
                        parentVerticalPager.setUserInputEnabled(page == 1);
                    }
                    // Notify the fragment, including our adapter position so the
                    // fragment can ignore events from off-screen recycled items.
                    if (listener != null) {
                        listener.onPageChanged(adapterPosition, page);
                    }
                }
            };
            horizontalPager.registerOnPageChangeCallback(pageChangeCallback);
        }
    }
}