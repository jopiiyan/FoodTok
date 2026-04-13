package com.example.foodtok.ui;

import android.os.Bundle;
import android.text.InputFilter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;

import com.example.foodtok.R;
import com.example.foodtok.models.IngredientInput;
import com.example.foodtok.models.dto.CreateIngredientRequest;
import com.example.foodtok.models.dto.CreateRecipeIngredientRequest;
import com.example.foodtok.models.dto.IngredientDto;
import com.example.foodtok.models.dto.RecipeDto;
import com.example.foodtok.models.dto.RecipeIngredientDto;
import com.example.foodtok.services.SupabaseApi;
import com.example.foodtok.util.ApiClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment for editing the ingredients of an existing recipe.
 * Fetches current ingredients from Supabase, lets the user add/remove rows,
 * then replaces the recipe_ingredients rows on Save.
 */
public class ManageIngredientsFragment extends Fragment {

    private static final String ARG_RECIPE_ID = "recipe_id";

    private String recipeId;

    private EditText ingredientNameInput;
    private EditText ingredientQuantityInput;
    private Button addIngredientButton;
    private LinearLayout ingredientList;
    private ProgressBar loadingIndicator;
    private Button btnSave;

    private SupabaseApi api;

    public static ManageIngredientsFragment newInstance(String recipeId) {
        ManageIngredientsFragment fragment = new ManageIngredientsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_RECIPE_ID, recipeId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            recipeId = getArguments().getString(ARG_RECIPE_ID);
        }
        api = ApiClient.getSupabaseApi();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_manage_ingredients, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ingredientNameInput = view.findViewById(R.id.ingredientNameInput);
        ingredientQuantityInput = view.findViewById(R.id.ingredientQuantityInput);
        addIngredientButton = view.findViewById(R.id.addIngredientButton);
        ingredientList = view.findViewById(R.id.ingredientList);
        loadingIndicator = view.findViewById(R.id.loadingIndicator);
        btnSave = view.findViewById(R.id.btnSave);

