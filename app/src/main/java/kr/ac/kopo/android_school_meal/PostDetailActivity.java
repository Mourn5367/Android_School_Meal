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
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;

public class PostDetailActivity extends AppCompatActivity implements CommentAdapter.OnCommentClickListener {
    private static final String TAG = "PostDetailActivity";

    // 뷰 요소들
    private MaterialTextView titleTextView;
    private MaterialTextView authorTextView;
    private MaterialTextView timeTextView;
    private MaterialTextView contentTextView;
    private ImageView postImageView;                 // ImageView로 변경
    private LinearLayout likeButton;
    private MaterialTextView likeCountTextView;
    private MaterialTextView commentTitleTextView;
    private RecyclerView commentsRecyclerView;
    private TextInputLayout authorEditLayout;
    private TextInputEditText authorEditText;
    private TextInputLayout commentEditLayout;
    private TextInputEditText commentEditText;
    private View sendCommentButton;

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
        sendCommentButton = (View) findViewById(R.id.sendCommentButton).getParent();

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
        commentAdapter = new CommentAdapter(this); // this를 listener로 전달
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
            Log.d(TAG, "원본 이미지 URL: " + post.getImageUrl());

            String fullImageUrl;
            if (post.getImageUrl().startsWith("http")) {
                // 이미 완전한 URL인 경우
                fullImageUrl = post.getImageUrl();
            } else {
                // 상대경로인 경우 Base URL 추가
                fullImageUrl = ApiConfig.BASE_URL + post.getImageUrl();
            }

            Log.d(TAG, "완성된 이미지 URL: " + fullImageUrl);

