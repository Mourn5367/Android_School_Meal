package kr.ac.kopo.android_school_meal;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

public class NetworkManager {
    private static NetworkManager instance;
    private ApiService apiService;

    private NetworkManager() {
        // HTTP 로깅 인터셉터 설정
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        // OkHttpClient 설정 - 타임아웃 증가 및 재시도 활성화
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)    // 연결 타임아웃 증가
                .readTimeout(60, TimeUnit.SECONDS)       // 읽기 타임아웃 증가
                .writeTimeout(60, TimeUnit.SECONDS)      // 쓰기 타임아웃 증가
                .retryOnConnectionFailure(true)          // 연결 실패 시 재시도 활성화
                .build();

        // Gson 설정
        Gson gson = new GsonBuilder()
                .setLenient()
                .create();

        // Retrofit 설정
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(ApiConfig.BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public static synchronized NetworkManager getInstance() {
        if (instance == null) {
            instance = new NetworkManager();
        }
        return instance;
    }

    public ApiService getApiService() {
        return apiService;
    }
}