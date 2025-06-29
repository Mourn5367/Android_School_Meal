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

    // 특정 위치의 댓글 가져오기 (PostDetailActivity에서 사용)
    public Comment getCommentAtPosition(int position) {
        if (position >= 0 && position < comments.size()) {
            return comments.get(position);
        }
        return null;
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

            // 좋아요 수 표시 개선
            updateLikeDisplay(comment);

            // 좋아요 버튼 클릭 이벤트
            likeButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onCommentLikeClick(comment);
                }
            });
        }

        private void updateLikeDisplay(Comment comment) {
            int likes = comment.getLikes();

            if (likes > 0) {
                likeCountTextView.setVisibility(View.VISIBLE);
                likeCountTextView.setText(String.valueOf(likes));

                // 좋아요가 있을 때 아이콘 색상 변경
                if (likeIcon != null) {
                    likeIcon.setImageResource(R.drawable.ic_favorite); // 채워진 하트
                    likeIcon.setColorFilter(itemView.getContext().getColor(R.color.like_color));
                }
            } else {
                likeCountTextView.setVisibility(View.GONE);

                // 좋아요가 없을 때 기본 아이콘
                if (likeIcon != null) {
                    likeIcon.setImageResource(R.drawable.ic_favorite_border); // 빈 하트
                    likeIcon.setColorFilter(itemView.getContext().getColor(R.color.gray_dark));
                }
            }
        }
    }
}