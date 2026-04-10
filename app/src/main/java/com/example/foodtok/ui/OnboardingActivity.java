package com.example.foodtok.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ViewFlipper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.auth.AuthManager;
import com.example.foodtok.models.User;
import com.example.foodtok.models.dto.UpdateProfileRequest;
import com.example.foodtok.models.dto.UserDto;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;
import com.example.foodtok.util.SessionManager;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Post-signup onboarding screen that collects user preferences in 3 steps:
 * avatar selection, cuisine/style preferences, and allergen blacklist.
 */
public class OnboardingActivity extends AppCompatActivity {

  private static final String TAG = "OnboardingActivity";
  private static final int INITIAL_PREFERENCE_SCORE = 30;

  private static final String[] PREFERENCE_TAGS = {
      "Italian", "Vegan", "Quick Meals", "Desserts", "Asian",
      "Healthy", "Comfort Food", "Seafood", "Spicy", "Breakfast",
      "Mexican", "Mediterranean", "Baking", "Grilling", "Soup"
  };

  private static final String[] ALLERGEN_TAGS = {
      "Peanuts", "Gluten", "Dairy", "Shellfish", "Eggs",
      "Soy", "Tree Nuts", "Fish", "Sesame", "Wheat"
  };

  private static final String[] AVATAR_URLS = {
      "avatar_chef", "avatar_pizza", "avatar_sushi",
      "avatar_cupcake", "avatar_salad", "avatar_burger",
      "avatar_taco", "avatar_ramen", "avatar_cookie"
  };

  private ViewFlipper viewFlipper;
  private TextView tvStepIndicator;
  private Button btnBack;
  private Button btnSkip;
  private Button btnNext;

  private String selectedAvatarUrl;
  private final Set<String> selectedPreferences = new HashSet<>();
  private final Set<String> selectedAllergens = new HashSet<>();

