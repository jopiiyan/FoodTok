package com.example.foodtok.ui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.foodtok.R;
import com.example.foodtok.adapters.FeedAdapter;
import com.example.foodtok.adapters.OnRecipeInteractionListener;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.services.InteractionCallback;
import com.example.foodtok.services.InteractionServiceProvider;
import com.example.foodtok.services.RecipeListCallback;
import com.example.foodtok.services.RecipeServiceProvider;

import java.util.List;

/** Self-contained full-screen doomscroll feed for the grid explore tab. */
public class GridFeedFragment extends Fragment {

  private static final int FEED_PAGE_SIZE = 20;

  private ViewPager2 gridFeedViewPager;
  private FeedAdapter feedAdapter;

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (getActivity() instanceof MainActivity) {
      ((MainActivity) getActivity()).setBottomNavVisibility(false);
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (getActivity() instanceof MainActivity) {
      ((MainActivity) getActivity()).setBottomNavVisibility(true);
    }
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                           @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_grid_feed, container, false);
    gridFeedViewPager = view.findViewById(R.id.gridFeedViewPager);
    ProgressBar spinner = view.findViewById(R.id.gridFeedLoadingSpinner);

    view.findViewById(R.id.btnGridFeedBack).setOnClickListener(v ->
        requireActivity().getSupportFragmentManager().popBackStack());

    RecipeServiceProvider.getRecipeService().getFeedRecipes(
        0, FEED_PAGE_SIZE, new RecipeListCallback() {
          @Override
          public void onSuccess(List<Recipe> recipes) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
              spinner.setVisibility(View.GONE);
              initFeedAdapter(recipes);
            });
          }

          @Override
          public void onError(String message) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
              spinner.setVisibility(View.GONE);
              showToast("Failed to load: " + message);
            });
          }
        });

    return view;
  }

  private void initFeedAdapter(List<Recipe> recipes) {
    feedAdapter = new FeedAdapter(recipes, new OnRecipeInteractionListener() {
      @Override
      public void onLikeClicked(Recipe recipe) {
        InteractionServiceProvider.getInteractionService()
            .likeRecipe(recipe.getId(), new InteractionCallback() {
              @SuppressLint("NotifyDataSetChanged")
              @Override
              public void onSuccess() {
                feedAdapter.notifyDataSetChanged();
              }

              @Override
              public void onError(String message) {
                if ("Please log in first".equals(message)) {
                  startActivity(new Intent(getActivity(), LoginActivity.class));
                } else {
                  showToast(message);
                }
              }
            });
      }

      @Override
      public void onCommentClicked(Recipe recipe) {
        showToast("Comment clicked for " + recipe.getTitle());
      }

      @Override
      public void onSaveClicked(Recipe recipe) {
        InteractionServiceProvider.getInteractionService()
            .saveRecipe(recipe.getId(), new InteractionCallback() {
              @SuppressLint("NotifyDataSetChanged")
              @Override
              public void onSuccess() {
                feedAdapter.notifyDataSetChanged();
              }

              @Override
              public void onError(String message) {
                if ("Please log in first".equals(message)) {
                  startActivity(new Intent(getActivity(), LoginActivity.class));
                } else {
                  showToast(message);
                }
              }
            });
      }

      @Override
      public void onNotInterestedClicked(Recipe recipe) {
        InteractionServiceProvider.getInteractionService()
            .markNotInterested(recipe.getId(), new InteractionCallback() {
              @SuppressLint("NotifyDataSetChanged")
              @Override
              public void onSuccess() {
                feedAdapter.notifyDataSetChanged();
              }

              @Override
              public void onError(String message) {
                if ("Please log in first".equals(message)) {
                  startActivity(new Intent(getActivity(), LoginActivity.class));
                } else {
                  showToast(message);
                }
              }
            });
      }
    });

    feedAdapter.setParentVerticalPager(gridFeedViewPager);
    gridFeedViewPager.setAdapter(feedAdapter);

    int startPos = getArguments() != null ? getArguments().getInt("startPosition", 0) : 0;
    if (startPos > 0) {
      gridFeedViewPager.setCurrentItem(startPos, false);
    }
  }

  private void showToast(String message) {
    if (getContext() != null) {
      Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
    }
  }
}
