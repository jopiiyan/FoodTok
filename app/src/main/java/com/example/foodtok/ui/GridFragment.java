package com.example.foodtok.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.adapters.GridAdapter;
import com.example.foodtok.models.Recipe;
import com.example.foodtok.services.RecipeListCallback;
import com.example.foodtok.services.RecipeServiceProvider;

import java.util.List;

/** Grid layout video feed tab fragment — shows recipes in a 2-column thumbnail grid. */
public class GridFragment extends Fragment {

  private static final int GRID_PAGE_SIZE = 20;

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_grid, container, false);

    RecyclerView rvGrid = view.findViewById(R.id.rvGrid);
    ProgressBar spinner = view.findViewById(R.id.gridLoadingSpinner);

    rvGrid.setLayoutManager(new GridLayoutManager(getContext(), 2));

    RecipeServiceProvider.getRecipeService().getFeedRecipes(
        0, GRID_PAGE_SIZE, new RecipeListCallback() {
          @Override
          public void onSuccess(List<Recipe> recipes) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
              spinner.setVisibility(View.GONE);
              GridAdapter adapter = new GridAdapter(recipes, position -> openFeedAt(position));
              rvGrid.setAdapter(adapter);
            });
          }

          @Override
          public void onError(String message) {
            if (getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
              spinner.setVisibility(View.GONE);
              Toast.makeText(getContext(), "Failed to load recipes: " + message,
                  Toast.LENGTH_SHORT).show();
            });
          }
        });

    return view;
  }

  private void openFeedAt(int position) {
    Bundle args = new Bundle();
    args.putInt("startPosition", position);
    GridFeedFragment feedFragment = new GridFeedFragment();
    feedFragment.setArguments(args);

    requireActivity().getSupportFragmentManager()
        .beginTransaction()
        .setCustomAnimations(
            R.anim.feed_enter,   // new fragment enters
            R.anim.feed_exit,    // current fragment exits
            R.anim.feed_enter,   // current fragment re-enters on back
            R.anim.feed_exit     // new fragment exits on back
        )
        .replace(R.id.fragmentContainer, feedFragment)
        .addToBackStack(null)
        .commit();
  }
}