  private int currentStep = 0;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_onboarding);

    viewFlipper = findViewById(R.id.viewFlipper);
    tvStepIndicator = findViewById(R.id.tvStepIndicator);
    btnBack = findViewById(R.id.btnBack);
    btnSkip = findViewById(R.id.btnSkip);
    btnNext = findViewById(R.id.btnNext);

    setupAvatarStep();
    setupChipGroup(
        findViewById(R.id.chipGroupPreferences), PREFERENCE_TAGS,
        selectedPreferences);
    setupChipGroup(
        findViewById(R.id.chipGroupAllergens), ALLERGEN_TAGS,
        selectedAllergens);

    btnNext.setOnClickListener(v -> onNextClicked());
    btnBack.setOnClickListener(v -> onBackClicked());
    btnSkip.setOnClickListener(v -> onSkipClicked());

    updateStepUi();
  }

  private void setupAvatarStep() {
    RecyclerView rvAvatars = findViewById(R.id.rvAvatars);
    rvAvatars.setLayoutManager(new GridLayoutManager(this, 3));
    rvAvatars.setAdapter(new AvatarAdapter(AVATAR_URLS, url -> {
      selectedAvatarUrl = url;
      rvAvatars.getAdapter().notifyDataSetChanged();
    }));
  }

  private void setupChipGroup(ChipGroup group, String[] tags,
      Set<String> selectedSet) {
    for (String tag : tags) {
      Chip chip = new Chip(this);
      chip.setText(tag);
      chip.setCheckable(true);
      chip.setCheckedIconVisible(true);
      chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
        if (isChecked) {
          selectedSet.add(tag);
        } else {
          selectedSet.remove(tag);
        }
      });
      group.addView(chip);
    }
  }

  private void onNextClicked() {
    if (currentStep < 2) {
      currentStep++;
      viewFlipper.showNext();
      updateStepUi();
    } else {
      finishOnboarding();
    }
  }

  private void onBackClicked() {
    if (currentStep > 0) {
      currentStep--;
      viewFlipper.showPrevious();
      updateStepUi();
    }
  }

  private void onSkipClicked() {
    if (currentStep < 2) {
      currentStep++;
      viewFlipper.showNext();
      updateStepUi();
    } else {
      finishOnboarding();
    }
  }

  private void updateStepUi() {
    tvStepIndicator.setText("Step " + (currentStep + 1) + " of 3");

    btnBack.setVisibility(currentStep > 0 ? View.VISIBLE : View.GONE);

    if (currentStep == 2) {
      btnNext.setText("Finish");
      btnSkip.setText("Skip");
    } else {
      btnNext.setText("Next");
      btnSkip.setText("Skip");
    }
  }

  private void finishOnboarding() {
    btnNext.setEnabled(false);
    btnSkip.setEnabled(false);

    User user = AuthManager.getInstance().getCurrentUser();
    if (user != null) {
      // Update in-memory user model
      if (selectedAvatarUrl != null) {
        user.setAvatarUrl(selectedAvatarUrl);
      }
      for (String tag : selectedPreferences) {
        user.updateInterestScore(tag, INITIAL_PREFERENCE_SCORE);
      }
      for (String allergen : selectedAllergens) {
        user.addBlacklistedIngredient(allergen);
      }
    }

    // Persist to Supabase
    saveProfileToSupabase();
  }

  private void saveProfileToSupabase() {
    String userId = SessionManager.getInstance().getUserId();
    if (userId == null) {
      navigateToMain();
      return;
    }

    UpdateProfileRequest request = new UpdateProfileRequest();
    request.avatarUrl = selectedAvatarUrl;

    // Build interest profile map with initial scores
    Map<String, Integer> interestMap = new LinkedHashMap<>();
    for (String tag : selectedPreferences) {
      interestMap.put(tag, INITIAL_PREFERENCE_SCORE);
    }
    request.interestProfile = interestMap;

    request.blacklistedIngredients =
        new ArrayList<>(selectedAllergens);

    SupabaseApi api =
        ApiClient.getRestClient().create(SupabaseApi.class);
    api.updateProfile("eq." + userId, request)
        .enqueue(new Callback<List<UserDto>>() {
          @Override
          public void onResponse(Call<List<UserDto>> call,
              Response<List<UserDto>> response) {
            if (!response.isSuccessful()) {
              Log.w(TAG, "Profile update failed: "
                  + response.code());
            }
            runOnUiThread(() -> navigateToMain());
          }

          @Override
          public void onFailure(Call<List<UserDto>> call,
              Throwable t) {
            Log.w(TAG, "Profile update network error", t);
            runOnUiThread(() -> navigateToMain());
          }
        });
  }

  private void navigateToMain() {
    Intent intent = new Intent(this, MainActivity.class);
    intent.setFlags(
        Intent.FLAG_ACTIVITY_NEW_TASK
            | Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startActivity(intent);
    finish();
  }

  // ── Avatar adapter (inner class) ─────────────────────────────────────

  /**
   * Simple RecyclerView adapter for avatar selection grid.
   * Each item shows an avatar identifier and highlights the selected one.
   */
  private class AvatarAdapter
      extends RecyclerView.Adapter<AvatarAdapter.VH> {

    private final String[] avatars;
    private final OnAvatarSelected callback;

    AvatarAdapter(String[] avatars, OnAvatarSelected callback) {
      this.avatars = avatars;
      this.callback = callback;
    }

    @Override
    public VH onCreateViewHolder(
        android.view.ViewGroup parent, int viewType) {
      TextView tv = new TextView(parent.getContext());
      int size = (int) (80 * parent.getContext().getResources()
          .getDisplayMetrics().density);
      tv.setLayoutParams(new RecyclerView.LayoutParams(
          RecyclerView.LayoutParams.MATCH_PARENT, size));
      tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
      tv.setGravity(android.view.Gravity.CENTER);
      tv.setTextSize(32);
      return new VH(tv);
    }

    @Override
    public void onBindViewHolder(VH holder, int position) {
      String avatar = avatars[position];
      // Display a food emoji based on the avatar identifier
      holder.tv.setText(getEmojiForAvatar(avatar));

      boolean selected = avatar.equals(selectedAvatarUrl);
      if (selected) {
        holder.tv.setBackgroundResource(
            R.drawable.bg_chip_selected);
        holder.tv.setAlpha(1.0f);
      } else {
        holder.tv.setBackgroundResource(
            R.drawable.bg_chip_unselected);
        holder.tv.setAlpha(0.7f);
      }

      holder.tv.setOnClickListener(v -> callback.onSelected(avatar));
    }

    @Override
    public int getItemCount() {
      return avatars.length;
    }

    private String getEmojiForAvatar(String avatar) {
      switch (avatar) {
        case "avatar_chef": return "\uD83D\uDC68\u200D\uD83C\uDF73";
        case "avatar_pizza": return "\uD83C\uDF55";
        case "avatar_sushi": return "\uD83C\uDF63";
        case "avatar_cupcake": return "\uD83E\uDDC1";
        case "avatar_salad": return "\uD83E\uDD57";
        case "avatar_burger": return "\uD83C\uDF54";
        case "avatar_taco": return "\uD83C\uDF2E";
        case "avatar_ramen": return "\uD83C\uDF5C";
        case "avatar_cookie": return "\uD83C\uDF6A";
        default: return "\uD83C\uDF7D";
      }
    }

    class VH extends RecyclerView.ViewHolder {
      final TextView tv;

      VH(TextView tv) {
        super(tv);
        this.tv = tv;
      }
    }
  }

  interface OnAvatarSelected {
    void onSelected(String avatarUrl);
  }
}