            postImageView.setVisibility(android.view.View.VISIBLE);
            Glide.with(this)
                    .load(fullImageUrl)
                    .into(postImageView);
        } else {
            postImageView.setVisibility(android.view.View.GONE);
        }

        // 좋아요 수
        updateLikeDisplay();
    }

    private void updateLikeDisplay() {
        likeCountTextView.setText("좋아요 " + post.getLikes() + "개");

        // 좋아요 상태에 따른 UI 업데이트
        likeButton.setSelected(isLiked);
    }

    // 개선된 좋아요 토글 (NetworkRequestUtility 사용)
    private void toggleLike() {
        if (networkManager == null) return;

        ApiService.LikeRequest likeRequest = new ApiService.LikeRequest("anonymous");
        Call<ApiService.LikeResponse> call = networkManager.getApiService().togglePostLike(post.getId(), likeRequest);

        NetworkRequestUtility.executeWithRetry(call, new NetworkRequestUtility.NetworkCallback<ApiService.LikeResponse>() {
            @Override
            public void onSuccess(ApiService.LikeResponse result) {
                isLiked = result.liked;
                post.setLikes(result.likes);
                updateLikeDisplay();
                Log.d(TAG, "좋아요 처리 성공: " + result.liked + ", 총 " + result.likes + "개");
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "좋아요 처리 최종 실패: " + errorMessage);
                // ProtocolException의 경우 사용자에게 에러를 보이지 않고 조용히 실패
                if (!errorMessage.contains("서버 연결이 불안정")) {
                    showToast("좋아요 처리에 실패했습니다.");
                }
            }

            @Override
            public void onLoading(boolean isLoading) {
                // 좋아요는 빠른 동작이므로 로딩 표시 안 함
            }
        }, "좋아요 처리", false); // 로딩 표시 안 함
    }

    // 개선된 댓글 로드 (NetworkRequestUtility 사용)
    private void loadComments() {
        if (networkManager == null) return;

        Call<ApiService.PostDetailResponse> call = networkManager.getApiService().getPostDetail(post.getId());

        NetworkRequestUtility.executeWithRetry(call, new NetworkRequestUtility.NetworkCallback<ApiService.PostDetailResponse>() {
            @Override
            public void onSuccess(ApiService.PostDetailResponse result) {
                List<Comment> comments = result.comments != null ? result.comments : new ArrayList<>();
                Log.d(TAG, "댓글 로드 성공: " + comments.size() + "개");
                displayComments(comments);
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "댓글 로드 최종 실패: " + errorMessage);
                displayComments(new ArrayList<>()); // 빈 댓글 목록 표시
                if (!errorMessage.contains("서버 연결이 불안정")) {
                    showToast("댓글을 불러올 수 없습니다.");
                }
            }

            @Override
            public void onLoading(boolean isLoading) {
                // 댓글 로딩은 자동으로 처리되므로 별도 UI 업데이트 불필요
            }
        }, "댓글 로드");
    }

    private void displayComments(List<Comment> comments) {
        commentTitleTextView.setText("댓글 " + comments.size() + "개");
        commentAdapter.updateData(comments);
    }

    // 댓글 작성 (NetworkRequestUtility 사용)
    private void addComment() {
        String author = authorEditText.getText().toString().trim();
        String content = commentEditText.getText().toString().trim();

        // 입력 검증
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

        // 버튼 비활성화 (중복 전송 방지)
        sendCommentButton.setEnabled(false);
        sendCommentButton.setAlpha(0.5f);

        ApiService.CreateCommentRequest request = new ApiService.CreateCommentRequest(content, author);
        Call<Comment> call = networkManager.getApiService().createComment(post.getId(), request);

        // 댓글 작성은 재시도하지 않음 (중복 방지)
        NetworkRequestUtility.executeOnce(call, new NetworkRequestUtility.NetworkCallback<Comment>() {
            @Override
            public void onSuccess(Comment result) {
                Log.d(TAG, "댓글 작성 성공");
                showToast("댓글이 작성되었습니다.");

                // 입력 필드 초기화
                authorEditText.setText("");
                commentEditText.setText("");

                // 버튼 활성화
                sendCommentButton.setEnabled(true);
                sendCommentButton.setAlpha(1.0f);

                // 댓글 목록 새로고침
                loadComments();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "댓글 작성 실패: " + errorMessage);

                // 버튼 활성화
                sendCommentButton.setEnabled(true);
                sendCommentButton.setAlpha(1.0f);

                showToast("댓글 작성에 실패했습니다.");
            }

            @Override
            public void onLoading(boolean isLoading) {
                // 버튼 상태로 이미 관리하므로 여기서는 처리하지 않음
            }
        }, "댓글 작성");
    }
    // CommentAdapter.OnCommentClickListener 인터페이스 구현
    @Override
    public void onCommentLikeClick(Comment comment) {
        if (networkManager == null) return;

        ApiService.LikeRequest likeRequest = new ApiService.LikeRequest("anonymous");
        Call<ApiService.LikeResponse> call = networkManager.getApiService().toggleCommentLike(comment.getId(), likeRequest);

        NetworkRequestUtility.executeWithRetry(call, new NetworkRequestUtility.NetworkCallback<ApiService.LikeResponse>() {
            @Override
            public void onSuccess(ApiService.LikeResponse result) {
                Log.d(TAG, "댓글 좋아요 처리 성공: " + result.liked + ", 총 " + result.likes + "개");

                // 댓글 객체 업데이트
                comment.setLikes(result.likes);

                // 어댑터 새로고침 (특정 위치만)
                int position = findCommentPosition(comment.getId());
                if (position != -1) {
                    commentAdapter.notifyItemChanged(position);
                }
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "댓글 좋아요 처리 최종 실패: " + errorMessage);
                // ProtocolException의 경우 사용자에게 에러를 보이지 않음
                if (!errorMessage.contains("서버 연결이 불안정")) {
                    showToast("좋아요 처리에 실패했습니다.");
                }
            }

            @Override
            public void onLoading(boolean isLoading) {
                // 댓글 좋아요는 빠른 동작이므로 로딩 표시 안 함
            }
        }, "댓글 좋아요 처리", false); // 로딩 표시 안 함
    }

    // 댓글 위치 찾기 헬퍼 메서드
    private int findCommentPosition(int commentId) {
        if (commentAdapter == null) return -1;

        for (int i = 0; i < commentAdapter.getItemCount(); i++) {
            Comment comment = commentAdapter.getCommentAtPosition(i);
            if (comment != null && comment.getId() == commentId) {
                return i;
            }
        }
        return -1;
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