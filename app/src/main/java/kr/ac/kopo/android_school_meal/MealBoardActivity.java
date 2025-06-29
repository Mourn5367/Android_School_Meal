package kr.ac.kopo.android_school_meal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MealBoardActivity extends AppCompatActivity implements PostAdapter.OnPostClickListener {
    private static final String TAG = "MealBoardActivity";
    private static final int REQUEST_CREATE_POST = 1001;

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialTextView mealInfoText;
    private MaterialTextView mealContentText;
    private MaterialTextView postCountText;
    private View emptyView;
    private View errorView;
    private FloatingActionButton fabCreatePost;

    private PostAdapter adapter;
    private NetworkManager networkManager;

    // 메뉴 정보
    private int mealId;
    private String mealType;
    private String mealContent;
    private String mealDate;

    private static final int MAX_RETRY_COUNT = 3; // 최대 3번 재시도
    private static final long[] RETRY_DELAYS = {0, 1500, 3000, 5000}; // 재시도 간격 (ms)
    private int currentRetryCount = 0;
    private boolean isLoadingPosts = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meal_board);

        // 인텐트에서 데이터 받기
        getIntentData();

        // 뷰 초기화
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSwipeRefresh();
        setupFab();

        // 네트워크 매니저 초기화
        networkManager = NetworkManager.getInstance();

        // 메뉴 정보 표시
        displayMealInfo();

        // 게시글 로드
        showProgressiveLoading();
        loadPosts();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        mealId = intent.getIntExtra("meal_id", 0);
        mealType = intent.getStringExtra("meal_type");
        mealContent = intent.getStringExtra("meal_content");
        mealDate = intent.getStringExtra("meal_date");

        Log.d(TAG, "메뉴 정보: ID=" + mealId + ", 타입=" + mealType + ", 날짜=" + mealDate);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        mealInfoText = findViewById(R.id.mealInfoText);
        mealContentText = findViewById(R.id.mealContentText);
        postCountText = findViewById(R.id.postCountText);
        emptyView = findViewById(R.id.emptyView);
        errorView = findViewById(R.id.errorView);
        fabCreatePost = findViewById(R.id.fabCreatePost);

        // 에러 뷰의 재시도 버튼
        findViewById(R.id.retryButton).setOnClickListener(v -> loadPosts());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(mealType + " 메뉴");
        }
    }

    private void setupRecyclerView() {
        adapter = new PostAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> {
            // 수동 새로고침 시 재시도 카운터 리셋
            currentRetryCount = 0;
            isLoadingPosts = false;
            loadPostsWithRetry();
        });

        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent
        );
    }

    private void setupFab() {
        fabCreatePost.setOnClickListener(v -> {
            Intent intent = new Intent(this, PostCreateActivity.class);
            intent.putExtra("meal_id", mealId);
            intent.putExtra("meal_type", mealType);
            intent.putExtra("meal_content", mealContent);
            intent.putExtra("meal_date", mealDate);
            startActivityForResult(intent, REQUEST_CREATE_POST);
        });
    }

    private void displayMealInfo() {
        // 날짜 및 식사 유형 정보
        String displayDate = DateUtils.formatForDisplay(mealDate);
        mealInfoText.setText(displayDate + " " + mealType);

        // 메뉴 내용을 리스트 형태로 표시
        String formattedContent = formatMealContent(mealContent);
        mealContentText.setText(formattedContent);
    }

    private String formatMealContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "메뉴 정보가 없습니다.";
        }

        String[] items = content.split(",");
        StringBuilder formatted = new StringBuilder();

        for (String item : items) {
            formatted.append("• ").append(item.trim()).append("\n");
        }

        return formatted.toString().trim();
    }

    private void loadPosts() {
        currentRetryCount = 0; // 재시도 카운터 초기화
        loadPostsWithRetry();
    }
    private void loadPostsWithRetry() {
        if (isLoadingPosts) return;

        String formattedDate = DateUtils.formatToApiDate(mealDate);
        Log.d(TAG, "게시글 조회 시도 " + (currentRetryCount + 1) + "회 - 날짜: " + formattedDate + ", 식사: " + mealType);

        isLoadingPosts = true;

        // 첫 번째 시도가 아니면 지연 후 실행
        long delay = RETRY_DELAYS[currentRetryCount];

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            // 사용자에게 로딩 표시 (첫 번째 시도이거나 마지막 시도일 때만)
            if (currentRetryCount == 0 || currentRetryCount >= MAX_RETRY_COUNT) {
                swipeRefreshLayout.setRefreshing(true);
            }

            networkManager.getApiService().getPosts(formattedDate, mealType)
                    .enqueue(new Callback<List<Post>>() {
                        @Override
                        public void onResponse(@NonNull Call<List<Post>> call, @NonNull Response<List<Post>> response) {
                            isLoadingPosts = false;
                            swipeRefreshLayout.setRefreshing(false);

                            if (response.isSuccessful() && response.body() != null) {
                                List<Post> posts = response.body();
                                Log.d(TAG, "게시글 조회 성공: " + posts.size() + "개 (시도 " + (currentRetryCount + 1) + "회)");
                                displayPosts(posts);
                                currentRetryCount = 0; // 성공 시 카운터 리셋
                            } else {
                                Log.e(TAG, "게시글 조회 실패: " + response.code() + " (시도 " + (currentRetryCount + 1) + "회)");
                                handleLoadError("서버 응답 오류: " + response.code());
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<List<Post>> call, @NonNull Throwable t) {
                            isLoadingPosts = false;
                            Log.w(TAG, "게시글 조회 네트워크 오류 (시도 " + (currentRetryCount + 1) + "회): " + t.getMessage());

                            // ProtocolException인 경우 특별 처리
                            if (t instanceof java.net.ProtocolException) {
                                handleProtocolException();
                            } else {
                                handleLoadError("네트워크 오류: " + t.getMessage());
                            }
                        }
                    });
        }, delay);
    }
    private void handleProtocolException() {
        // ProtocolException은 자동 재시도
        if (currentRetryCount < MAX_RETRY_COUNT) {
            currentRetryCount++;
            Log.d(TAG, "ProtocolException 발생, " + RETRY_DELAYS[currentRetryCount] + "ms 후 자동 재시도 (" + currentRetryCount + "/" + MAX_RETRY_COUNT + ")");

            // 사용자에게는 로딩 중이라고 표시 (에러 메시지 없음)
            if (currentRetryCount == 1) {
                // 첫 번째 재시도 시에만 로딩 표시
                swipeRefreshLayout.setRefreshing(true);
            }

            loadPostsWithRetry();
        } else {
            // 최대 재시도 횟수 초과 시에만 에러 표시
            swipeRefreshLayout.setRefreshing(false);
            Log.e(TAG, "ProtocolException 최대 재시도 횟수 초과");
            showError("연결이 불안정합니다. 새로고침을 시도해주세요.");
        }
    }
    private void handleLoadError(String errorMessage) {
        // 일반적인 네트워크 오류 처리
        if (currentRetryCount < MAX_RETRY_COUNT) {
            currentRetryCount++;
            Log.d(TAG, "네트워크 오류, " + RETRY_DELAYS[currentRetryCount] + "ms 후 재시도 (" + currentRetryCount + "/" + MAX_RETRY_COUNT + ")");
            loadPostsWithRetry();
        } else {
            // 최대 재시도 횟수 초과 시에만 에러 표시
            swipeRefreshLayout.setRefreshing(false);
            Log.e(TAG, "최대 재시도 횟수 초과: " + errorMessage);
            showError("게시글을 불러올 수 없습니다. 새로고침을 시도해주세요.");
        }
    }

    private void displayPosts(List<Post> posts) {
        adapter.updateData(posts);
        updatePostCount(posts.size());

        if (posts.isEmpty()) {
            showEmptyView();
        } else {
            showContent();
        }
    }

    private void updatePostCount(int count) {
        postCountText.setText("게시글 " + count + "개");
    }

    private void showContent() {
        recyclerView.setVisibility(View.VISIBLE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    private void showEmptyView() {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);
    }

    private void showError(String message) {
        // 에러 뷰 대신 Toast만 표시하여 덜 침입적으로 만들기
        if (getCurrentFocus() != null) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        }

        // 기존 데이터가 있으면 그대로 유지
        if (adapter.getItemCount() > 0) {
            showContent();
        } else {
            showEmptyView();
        }
    }
    // 더 나은 사용자 경험을 위한 점진적 로딩
    private void showProgressiveLoading() {
        // 0.5초 후에 로딩 표시 (즉각적인 응답을 위해)
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (isLoadingPosts && currentRetryCount == 0) {
                swipeRefreshLayout.setRefreshing(true);
            }
        }, 500);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.board_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_refresh) {
            loadPosts();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CREATE_POST && resultCode == RESULT_OK) {
            // 게시글 작성 후 목록 새로고침
            loadPosts();
        }
    }

    @Override
    public void onPostClick(Post post) {
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("post_id", post.getId());
        intent.putExtra("post_title", post.getTitle());
        intent.putExtra("post_author", post.getAuthor());
        intent.putExtra("post_content", post.getContent());
        intent.putExtra("post_created_at", post.getCreatedAt());
        intent.putExtra("post_likes", post.getLikes());
        intent.putExtra("post_comment_count", post.getCommentCount());
        intent.putExtra("post_image_url", post.getImageUrl());
        startActivity(intent);
    }
}
