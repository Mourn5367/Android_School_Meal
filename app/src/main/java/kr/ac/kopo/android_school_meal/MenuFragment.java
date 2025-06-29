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


    private boolean isNetworkAvailable = true;
    private boolean isUsingFreshData = false;

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

            // 캐시 데이터 표시
            displayMeals(cachedMeals);

            // 캐시를 사용 중이므로 최신 데이터 플래그 해제
            isUsingFreshData = false;

            // 백그라운드에서 업데이트 시도
            updateCacheInBackground();

            // 캐시 상태 업데이트
            updateCacheStatus();
            return;
        }

        // 2. 캐시가 없으면 네트워크에서 가져오기
        setLoading(true);
        fetchMealsFromNetwork();
    }


    private void fetchMealsFromNetwork() {
        networkManager.getApiService().getMeals().enqueue(new Callback<List<Meal>>() {
            @Override
            public void onResponse(@NonNull Call<List<Meal>> call, @NonNull Response<List<Meal>> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    List<Meal> meals = response.body();
                    Log.d(TAG, "네트워크에서 메뉴 데이터 가져오기 성공: " + meals.size() + "개");

                    // 네트워크 연결 성공 및 최신 데이터 플래그 설정
                    isNetworkAvailable = true;
                    isUsingFreshData = true;

                    // 캐시에 저장
                    cacheManager.cacheMeals(meals);

                    displayMeals(meals);
                    updateCacheStatus();
                } else {
                    Log.e(TAG, "네트워크 응답 오류: " + response.code());
                    isNetworkAvailable = false;
                    handleNetworkError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Meal>> call, @NonNull Throwable t) {
                setLoading(false);
                Log.e(TAG, "네트워크 요청 실패", t);

                // 네트워크 연결 실패 플래그 설정
                isNetworkAvailable = false;
                isUsingFreshData = false;

                handleNetworkError();
            }
        });
    }

    private void handleNetworkError(String errorMessage) {
        // 네트워크 실패 시 만료된 캐시라도 사용
        List<Meal> fallbackMeals = cacheManager.getExpiredCachedMeals();
        if (fallbackMeals != null && !fallbackMeals.isEmpty()) {
            Log.d(TAG, "만료된 캐시 데이터 사용");
            displayMeals(fallbackMeals);
            showToast("인터넷 연결을 확인해주세요. 이전 데이터를 표시합니다.");
        } else {
            showErrorView();
            showToast(errorMessage);
        }
        updateCacheStatus();
    }
    // 오류 처리 메서드 개선
    private void handleNetworkError() {
        handleNetworkError("인터넷 연결을 확인해주세요.");
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
        networkManager.getApiService().getMeals().enqueue(new Callback<List<Meal>>() {
            @Override
            public void onResponse(@NonNull Call<List<Meal>> call, @NonNull Response<List<Meal>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Meal> newMeals = response.body();

                    // 백그라운드 업데이트 성공
                    isNetworkAvailable = true;
                    isUsingFreshData = true;

                    // 캐시 저장
                    cacheManager.cacheMeals(newMeals);

                    // 새로운 데이터로 UI 업데이트
                    displayMeals(newMeals);

                    Log.d(TAG, "백그라운드 캐시 업데이트 완료");
                    updateCacheStatus();
                } else {
                    Log.w(TAG, "백그라운드 캐시 업데이트 응답 오류: " + response.code());
                    isNetworkAvailable = false;
                    updateCacheStatus();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<Meal>> call, @NonNull Throwable t) {
                Log.w(TAG, "백그라운드 캐시 업데이트 실패: " + t.getMessage());
                isNetworkAvailable = false;
                updateCacheStatus();
            }
        });
    }
    private void updateCacheWithRetry(int retryCount) {
        final int maxRetries = 2;
        final long[] retryDelays = {0, 2000, 5000};

        if (retryCount > maxRetries) {
            Log.w(TAG, "백그라운드 캐시 업데이트 최대 재시도 횟수 초과");
            return;
        }

        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            networkManager.getApiService().getMeals().enqueue(new Callback<List<Meal>>() {
                @Override
                public void onResponse(@NonNull Call<List<Meal>> call, @NonNull Response<List<Meal>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        cacheManager.cacheMeals(response.body());
                        Log.d(TAG, "백그라운드 캐시 업데이트 완료 (시도 " + (retryCount + 1) + "회)");
                        updateCacheStatus();
                    } else {
                        Log.w(TAG, "백그라운드 캐시 업데이트 응답 오류: " + response.code() + " (시도 " + (retryCount + 1) + "회)");
                        if (retryCount < maxRetries) {
                            updateCacheWithRetry(retryCount + 1);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<List<Meal>> call, @NonNull Throwable t) {
                    Log.w(TAG, "백그라운드 캐시 업데이트 실패 (시도 " + (retryCount + 1) + "회): " + t.getMessage());

                    // 재시도 (ProtocolException은 재시도하지 않음)
                    if (retryCount < maxRetries && !(t instanceof java.net.ProtocolException)) {
                        updateCacheWithRetry(retryCount + 1);
                    }
                }
            });
        }, retryDelays[retryCount]);
    }
    private void updateCacheStatus() {
        if (getActivity() == null) return;

        CacheManager.CacheStatus status = cacheManager.getCacheStatus();

        // 캐시가 없으면 아무것도 표시하지 않음
        if (!status.exists) {
            cacheStatusCard.setVisibility(View.GONE);
            return;
        }

        // 최신 데이터를 사용 중이면 상태 카드 숨김
        if (isUsingFreshData && isNetworkAvailable) {
            cacheStatusCard.setVisibility(View.GONE);
            return;
        }

        // 캐시가 유효하고 네트워크가 불안정하거나 오프라인인 경우
        if (status.isValid && !isNetworkAvailable) {
            cacheStatusCard.setVisibility(View.VISIBLE);
            cacheStatusText.setText("오프라인 모드");
            cacheStatusCard.setCardBackgroundColor(
                    getResources().getColor(R.color.cache_valid_background, null)
            );
            return;
        }

        // 캐시가 만료된 경우 (1일 이상 된 데이터)
        if (!status.isValid && status.daysOld > 0) {
            cacheStatusCard.setVisibility(View.VISIBLE);
            cacheStatusText.setText(status.daysOld + "일 전 데이터 사용 중");
            cacheStatusCard.setCardBackgroundColor(
                    getResources().getColor(R.color.cache_expired_background, null)
            );
            return;
        }

        // 기타 경우는 숨김
        cacheStatusCard.setVisibility(View.GONE);
    }

    public void refreshData() {
        if (isRefreshing) return;

        Log.d(TAG, "새로고침 시작");
        setLoading(true);

        // 새로고침이므로 네트워크에서 직접 가져오기
        fetchMealsFromNetwork();
    }

    public void forceRefresh() {
        if (isRefreshing) return;

        Log.d(TAG, "강제 새로고침 시작");

        // 캐시 삭제 및 플래그 리셋
        cacheManager.clearCache();
        isUsingFreshData = false;
        isNetworkAvailable = true; // 강제 새로고침이므로 네트워크 시도

        setLoading(true);
        fetchMealsFromNetwork();
    }

    private void setLoading(boolean loading) {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(loading);
        }
        isRefreshing = loading;

        Log.d(TAG, "로딩 상태 변경: " + loading);
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
        if (getActivity() != null && isAdded()) {
            getActivity().runOnUiThread(() -> {
                Toast.makeText(getActivity(), message, Toast.LENGTH_SHORT).show();
            });
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
