package kr.ac.kopo.android_school_meal;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class CacheManager {
    private static final String TAG = "CacheManager";
    private static final String PREFS_NAME = "meal_cache";
    private static final String KEY_MEALS = "cached_meals";
    private static final String KEY_CACHE_TIME = "cache_time";
    private static final long CACHE_VALIDITY_DAYS = 7; // 7일
    private static final long CACHE_VALIDITY_MILLIS = CACHE_VALIDITY_DAYS * 24 * 60 * 60 * 1000;

    private SharedPreferences prefs;
    private Gson gson;

    public CacheManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    // 메뉴 데이터를 캐시에 저장
    public void cacheMeals(List<Meal> meals) {
        try {
            String mealsJson = gson.toJson(meals);
            long currentTime = System.currentTimeMillis();

            prefs.edit()
                    .putString(KEY_MEALS, mealsJson)
                    .putLong(KEY_CACHE_TIME, currentTime)
                    .apply();

            Log.d(TAG, "메뉴 데이터 캐시 저장 완료: " + meals.size() + "개 항목");
        } catch (Exception e) {
            Log.e(TAG, "메뉴 캐시 저장 실패", e);
        }
    }

    // 캐시에서 메뉴 데이터 불러오기
    public List<Meal> getCachedMeals() {
        try {
            String mealsJson = prefs.getString(KEY_MEALS, null);
            if (mealsJson == null) {
                Log.d(TAG, "캐시된 데이터가 없음");
                return null;
            }

            long cacheTime = prefs.getLong(KEY_CACHE_TIME, 0);
            long currentTime = System.currentTimeMillis();

            if (currentTime - cacheTime > CACHE_VALIDITY_MILLIS) {
                Log.d(TAG, "캐시가 만료됨");
                clearCache();
                return null;
            }

            Type listType = new TypeToken<List<Meal>>(){}.getType();
            List<Meal> meals = gson.fromJson(mealsJson, listType);

            Log.d(TAG, "캐시에서 메뉴 데이터 로드: " + (meals != null ? meals.size() : 0) + "개 항목");
            return meals;
        } catch (Exception e) {
            Log.e(TAG, "캐시 데이터 로드 실패", e);
            clearCache(); // 손상된 캐시 삭제
            return null;
        }
    }

    // 만료된 캐시도 가져오기 (네트워크 실패 시 폴백용)
    public List<Meal> getExpiredCachedMeals() {
        try {
            String mealsJson = prefs.getString(KEY_MEALS, null);
            if (mealsJson == null) {
                Log.d(TAG, "만료 캐시 파일이 존재하지 않음");
                return null;
            }

            Type listType = new TypeToken<List<Meal>>(){}.getType();
            List<Meal> meals = gson.fromJson(mealsJson, listType);

            long cacheTime = prefs.getLong(KEY_CACHE_TIME, 0);
            long daysDifference = (System.currentTimeMillis() - cacheTime) / (24 * 60 * 60 * 1000);

            Log.d(TAG, "만료된 캐시에서 메뉴 데이터 로드: " + (meals != null ? meals.size() : 0) +
                    "개 항목 (" + daysDifference + "일 전)");

            return meals;
        } catch (Exception e) {
            Log.e(TAG, "만료 캐시 데이터 로드 실패", e);
            return null;
        }
    }

    // 캐시 상태 확인
    public CacheStatus getCacheStatus() {
        try {
            String mealsJson = prefs.getString(KEY_MEALS, null);
            boolean exists = mealsJson != null;

            if (!exists) {
                return new CacheStatus(false, null, 0, false, 0);
            }

            long cacheTime = prefs.getLong(KEY_CACHE_TIME, 0);
            Date cachedAt = new Date(cacheTime);
            long currentTime = System.currentTimeMillis();
            long daysDifference = (currentTime - cacheTime) / (24 * 60 * 60 * 1000);
            boolean isValid = (currentTime - cacheTime) <= CACHE_VALIDITY_MILLIS;

            Type listType = new TypeToken<List<Meal>>(){}.getType();
            List<Meal> meals = gson.fromJson(mealsJson, listType);
            int itemCount = meals != null ? meals.size() : 0;

            return new CacheStatus(true, cachedAt, itemCount, isValid, daysDifference);
        } catch (Exception e) {
            Log.e(TAG, "캐시 상태 확인 실패", e);
            return new CacheStatus(false, null, 0, false, 0);
        }
    }

    // 캐시 삭제
    public void clearCache() {
        try {
            prefs.edit()
                    .remove(KEY_MEALS)
                    .remove(KEY_CACHE_TIME)
                    .apply();
            Log.d(TAG, "캐시 삭제 완료");
        } catch (Exception e) {
            Log.e(TAG, "캐시 삭제 실패", e);
        }
    }

    // 캐시 상태 클래스
    public static class CacheStatus {
        public final boolean exists;
        public final Date cachedAt;
        public final int itemCount;
        public final boolean isValid;
        public final long daysOld;

        public CacheStatus(boolean exists, Date cachedAt, int itemCount, boolean isValid, long daysOld) {
            this.exists = exists;
            this.cachedAt = cachedAt;
            this.itemCount = itemCount;
            this.isValid = isValid;
            this.daysOld = daysOld;
        }
    }
}