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
 * 생성/수정 작업에는 재시도를 하지 않고, 읽기 작업에만 재시도 적용
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
     * 읽기 전용 작업에 재시도 적용 (기본)
     */
    public static <T> void executeWithRetry(Call<T> call, NetworkCallback<T> callback, String operationName) {
        executeWithRetry(call, callback, operationName, 0, true, true);
    }

    /**
     * 로딩 표시 여부 지정 (재시도 허용)
     */
    public static <T> void executeWithRetry(Call<T> call, NetworkCallback<T> callback, String operationName, boolean showLoading) {
        executeWithRetry(call, callback, operationName, 0, showLoading, true);
    }

    /**
     * 재시도 여부를 명시적으로 지정 (로딩 표시함)
     */
    public static <T> void executeWithRetryControl(Call<T> call, NetworkCallback<T> callback, String operationName, boolean allowRetry) {
        executeWithRetry(call, callback, operationName, 0, true, allowRetry);
    }

    /**
     * 완전한 제어를 위한 메서드
     */
    public static <T> void executeWithFullControl(Call<T> call, NetworkCallback<T> callback, String operationName, boolean showLoading, boolean allowRetry) {
        executeWithRetry(call, callback, operationName, 0, showLoading, allowRetry);
    }

    private static <T> void executeWithRetry(Call<T> call, NetworkCallback<T> callback, String operationName, int retryCount, boolean showLoading, boolean allowRetry) {
        if (retryCount > MAX_RETRY_COUNT) {
            Log.w(TAG, operationName + " 최대 재시도 횟수 초과");
            callback.onLoading(false);
            callback.onFailure("네트워크 연결이 불안정합니다. 잠시 후 다시 시도해주세요.");
            return;
        }

        Log.d(TAG, operationName + " 시도 " + (retryCount + 1) + "회" + (allowRetry ? " (재시도 허용)" : " (재시도 없음)"));

        // 첫 번째 시도가 아니면 지연 후 실행
        long delay = retryCount > 0 ? RETRY_DELAYS[retryCount] : 0;

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
                        String errorMessage = "서버 응답 오류: " + response.code();

                        // 재시도 허용되고 4xx 클라이언트 오류가 아닌 경우만 재시도
                        if (allowRetry && response.code() >= 500) {
                            handleRetry(call, callback, operationName, retryCount, showLoading, allowRetry, errorMessage);
                        } else {
                            // 클라이언트 오류(4xx)이거나 재시도 비허용 시 즉시 실패
                            callback.onFailure(errorMessage);
                        }
                    }
                }

                @Override
                public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                    Log.w(TAG, operationName + " 네트워크 오류 (시도 " + (retryCount + 1) + "회): " + t.getMessage());

                    String errorMessage = getErrorMessage(t);

                    // 재시도 허용 여부 확인
                    if (allowRetry) {
                        // ProtocolException은 자동 재시도, 다른 오류도 재시도
                        if (t instanceof java.net.ProtocolException) {
                            // ProtocolException은 사용자에게 보이지 않게 재시도
                            handleRetry(call, callback, operationName, retryCount, false, allowRetry, errorMessage);
                        } else {
                            // 다른 오류도 재시도하되, 마지막에만 에러 표시
                            handleRetry(call, callback, operationName, retryCount, showLoading, allowRetry, errorMessage);
                        }
                    } else {
                        // 재시도 비허용 시 즉시 실패
                        Log.d(TAG, operationName + " 재시도 비허용으로 즉시 실패 처리");
                        callback.onLoading(false);
                        callback.onFailure(errorMessage);
                    }
                }
            });
        }, delay);
    }

    private static <T> void handleRetry(Call<T> originalCall, NetworkCallback<T> callback, String operationName, int retryCount, boolean showLoading, boolean allowRetry, String errorMessage) {
        if (retryCount < MAX_RETRY_COUNT) {
            Log.d(TAG, operationName + " " + RETRY_DELAYS[retryCount + 1] + "ms 후 재시도 (" + (retryCount + 1) + "/" + MAX_RETRY_COUNT + ")");
            executeWithRetry(originalCall, callback, operationName, retryCount + 1, showLoading, allowRetry);
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
     * 생성/수정 작업용 간편 메서드 (재시도 없음)
     */
    public static <T> void executeOnce(Call<T> call, NetworkCallback<T> callback, String operationName) {
        executeWithRetry(call, callback, operationName, 0, true, false);
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