        view.findViewById(R.id.btnBack).setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack());

        setupInputFilters();
        addIngredientButton.setOnClickListener(v -> addIngredientRow());
        btnSave.setOnClickListener(v -> saveIngredients());

        fetchCurrentIngredients();
    }

    private void setupInputFilters() {
        InputFilter noCommasOrSpaces = (source, start, end, dest, dstart, dend) -> {
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (c == ',' || Character.isWhitespace(c)) {
                    return "";
                }
            }
            return null;
        };
        ingredientNameInput.setFilters(new InputFilter[]{noCommasOrSpaces});
    }

    private void fetchCurrentIngredients() {
        loadingIndicator.setVisibility(View.VISIBLE);
        String select = "id,recipe_ingredients(quantity,is_optional,ingredients(id,name))";
        api.getRecipeById("eq." + recipeId, select)
                .enqueue(new Callback<List<RecipeDto>>() {
                    @Override
                    public void onResponse(Call<List<RecipeDto>> call,
                                           Response<List<RecipeDto>> response) {
                        if (!isAdded()) return;
                        loadingIndicator.setVisibility(View.GONE);
                        if (!response.isSuccessful() || response.body() == null
                                || response.body().isEmpty()) return;

                        RecipeDto recipe = response.body().get(0);
                        if (recipe.recipeIngredients == null) return;

                        requireActivity().runOnUiThread(() -> {
                            for (RecipeIngredientDto ri : recipe.recipeIngredients) {
                                if (ri.ingredient == null) continue;
                                String name = ri.ingredient.name;
                                String quantity = ri.quantity != null ? ri.quantity : "1";
                                String display = "• " + name + "  (" + quantity + ")";
                                LinearLayout row = buildRow(display, name);
                                row.setTag(new String[]{name, quantity});
                                ingredientList.addView(row);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Call<List<RecipeDto>> call, Throwable t) {
                        if (!isAdded()) return;
                        loadingIndicator.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Failed to load ingredients",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addIngredientRow() {
        String name = ingredientNameInput.getText().toString().trim();
        String quantity = ingredientQuantityInput.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Enter an ingredient name", Toast.LENGTH_SHORT).show();
            return;
        }
        if (ingredientRowExists(name)) {
            Toast.makeText(requireContext(), "Ingredient already added", Toast.LENGTH_SHORT).show();
            return;
        }
        if (quantity.isEmpty()) {
            quantity = "1";
        }
        String display = "• " + name + "  (" + quantity + ")";
        LinearLayout row = buildRow(display, name);
        row.setTag(new String[]{name, quantity});
        ingredientList.addView(row);
        ingredientNameInput.setText("");
        ingredientQuantityInput.setText("");
    }

    private boolean ingredientRowExists(String name) {
        String lower = name.toLowerCase();
        for (int i = 0; i < ingredientList.getChildCount(); i++) {
            Object tag = ingredientList.getChildAt(i).getTag();
            if (tag instanceof String[]) {
                String existing = ((String[]) tag)[0];
                if (existing != null && existing.toLowerCase().equals(lower)) {
                    return true;
                }
            }
        }
        return false;
    }

    private LinearLayout buildRow(String displayText, String dataValue) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 8, 0, 8);

        TextView nameView = new TextView(requireContext());
        LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        nameView.setLayoutParams(nameParams);
        nameView.setText(displayText);
        nameView.setTextColor(0xFF000000);
        nameView.setTextSize(15);
        nameView.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));

        Button removeButton = new Button(requireContext());
        removeButton.setText("✕");
        removeButton.setTextColor(0xFFFFFFFF);
        removeButton.setTypeface(ResourcesCompat.getFont(requireContext(), R.font.poppins));
        final LinearLayout rowRef = row;
        removeButton.setOnClickListener(v -> {
            ViewGroup parent = (ViewGroup) rowRef.getParent();
            if (parent != null) parent.removeView(rowRef);
        });

        row.addView(nameView);
        row.addView(removeButton);
        row.setTag(dataValue);
        return row;
    }

    private void saveIngredients() {
        List<IngredientInput> inputs = new ArrayList<>();
        for (int i = 0; i < ingredientList.getChildCount(); i++) {
            Object tag = ingredientList.getChildAt(i).getTag();
            if (tag instanceof String[]) {
                String[] pair = (String[]) tag;
                if (pair.length >= 2 && pair[0] != null) {
                    inputs.add(new IngredientInput(pair[0], pair[1]));
                }
            }
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving…");
        loadingIndicator.setVisibility(View.VISIBLE);

        // Step 1: delete all existing recipe_ingredients rows for this recipe
        api.deleteRecipeIngredients("eq." + recipeId)
                .enqueue(new Callback<Void>() {
                    @Override
                    public void onResponse(Call<Void> call, Response<Void> response) {
                        if (!isAdded()) return;
                        if (!response.isSuccessful()) {
                            onSaveError("Failed to clear old ingredients: HTTP " + response.code());
                            return;
                        }
                        if (inputs.isEmpty()) {
                            onSaveComplete();
                        } else {
                            // Step 2: re-insert with resolve-then-batch pattern
                            resolveAndInsertIngredients(inputs);
                        }
                    }

                    @Override
                    public void onFailure(Call<Void> call, Throwable t) {
                        if (!isAdded()) return;
                        onSaveError("Network error: " + t.getMessage());
                    }
                });
    }

    /** Resolves ingredient IDs one-by-one then batch-inserts join rows. */
    private void resolveAndInsertIngredients(List<IngredientInput> inputs) {
        final String[] ingredientIds = new String[inputs.size()];
        final int[] resolvedCount = {0};
        final boolean[] failed = {false};

        for (int i = 0; i < inputs.size(); i++) {
            final int index = i;
            final String name = inputs.get(i).getName().toLowerCase();

            api.getIngredientByName("eq." + name, "id")
                    .enqueue(new Callback<List<IngredientDto>>() {
                        @Override
                        public void onResponse(Call<List<IngredientDto>> call,
                                               Response<List<IngredientDto>> response) {
                            if (!isAdded() || failed[0]) return;
                            if (!response.isSuccessful()) {
                                failResolve("Lookup failed for '" + name + "'");
                                return;
                            }
                            List<IngredientDto> body = response.body();
                            if (body != null && !body.isEmpty()) {
                                ingredientIds[index] = body.get(0).id;
                                markResolved(inputs, ingredientIds, resolvedCount, failed);
                            } else {
                                createIngredientThenResolve(name, index, inputs,
                                        ingredientIds, resolvedCount, failed);
                            }
                        }

                        @Override
                        public void onFailure(Call<List<IngredientDto>> call, Throwable t) {
                            if (!isAdded() || failed[0]) return;
                            failResolve("Network error for '" + name + "'");
                        }
                    });
        }
    }

    private void createIngredientThenResolve(String name, int index,
                                              List<IngredientInput> inputs,
                                              String[] ingredientIds,
                                              int[] resolvedCount,
                                              boolean[] failed) {
        api.createIngredient(new CreateIngredientRequest(name))
                .enqueue(new Callback<List<IngredientDto>>() {
                    @Override
                    public void onResponse(Call<List<IngredientDto>> call,
                                           Response<List<IngredientDto>> response) {
                        if (!isAdded() || failed[0]) return;
                        if (response.isSuccessful() && response.body() != null
                                && !response.body().isEmpty()) {
                            ingredientIds[index] = response.body().get(0).id;
                            markResolved(inputs, ingredientIds, resolvedCount, failed);
                        } else {
                            failResolve("Insert failed for '" + name + "'");
                        }
                    }

                    @Override
                    public void onFailure(Call<List<IngredientDto>> call, Throwable t) {
                        if (!isAdded() || failed[0]) return;
                        failResolve("Network error inserting '" + name + "'");
                    }
                });
    }

    private void markResolved(List<IngredientInput> inputs, String[] ingredientIds,
                               int[] resolvedCount, boolean[] failed) {
        resolvedCount[0]++;
        if (resolvedCount[0] == inputs.size()) {
            batchInsertRecipeIngredients(inputs, ingredientIds, failed);
        }
    }

    private void batchInsertRecipeIngredients(List<IngredientInput> inputs,
                                               String[] ingredientIds,
                                               boolean[] failed) {
        List<CreateRecipeIngredientRequest> rows = new ArrayList<>();
        for (int i = 0; i < inputs.size(); i++) {
            String quantity = inputs.get(i).getQuantity();
            if (quantity == null || quantity.trim().isEmpty()) quantity = "1";
            rows.add(new CreateRecipeIngredientRequest(recipeId, ingredientIds[i], quantity, false));
        }

        api.createRecipeIngredients(rows).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) return;
                if (!response.isSuccessful()) {
                    failResolve("Failed to save ingredients: HTTP " + response.code());
                } else {
                    onSaveComplete();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                if (!isAdded()) return;
                failResolve("Network error saving ingredients");
            }
        });
    }

    private void failResolve(String message) {
        onSaveError(message);
    }

    private void onSaveComplete() {
        requireActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(), "Ingredients updated", Toast.LENGTH_SHORT).show();
            requireActivity().getSupportFragmentManager().popBackStack();
        });
    }

    private void onSaveError(String message) {
        requireActivity().runOnUiThread(() -> {
            loadingIndicator.setVisibility(View.GONE);
            btnSave.setEnabled(true);
            btnSave.setText("Save");
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
        });
    }
}
