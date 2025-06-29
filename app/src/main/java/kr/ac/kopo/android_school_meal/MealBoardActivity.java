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
        swipeRefreshLayout.setOnRefreshListener(this::loadPosts);
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
        String formattedDate = DateUtils.formatToApiDate(mealDate);

        Log.d(TAG, "게시글 조회 - 날짜: " + formattedDate + ", 식사: " + mealType);

        swipeRefreshLayout.setRefreshing(true);

        networkManager.getApiService().getPosts(formattedDate, mealType)
                .enqueue(new Callback<List<Post>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<Post>> call, @NonNull Response<List<Post>> response) {
                        swipeRefreshLayout.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null) {
                            List<Post> posts = response.body();
                            Log.d(TAG, "게시글 조회 성공: " + posts.size() + "개");
                            displayPosts(posts);
                        } else {
                            Log.e(TAG, "게시글 조회 실패: " + response.code());
                            showError("게시글을 불러올 수 없습니다.");
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<Post>> call, @NonNull Throwable t) {
                        swipeRefreshLayout.setRefreshing(false);
                        Log.e(TAG, "게시글 조회 네트워크 오류", t);
                        showError("인터넷 연결을 확인해주세요.");
                    }
                });
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
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
