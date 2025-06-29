package kr.ac.kopo.android_school_meal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments = new ArrayList<>();
    private OnCommentClickListener listener;

    public interface OnCommentClickListener {
        void onCommentLikeClick(Comment comment);
    }

    public CommentAdapter() {}

    public CommentAdapter(OnCommentClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Comment> newComments) {
        this.comments.clear();
        this.comments.addAll(newComments);
        notifyDataSetChanged();
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
        Comment comment = comments.get(position);
        holder.bind(comment);
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    class CommentViewHolder extends RecyclerView.ViewHolder {
        private MaterialTextView contentTextView;
        private MaterialTextView authorTextView;
        private MaterialTextView timeTextView;
        private LinearLayout likeButton;
        private ImageView likeIcon;
        private MaterialTextView likeCountTextView;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            contentTextView = itemView.findViewById(R.id.contentTextView);
            authorTextView = itemView.findViewById(R.id.authorTextView);
            timeTextView = itemView.findViewById(R.id.timeTextView);
            likeButton = itemView.findViewById(R.id.likeButton);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            likeCountTextView = itemView.findViewById(R.id.likeCountTextView);
        }

        public void bind(Comment comment) {
            // 댓글 내용
            contentTextView.setText(comment.getContent());

            // 작성자
            authorTextView.setText(comment.getAuthor());

            // 작성 시간
            String relativeTime = DateUtils.formatRelativeTime(comment.getCreatedAt());
            timeTextView.setText(relativeTime);

            // 좋아요 수
            if (comment.getLikes() > 0) {
                likeCountTextView.setVisibility(View.VISIBLE);
                likeCountTextView.setText(String.valueOf(comment.getLikes()));
            } else {
                likeCountTextView.setVisibility(View.GONE);
            }

            // 좋아요 버튼 클릭 이벤트
            likeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentLikeClick(comment);
                }
            });
        }
    }
}