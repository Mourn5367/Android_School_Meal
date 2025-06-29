package kr.ac.kopo.android_school_meal;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MenuFragment extends Fragment implements MealGroupAdapter.OnMealClickListener {
    private static final String TAG = "MenuFragment";

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefreshLayout;
    private MaterialCardView cacheStatusCard;
    private MaterialTextView cacheStatusText;
    private View emptyView;
    private View errorView;

    private MealGroupAdapter adapter;
    private CacheManager cacheManager;
    private NetworkManager networkManager;

    private boolean isRefreshing = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        cacheManager = new CacheManager(requireContext());
        networkManager = NetworkManager.getInstance();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        initViews(view);
        setupRecyclerView();
        setupSwipeRefresh();

        loadMeals();
        updateCacheStatus();
    }

    private void initViews(View view) {
        recyclerView = view.findViewById(R.id.recyclerView);
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        cacheStatusCard = view.findViewById(R.id.cacheStatusCard);
        cacheStatusText = view.findViewById(R.id.cacheStatusText);
        emptyView = view.findViewById(R.id.emptyView);
        errorView = view.findViewById(R.id.errorView);

        // 에러 뷰의 재시도 버튼
        view.findViewById(R.id.retryButton).setOnClickListener(v -> refreshData());
        view.findViewById(R.id.forceRefreshButton).setOnClickListener(v -> forceRefresh());
    }

    private void setupRecyclerView() {
        adapter = new MealGroupAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener(() -> refreshData());
        swipeRefreshLayout.setColorSchemeResources(
                R.color.colorPrimary,
                R.color.colorAccent
        );
    }

    public void loadMeals() {
        if (isRefreshing) return;

        Log.d(TAG, "메뉴 데이터 요청 시작");

        // 1. 먼저 캐시에서 시도
        List<Meal> cachedMeals = cacheManager.getCachedMeals();
        if (cachedMeals != null && !cachedMeals.isEmpty()) {
            Log.d(TAG, "캐시에서 메뉴 데이터 로드 성공");
            displayMeals(cachedMeals);
            updateCacheInBackground();
            return;
        }

        // 2. 캐시가 없으면 네트워크에서 가져오기
        fetchMealsFromNetwork();
    }

    private void fetchMealsFromNetwork() {
        setLoading(true);

        networkManager.getApiService().getMeals().enqueue(new Callback<List<Meal>>() {
            @Override
            public void onResponse(@NonNull Call<List<Meal>> call, @NonNull Response<List<Meal>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Meal> meals = response.body();
                    Log.d(TAG, "네트워크에서 메뉴 데이터 가져오기 성공: " + meals.size() + "개");

                    // 캐시에 저장
                    cacheManager.cacheMeals(meals);

                    displayMeals(meals);
                    updateCacheStatus();
                } else {
                    Log.e(TAG, "네트워크 응답 오류: " + response.code());
                    handleNetworkError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Meal>> call, @NonNull Throwable t) {
                setLoading(false);
                Log.e(TAG, "네트워크 요청 실패", t);
                handleNetworkError();
            }
        });
    }

    private void handleNetworkError() {
        // 3. 네트워크 실패 시 만료된 캐시라도 사용
        List<Meal> fallbackMeals = cacheManager.getExpiredCachedMeals();
        if (fallbackMeals != null && !fallbackMeals.isEmpty()) {
            Log.d(TAG, "만료된 캐시 데이터 사용");
            displayMeals(fallbackMeals);
            showToast("인터넷 연결을 확인해주세요. 이전 데이터를 표시합니다.");
        } else {
            showErrorView();
            showToast("인터넷 연결을 확인해주세요.");
        }
        updateCacheStatus();
    }

    private void displayMeals(List<Meal> meals) {
        Map<String, List<Meal>> groupedMeals = groupMealsByDate(meals);
        adapter.updateData(groupedMeals);

        if (groupedMeals.isEmpty()) {
            showEmptyView();
        } else {
            showContent();
        }
    }

    private Map<String, List<Meal>> groupMealsByDate(List<Meal> meals) {
        Map<String, List<Meal>> grouped = new HashMap<>();
        Date today = new Date();

        for (Meal meal : meals) {
            Date mealDate = DateUtils.parseDate(meal.getDate());
            if (mealDate == null) continue;

            // 주말 제외하고 현재 날짜 이후만
            if (isWeekend(mealDate) || mealDate.before(subtractDays(today, 1))) {
                continue;
            }

            String dateKey = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(mealDate);

            if (!grouped.containsKey(dateKey)) {
                grouped.put(dateKey, new ArrayList<>());
            }
            grouped.get(dateKey).add(meal);
        }

        // 각 날짜의 메뉴를 식사 순서대로 정렬
        for (List<Meal> mealsInDay : grouped.values()) {
            mealsInDay.sort((a, b) -> {
                Map<String, Integer> order = new HashMap<>();
                order.put("아침", 1);
                order.put("점심", 2);
                order.put("저녁", 3);

                return order.getOrDefault(a.getMealType(), 4)
                        .compareTo(order.getOrDefault(b.getMealType(), 4));
            });
        }

        return grouped;
    }

    private boolean isWeekend(Date date) {
        SimpleDateFormat format = new SimpleDateFormat("u", Locale.getDefault());
        int dayOfWeek = Integer.parseInt(format.format(date));
        return dayOfWeek == 6 || dayOfWeek == 7; // 토요일(6), 일요일(7)
    }

    private Date subtractDays(Date date, int days) {
        return new Date(date.getTime() - (days * 24 * 60 * 60 * 1000L));
    }

    private void updateCacheInBackground() {
        // 백그라운드에서 캐시 업데이트
        networkManager.getApiService().getMeals().enqueue(new Callback<List<Meal>>() {
            @Override
            public void onResponse(@NonNull Call<List<Meal>> call, @NonNull Response<List<Meal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    cacheManager.cacheMeals(response.body());
                    Log.d(TAG, "백그라운드 캐시 업데이트 완료");
                    updateCacheStatus();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Meal>> call, @NonNull Throwable t) {
                Log.w(TAG, "백그라운드 캐시 업데이트 실패", t);
            }
        });
    }

    private void updateCacheStatus() {
        if (getActivity() == null) return;

        CacheManager.CacheStatus status = cacheManager.getCacheStatus();

        if (!status.exists) {
            cacheStatusCard.setVisibility(View.GONE);
            return;
        }

        cacheStatusCard.setVisibility(View.VISIBLE);

        if (status.isValid) {
            cacheStatusText.setText("오프라인 모드");
            cacheStatusCard.setCardBackgroundColor(
                    getResources().getColor(R.color.cache_valid_background, null)
            );
        } else {
            cacheStatusText.setText(status.daysOld + "일 전 데이터 사용 중");
            cacheStatusCard.setCardBackgroundColor(
                    getResources().getColor(R.color.cache_expired_background, null)
            );
        }
    }

    public void refreshData() {
        loadMeals();
    }

    public void forceRefresh() {
        cacheManager.clearCache();
        fetchMealsFromNetwork();
    }

    private void setLoading(boolean loading) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(loading);
        }
        isRefreshing = loading;
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

    private void showErrorView() {
        recyclerView.setVisibility(View.GONE);
        emptyView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onMealClick(Meal meal, String date) {
        Intent intent = new Intent(getContext(), MealBoardActivity.class);
        intent.putExtra("meal_id", meal.getId());
        intent.putExtra("meal_type", meal.getMealType());
        intent.putExtra("meal_content", meal.getContent());
        intent.putExtra("meal_date", date);
        startActivity(intent);
    }
}
