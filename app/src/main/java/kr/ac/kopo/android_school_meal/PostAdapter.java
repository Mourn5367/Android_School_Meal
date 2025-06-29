package kr.ac.kopo.android_school_meal;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> posts = new ArrayList<>();
    private OnPostClickListener listener;

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public PostAdapter(OnPostClickListener listener) {
        this.listener = listener;
    }

    public void updateData(List<Post> newPosts) {
        this.posts.clear();
        this.posts.addAll(newPosts);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.bind(post);
    }

    @Override
    public int getItemCount() {
        return posts.size();
    }

    class PostViewHolder extends RecyclerView.ViewHolder {
        private MaterialCardView postCard;
        private MaterialTextView titleText;
        private MaterialTextView contentText;
        private MaterialTextView authorText;
        private MaterialTextView timeText;
        private MaterialTextView likesText;
        private MaterialTextView commentsText;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            postCard = itemView.findViewById(R.id.postCard);
            titleText = itemView.findViewById(R.id.titleText);
            contentText = itemView.findViewById(R.id.contentText);
            authorText = itemView.findViewById(R.id.authorText);
            timeText = itemView.findViewById(R.id.timeText);
            likesText = itemView.findViewById(R.id.likesText);
            commentsText = itemView.findViewById(R.id.commentsText);
        }

        public void bind(Post post) {
            // 제목
            titleText.setText(post.getTitle());

            // 내용 (최대 2줄)
            contentText.setText(post.getContent());

            // 작성자
            authorText.setText(post.getAuthor());

            // 작성 시간
            String relativeTime = DateUtils.formatRelativeTime(post.getCreatedAt());
            timeText.setText(relativeTime);

            // 좋아요 수
            likesText.setText(String.valueOf(post.getLikes()));

            // 댓글 수
            commentsText.setText(String.valueOf(post.getCommentCount()));

            // 클릭 이벤트
            postCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClick(post);
                }
            });
        }
    }
}
