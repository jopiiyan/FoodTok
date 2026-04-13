package com.example.foodtok.ui;

import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;

import com.example.foodtok.R;
import com.example.foodtok.adapters.CommentAdapter;
import com.example.foodtok.models.Comment;
import com.example.foodtok.services.CommentCallback;
import com.example.foodtok.services.CommentListCallback;
import com.example.foodtok.services.CommentServiceProvider;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the comment section for a recipe as a bottom sheet.
 * Open it via CommentsFragment.newInstance(recipeId).show(fragmentManager, tag).
 *
 * OOP: Singleton-like factory (newInstance) ensures consistent instantiation with args.
 * Separation of Concerns: fragment manages UI only; data fetching is delegated
 * to the comment service layer.
 */
public class CommentsFragment extends BottomSheetDialogFragment {

  private static final String ARG_RECIPE_ID = "recipe_id";

  private String recipeId;
  private List<Comment> comments;
  private CommentAdapter adapter;

  // --- Factory method (OOP: controls instantiation, enforces required args) ---

  public static CommentsFragment newInstance(String recipeId) {
    CommentsFragment fragment = new CommentsFragment();
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
    comments = new ArrayList<>();
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_comments, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view,
      @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    TextView tvCount     = view.findViewById(R.id.tv_comment_count);
    RecyclerView rv      = view.findViewById(R.id.rv_comments);
    EditText etInput     = view.findViewById(R.id.et_comment_input);
    ImageView btnSend    = view.findViewById(R.id.btn_send_comment);
    ImageView btnClose   = view.findViewById(R.id.btn_close_comments);

    // Set up RecyclerView
    adapter = new CommentAdapter(comments);
    rv.setLayoutManager(new LinearLayoutManager(getContext()));
    rv.setAdapter(adapter);

    // Close sheet
    btnClose.setOnClickListener(v -> dismiss());

    // Load comments from backend
    CommentServiceProvider.getCommentService().getComments(recipeId,
        new CommentListCallback() {
          @Override
          public void onSuccess(List<Comment> loaded) {
            if (getActivity() == null) {
              return;
            }
            getActivity().runOnUiThread(() -> {
              comments.clear();
              comments.addAll(loaded);
              adapter.notifyDataSetChanged();
              updateCommentCount(tvCount);
            });
          }

          @Override
          public void onError(String message) {
            if (getContext() != null) {
              Toast.makeText(getContext(),
                  "Failed to load comments: " + message,
                  Toast.LENGTH_SHORT).show();
            }
          }
        });

    updateCommentCount(tvCount);

    // Post a new comment
    btnSend.setOnClickListener(v -> {
      String text = etInput.getText().toString().trim();
      if (TextUtils.isEmpty(text)) {
        return;
      }

      btnSend.setEnabled(false);
      CommentServiceProvider.getCommentService().postComment(recipeId,
          text, new CommentCallback() {
            @Override
            public void onSuccess(Comment comment) {
              if (getActivity() == null) {
                return;
              }
              getActivity().runOnUiThread(() -> {
                comments.add(0, comment);
                adapter.notifyItemInserted(0);
                rv.scrollToPosition(0);
                etInput.setText("");
                updateCommentCount(tvCount);
                btnSend.setEnabled(true);
              });
            }

            @Override
            public void onError(String message) {
              if (getActivity() == null) {
                return;
              }
              getActivity().runOnUiThread(() -> {
                Toast.makeText(getContext(),
                    "Failed to post comment: " + message,
                    Toast.LENGTH_SHORT).show();
                btnSend.setEnabled(true);
              });
            }
          });
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    if (getDialog() == null || getDialog().getWindow() == null) {
      return;
    }
    getDialog().getWindow().setSoftInputMode(
        WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

    FrameLayout sheet = getDialog().findViewById(
        com.google.android.material.R.id.design_bottom_sheet);
    if (sheet == null) {
      return;
    }

    int halfScreen =
        Resources.getSystem().getDisplayMetrics().heightPixels / 2;

    BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(sheet);
    behavior.setPeekHeight(halfScreen);
    behavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    behavior.setSkipCollapsed(false);

    // When the keyboard opens, expand to full screen so the input row
    // floats above the IME; on dismiss, drop back to the 50% peek.
    ViewCompat.setOnApplyWindowInsetsListener(sheet, (v, insets) -> {
      boolean imeVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
      behavior.setState(imeVisible
          ? BottomSheetBehavior.STATE_EXPANDED
          : BottomSheetBehavior.STATE_COLLAPSED);
      return insets;
    });
  }

  // --- Private helpers ---

  private void updateCommentCount(TextView tvCount) {
    tvCount.setText(String.valueOf(comments.size()));
  }
}
