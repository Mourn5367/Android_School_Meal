package kr.ac.kopo.android_school_meal;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;


import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private MenuFragment menuFragment;
    private CacheManager cacheManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 툴바 설정
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("학식 메뉴 앱");
        }

        // 캐시 매니저 초기화
        cacheManager = new CacheManager(this);

        // 메뉴 프래그먼트 로드
        if (savedInstanceState == null) {
            loadMenuFragment();
        }
    }

    private void loadMenuFragment() {
        menuFragment = new MenuFragment();

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragment_container, menuFragment);
        transaction.commit();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_refresh) {
            // 강제 새로고침
            if (menuFragment != null) {
                menuFragment.forceRefresh();
            }
            return true;
        } else if (id == R.id.action_clear_cache) {
            // 캐시 삭제
            cacheManager.clearCache();
            if (menuFragment != null) {
                menuFragment.refreshData();
            }
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

        if (currentFragment instanceof MenuFragment) {
            // 메인 화면에서 뒤로가기 시 앱 종료
            super.onBackPressed();
        } else {
            // 다른 프래그먼트에서는 메뉴로 돌아가기
            loadMenuFragment();
        }
    }
}