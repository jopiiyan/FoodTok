package com.example.foodtok.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.foodtok.R;
import com.example.foodtok.models.Comment;

import java.util.List;

/**
 * Adapter for the comments RecyclerView inside CommentsFragment.
 *
 * OOP: Separation of Concerns — adapter only handles view binding, not data fetching.
 * Tell, Don't Ask — ViewHolder.bind() owns all binding logic; the adapter just delegates.
 */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private final List<Comment> comments;

    public CommentAdapter(List<Comment> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(comments.get(position));
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    // --- ViewHolder ---

    static class CommentViewHolder extends RecyclerView.ViewHolder {

        private final ImageView iv_avatar;
        private final TextView tv_username;
        private final TextView tv_text;
        private final TextView tv_time;
        private final ImageView iv_like;
        private final TextView tv_like_count;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            iv_avatar    = itemView.findViewById(R.id.iv_comment_avatar);
            tv_username  = itemView.findViewById(R.id.tv_comment_username);
            tv_text      = itemView.findViewById(R.id.tv_comment_text);
            tv_time      = itemView.findViewById(R.id.tv_comment_time);
            iv_like      = itemView.findViewById(R.id.iv_comment_like);
            tv_like_count = itemView.findViewById(R.id.tv_comment_like_count);
        }

        /**
         * Tell, Don't Ask — ViewHolder decides how to display each Comment field.
         */
        void bind(Comment comment) {
            tv_username.setText(comment.getAuthorName());
            tv_text.setText(comment.getText());
            tv_time.setText(comment.getFormattedTime());
            tv_like_count.setText(String.valueOf(comment.getLikeCount()));

            // Avatar URL integration point: swap in Glide/Picasso here when backend is ready.
            // iv_avatar uses the placeholder drawable set in XML for now.

            iv_like.setOnClickListener(v -> {
                comment.incrementLike();
                tv_like_count.setText(String.valueOf(comment.getLikeCount()));
            });
        }
    }
}
