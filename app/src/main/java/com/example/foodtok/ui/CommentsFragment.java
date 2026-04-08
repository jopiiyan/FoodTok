package com.example.foodtok.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.adapters.CommentAdapter;
import com.example.foodtok.models.Comment;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.util.ArrayList;
import java.util.List;

/**
 * Displays the comment section for a recipe as a bottom sheet.
 * Open it via CommentsFragment.newInstance(recipeId).show(fragmentManager, tag).
 *
 * OOP: Singleton-like factory (newInstance) ensures consistent instantiation with args.
 * Separation of Concerns: fragment manages UI only; data fetching will be delegated
 * to a service/repository layer when the backend is wired up.
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
    // TODO: fetch real comments for recipeId from backend via a service
    comments = buildMockComments();
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_comments, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
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

    updateCommentCount(tvCount);

    // Close sheet
    btnClose.setOnClickListener(v -> dismiss());

    // Post a new comment
    btnSend.setOnClickListener(v -> {
      String text = etInput.getText().toString().trim();
      if (TextUtils.isEmpty(text)) return;

      // TODO: replace "You" with the logged-in user's name from AuthManager
      Comment newComment = new Comment(
          "local_" + System.currentTimeMillis(),
          "You",
          null,
          text,
          System.currentTimeMillis()
      );
      comments.add(0, newComment);           // newest comment at top
      adapter.notifyItemInserted(0);
      rv.scrollToPosition(0);
      etInput.setText("");
      updateCommentCount(tvCount);
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    // Expand the bottom sheet fully so keyboard doesn't hide the input
    if (getDialog() != null && getDialog().getWindow() != null) {
      getDialog().getWindow().setSoftInputMode(
          WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }
  }

  // --- Private helpers ---

  private void updateCommentCount(TextView tvCount) {
    tvCount.setText(String.valueOf(comments.size()));
  }

  /** Placeholder data — remove once backend is connected. */
  private List<Comment> buildMockComments() {
    List<Comment> list = new ArrayList<>();
    long now = System.currentTimeMillis();
    list.add(new Comment("c1", "foodie_alice", null,
        "This looks absolutely delicious! Can't wait to try it.", now - 3600_000L));
    list.add(new Comment("c2", "chef_marco", null,
        "Pro tip: add a pinch of smoked paprika for extra depth.", now - 7200_000L));
    list.add(new Comment("c3", "healthy_eats", null,
        "Does this work with oat milk instead of regular milk?", now - 86400_000L));
    list.add(new Comment("c4", "ramen_lover99", null,
        "Made this last night and my family loved it!", now - 2 * 86400_000L));
    return list;
  }
}
