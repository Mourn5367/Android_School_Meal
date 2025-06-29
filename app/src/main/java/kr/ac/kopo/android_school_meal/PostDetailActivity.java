package kr.ac.kopo.android_school_meal;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostDetailActivity extends AppCompatActivity {
    private static final String TAG = "PostDetailActivity";

    // UI 컴포넌트
    private MaterialTextView titleTextView;
    private MaterialTextView authorTextView;
    private MaterialTextView timeTextView;
    private MaterialTextView contentTextView;
    private ImageView postImageView;
    private LinearLayout likeButton;
    private MaterialTextView likeCountTextView;
    private MaterialTextView commentTitleTextView;
    private RecyclerView commentsRecyclerView;
    private TextInputLayout authorEditLayout;
    private TextInputEditText authorEditText;
    private TextInputLayout commentEditLayout;
    private TextInputEditText commentEditText;
    private MaterialButton sendCommentButton;

    // 데이터
    private Post post;
    private CommentAdapter commentAdapter;
    private NetworkManager networkManager;
    private boolean isLiked = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        // 인텐트에서 데이터 받기
        getIntentData();

        // 뷰 초기화
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupButtons();

        // 네트워크 매니저 초기화
        networkManager = NetworkManager.getInstance();

        // 게시글 정보 표시
        displayPostInfo();

        // 댓글 로드
        loadComments();
    }

    private void getIntentData() {
        Intent intent = getIntent();

        // Post 객체 생성
        post = new Post();
        post.setId(intent.getIntExtra("post_id", 0));
        post.setTitle(intent.getStringExtra("post_title"));
        post.setAuthor(intent.getStringExtra("post_author"));
        post.setContent(intent.getStringExtra("post_content"));
        post.setCreatedAt(intent.getStringExtra("post_created_at"));
        post.setLikes(intent.getIntExtra("post_likes", 0));
        post.setCommentCount(intent.getIntExtra("post_comment_count", 0));
        post.setImageUrl(intent.getStringExtra("post_image_url"));

        Log.d(TAG, "게시글 정보 - ID: " + post.getId() + ", 제목: " + post.getTitle());
    }

    private void initViews() {
        titleTextView = findViewById(R.id.titleTextView);
        authorTextView = findViewById(R.id.authorTextView);
        timeTextView = findViewById(R.id.timeTextView);
        contentTextView = findViewById(R.id.contentTextView);
        postImageView = findViewById(R.id.postImageView);
        likeButton = findViewById(R.id.likeButton);
        likeCountTextView = findViewById(R.id.likeCountTextView);
        commentTitleTextView = findViewById(R.id.commentTitleTextView);
        commentsRecyclerView = findViewById(R.id.commentsRecyclerView);
        authorEditLayout = findViewById(R.id.authorEditLayout);
        authorEditText = findViewById(R.id.authorEditText);
        commentEditLayout = findViewById(R.id.commentEditLayout);
        commentEditText = findViewById(R.id.commentEditText);
        sendCommentButton = findViewById(R.id.sendCommentButton);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("게시글");
        }
    }

    private void setupRecyclerView() {
        commentAdapter = new CommentAdapter();
        commentsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        commentsRecyclerView.setAdapter(commentAdapter);
    }

    private void setupButtons() {
        likeButton.setOnClickListener(v -> toggleLike());
        sendCommentButton.setOnClickListener(v -> addComment());
    }

    private void displayPostInfo() {
        // 제목
        titleTextView.setText(post.getTitle());

        // 작성자
        authorTextView.setText(post.getAuthor());

        // 시간
        String relativeTime = DateUtils.formatRelativeTime(post.getCreatedAt());
        timeTextView.setText(relativeTime);

        // 내용
        contentTextView.setText(post.getContent());

        // 이미지
        if (!TextUtils.isEmpty(post.getImageUrl())) {
            postImageView.setVisibility(View.VISIBLE);
            Glide.with(this)
                    .load(post.getImageUrl())
                    .into(postImageView);
        } else {
            postImageView.setVisibility(View.GONE);
        }

        // 좋아요 수
        updateLikeDisplay();
    }

    private void updateLikeDisplay() {
        likeCountTextView.setText("좋아요 " + post.getLikes() + "개");

        // 좋아요 상태에 따른 UI 업데이트
        likeButton.setSelected(isLiked);
    }

    private void toggleLike() {
        if (networkManager == null) return;

        // LikeRequest 생성 (사용자 식별자)
        ApiService.LikeRequest likeRequest = new ApiService.LikeRequest("anonymous");

        networkManager.getApiService().togglePostLike(post.getId(), likeRequest)
                .enqueue(new Callback<ApiService.LikeResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiService.LikeResponse> call,
                                           @NonNull Response<ApiService.LikeResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiService.LikeResponse likeResponse = response.body();
                            isLiked = likeResponse.liked;
                            post.setLikes(likeResponse.likes);
                            updateLikeDisplay();
                        } else {
                            Log.e(TAG, "좋아요 처리 실패: " + response.code());
                            showToast("좋아요 처리에 실패했습니다.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiService.LikeResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "좋아요 처리 네트워크 오류", t);
                        showToast("인터넷 연결을 확인해주세요.");
                    }
                });
    }

    private void loadComments() {
        if (networkManager == null) return;

        // getPostDetail을 사용해서 게시글 상세 정보와 댓글을 함께 가져옴
        networkManager.getApiService().getPostDetail(post.getId())
                .enqueue(new Callback<ApiService.PostDetailResponse>() {
                    @Override
                    public void onResponse(@NonNull Call<ApiService.PostDetailResponse> call,
                                           @NonNull Response<ApiService.PostDetailResponse> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ApiService.PostDetailResponse detailResponse = response.body();
                            List<Comment> comments = detailResponse.comments != null ?
                                    detailResponse.comments : new ArrayList<>();
                            Log.d(TAG, "댓글 로드 성공: " + comments.size() + "개");
                            displayComments(comments);
                        } else {
                            Log.e(TAG, "댓글 로드 실패: " + response.code());
                            displayComments(new ArrayList<>());
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ApiService.PostDetailResponse> call, @NonNull Throwable t) {
                        Log.e(TAG, "댓글 로드 네트워크 오류", t);
                        displayComments(new ArrayList<>());
                    }
                });
    }

    private void displayComments(List<Comment> comments) {
        commentTitleTextView.setText("댓글 " + comments.size() + "개");
        commentAdapter.updateData(comments);
    }

    private void addComment() {
        String author = authorEditText.getText().toString().trim();
        String content = commentEditText.getText().toString().trim();

        if (TextUtils.isEmpty(author)) {
            authorEditLayout.setError("닉네임을 입력해주세요");
            return;
        } else {
            authorEditLayout.setError(null);
        }

        if (TextUtils.isEmpty(content)) {
            commentEditLayout.setError("댓글 내용을 입력해주세요");
            return;
        } else {
            commentEditLayout.setError(null);
        }

        if (networkManager == null) return;

        // CreateCommentRequest 생성 (content, author 순서)
        ApiService.CreateCommentRequest request = new ApiService.CreateCommentRequest(
                content, author
        );

        networkManager.getApiService().createComment(post.getId(), request)
                .enqueue(new Callback<Comment>() {
                    @Override
                    public void onResponse(@NonNull Call<Comment> call,
                                           @NonNull Response<Comment> response) {
                        if (response.isSuccessful()) {
                            Log.d(TAG, "댓글 작성 성공");
                            showToast("댓글이 작성되었습니다.");

                            // 입력 필드 초기화
                            authorEditText.setText("");
                            commentEditText.setText("");

                            // 댓글 목록 새로고침
                            loadComments();
                        } else {
                            Log.e(TAG, "댓글 작성 실패: " + response.code());
                            showToast("댓글 작성에 실패했습니다.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Comment> call, @NonNull Throwable t) {
                        Log.e(TAG, "댓글 작성 네트워크 오류", t);
                        showToast("인터넷 연결을 확인해주세요.");
                    }
                });
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}