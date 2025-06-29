package kr.ac.kopo.android_school_meal;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * 네트워크 요청을 위한 통합 재시도 유틸리티
 * ProtocolException 등의 네트워크 오류에 대해 자동 재시도 기능 제공
 */
public class NetworkRequestUtility {
    private static final String TAG = "NetworkRequestUtility";
    private static final int MAX_RETRY_COUNT = 3;
    private static final long[] RETRY_DELAYS = {0, 1000, 2500, 4000}; // 재시도 간격 (ms)

    public interface NetworkCallback<T> {
        void onSuccess(T result);
        void onFailure(String errorMessage);
        void onLoading(boolean isLoading); // 선택적 로딩 상태 콜백
    }

    /**
     * 자동 재시도가 포함된 네트워크 요청 실행
     * @param call Retrofit Call 객체
     * @param callback 결과 콜백
     * @param operationName 작업 이름 (로깅용)
     * @param <T> 응답 타입
     */
    public static <T> void executeWithRetry(Call<T> call, NetworkCallback<T> callback, String operationName) {
        executeWithRetry(call, callback, operationName, 0, true);
    }

    /**
     * 자동 재시도가 포함된 네트워크 요청 실행 (로딩 표시 옵션)
     * @param call Retrofit Call 객체
     * @param callback 결과 콜백
     * @param operationName 작업 이름 (로깅용)
     * @param showLoading 로딩 표시 여부
     * @param <T> 응답 타입
     */
    public static <T> void executeWithRetry(Call<T> call, NetworkCallback<T> callback, String operationName, boolean showLoading) {
        executeWithRetry(call, callback, operationName, 0, showLoading);
    }

    private static <T> void executeWithRetry(Call<T> call, NetworkCallback<T> callback, String operationName, int retryCount, boolean showLoading) {
        if (retryCount > MAX_RETRY_COUNT) {
            Log.w(TAG, operationName + " 최대 재시도 횟수 초과");
            callback.onLoading(false);
            callback.onFailure("네트워크 연결이 불안정합니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        Log.d(TAG, operationName + " 시도 " + (retryCount + 1) + "회");

        // 첫 번째 시도가 아니면 지연 후 실행
        long delay = RETRY_DELAYS[retryCount];

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 로딩 표시 (첫 번째 시도이거나 마지막 시도일 때만)
            if (showLoading && (retryCount == 0 || retryCount >= MAX_RETRY_COUNT)) {
                callback.onLoading(true);
            }

            // Call 객체는 한 번만 사용 가능하므로 클론 생성
            Call<T> clonedCall = call.clone();

            clonedCall.enqueue(new Callback<T>() {
                @Override
                public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
                    callback.onLoading(false);

                    if (response.isSuccessful() && response.body() != null) {
                        Log.d(TAG, operationName + " 성공 (시도 " + (retryCount + 1) + "회)");
                        callback.onSuccess(response.body());
                    } else {
                        Log.e(TAG, operationName + " 응답 오류: " + response.code() + " (시도 " + (retryCount + 1) + "회)");
                        handleRetry(call, callback, operationName, retryCount, showLoading, "서버 응답 오류: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                    Log.w(TAG, operationName + " 네트워크 오류 (시도 " + (retryCount + 1) + "회): " + t.getMessage());

                    String errorMessage = getErrorMessage(t);

                    // ProtocolException은 자동 재시도, 다른 오류도 재시도
                    if (t instanceof java.net.ProtocolException) {
                        // ProtocolException은 사용자에게 보이지 않게 재시도
                        handleRetry(call, callback, operationName, retryCount, false, errorMessage);
                    } else {
                        // 다른 오류도 재시도하되, 마지막에만 에러 표시
                        handleRetry(call, callback, operationName, retryCount, showLoading, errorMessage);
                    }
                }
            });
        }, delay);
    }

    private static <T> void handleRetry(Call<T> originalCall, NetworkCallback<T> callback, String operationName, int retryCount, boolean showLoading, String errorMessage) {
        if (retryCount < MAX_RETRY_COUNT) {
            Log.d(TAG, operationName + " " + RETRY_DELAYS[retryCount + 1] + "ms 후 재시도 (" + (retryCount + 1) + "/" + MAX_RETRY_COUNT + ")");
            executeWithRetry(originalCall, callback, operationName, retryCount + 1, showLoading);
        } else {
            Log.e(TAG, operationName + " 최대 재시도 횟수 초과: " + errorMessage);
            callback.onLoading(false);
            callback.onFailure(errorMessage);
        }
    }

    private static String getErrorMessage(Throwable t) {
        if (t instanceof java.net.ProtocolException) {
            return "서버 연결이 불안정합니다.";
        } else if (t instanceof java.net.SocketTimeoutException) {
            return "응답 시간이 초과되었습니다. 네트워크 상태를 확인해주세요.";
        } else if (t instanceof java.net.UnknownHostException) {
            return "인터넷 연결을 확인해주세요.";
        } else {
            return "네트워크 오류가 발생했습니다.";
        }
    }

    /**
     * 간단한 콜백 구현체 (로딩 상태를 무시하는 경우)
     */
    public static abstract class SimpleNetworkCallback<T> implements NetworkCallback<T> {
        @Override
        public void onLoading(boolean isLoading) {
            // 기본적으로 아무것도 하지 않음
        }
    }
}