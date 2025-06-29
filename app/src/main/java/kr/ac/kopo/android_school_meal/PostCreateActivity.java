package kr.ac.kopo.android_school_meal;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.bumptech.glide.Glide;
import gun0912.tedimagepicker.builder.TedImagePicker;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textview.MaterialTextView;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PostCreateActivity extends AppCompatActivity {
    private static final String TAG = "PostCreateActivity";
    private static final int REQUEST_IMAGE_PICKER = 1001;

    private TextInputLayout titleLayout, authorLayout, contentLayout;
    private TextInputEditText titleEdit, authorEdit, contentEdit;
    private MaterialCardView mealInfoCard, imagePreviewCard;
    private MaterialTextView mealInfoText, mealTypeText;
    private ShapeableImageView imagePreview;
    private MaterialButton selectImageButton, removeImageButton;
    private View loadingOverlay;

    private NetworkManager networkManager;

    // 메뉴 정보
    private int mealId;
    private String mealType;
    private String mealContent;
    private String mealDate;

    // 선택된 이미지
    private Uri selectedImageUri;
    private String uploadedImageUrl;

    private boolean isLoading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_create);

        // 인텐트에서 데이터 받기
        getIntentData();

        // 뷰 초기화
        initViews();
        setupToolbar();
        setupButtons();

        // 네트워크 매니저 초기화
        networkManager = NetworkManager.getInstance();

        // 메뉴 정보 표시
        displayMealInfo();
    }

    private void getIntentData() {
        Intent intent = getIntent();
        mealId = intent.getIntExtra("meal_id", 0);
        mealType = intent.getStringExtra("meal_type");
        mealContent = intent.getStringExtra("meal_content");
        mealDate = intent.getStringExtra("meal_date");
    }

    private void initViews() {
        titleLayout = findViewById(R.id.titleLayout);
        authorLayout = findViewById(R.id.authorLayout);
        contentLayout = findViewById(R.id.contentLayout);
        titleEdit = findViewById(R.id.titleEdit);
        authorEdit = findViewById(R.id.authorEdit);
        contentEdit = findViewById(R.id.contentEdit);
        mealInfoCard = findViewById(R.id.mealInfoCard);
        imagePreviewCard = findViewById(R.id.imagePreviewCard);
        mealInfoText = findViewById(R.id.mealInfoText);
        mealTypeText = findViewById(R.id.mealTypeText);
        imagePreview = findViewById(R.id.imagePreview);
        selectImageButton = findViewById(R.id.selectImageButton);
        removeImageButton = findViewById(R.id.removeImageButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("게시글 작성");
        }
    }

    private void setupButtons() {
        selectImageButton.setOnClickListener(v -> showImagePickerOptions());
        removeImageButton.setOnClickListener(v -> removeSelectedImage());
    }

    private void displayMealInfo() {
        String displayDate = DateUtils.formatForDisplay(mealDate);
        mealInfoText.setText(displayDate + " 메뉴");
        mealTypeText.setText(mealType);

        // 식사 유형별 색상 설정
        int color = getMealTypeColor(mealType);
        mealTypeText.setBackgroundColor(color);
    }

    private int getMealTypeColor(String mealType) {
        switch (mealType) {
            case "아침":
                return getResources().getColor(R.color.meal_breakfast, null);
            case "점심":
                return getResources().getColor(R.color.meal_lunch, null);
            case "저녁":
                return getResources().getColor(R.color.meal_dinner, null);
            default:
                return getResources().getColor(R.color.meal_default, null);
        }
    }

    private void showImagePickerOptions() {
        String[] options = {"갤러리에서 선택", "카메라로 촬영"};

        new AlertDialog.Builder(this)
                .setTitle("이미지 선택")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        pickImageFromGallery();
                    } else {
                        takePictureFromCamera();
                    }
                })
                .show();
    }

    private void pickImageFromGallery() {
        TedImagePicker.with(this)
                .start(uri -> {
                    selectedImageUri = uri;
                    displaySelectedImage();
                });
    }

    private void takePictureFromCamera() {
        TedImagePicker.with(this)
                .camera()
                .start(uri -> {
                    selectedImageUri = uri;
                    displaySelectedImage();
                });
    }

    private void checkPermissionsAndPickImage(ImagePicker.Builder builder) {
        // TedImagePicker는 권한을 자동으로 처리하므로 이 메서드는 더 이상 필요하지 않습니다
        // 위의 pickImageFromGallery(), takePictureFromCamera() 메서드가 직접 호출됩니다
    }

    // onActivityResult 메서드는 TedImagePicker가 콜백으로 처리하므로 제거할 수 있습니다

    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .into(imagePreview);

            imagePreviewCard.setVisibility(View.VISIBLE);
            selectImageButton.setText("이미지 변경");
            removeImageButton.setVisibility(View.VISIBLE);
        }
    }

    private void removeSelectedImage() {
        selectedImageUri = null;
        uploadedImageUrl = null;
        imagePreviewCard.setVisibility(View.GONE);
        selectImageButton.setText("이미지 추가");
        removeImageButton.setVisibility(View.GONE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.post_create_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.action_save) {
            savePost();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void savePost() {
        if (isLoading) return;

        // 입력 검증
        if (!validateInput()) return;

        setLoading(true);

        // 이미지가 있으면 먼저 업로드
        if (selectedImageUri != null) {
            uploadImageAndSavePost();
        } else {
            createPost(null);
        }
    }

    private boolean validateInput() {
        boolean isValid = true;

        if (TextUtils.isEmpty(titleEdit.getText())) {
            titleLayout.setError("제목을 입력해주세요");
            isValid = false;
        } else {
            titleLayout.setError(null);
        }

        if (TextUtils.isEmpty(authorEdit.getText())) {
            authorLayout.setError("닉네임을 입력해주세요");
            isValid = false;
        } else {
            authorLayout.setError(null);
        }

        if (TextUtils.isEmpty(contentEdit.getText())) {
            contentLayout.setError("내용을 입력해주세요");
            isValid = false;
        } else {
            contentLayout.setError(null);
        }

        return isValid;
    }

    private void uploadImageAndSavePost() {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            String base64Image = bitmapToBase64(bitmap);
            String filename = "image_" + System.currentTimeMillis() + ".jpg";

            ApiService.ImageUploadRequest request = new ApiService.ImageUploadRequest(
                    "data:image/jpeg;base64," + base64Image, filename
            );

            networkManager.getApiService().uploadImage(request)
                    .enqueue(new Callback<ApiService.ImageUploadResponse>() {
                        @Override
                        public void onResponse(@NonNull Call<ApiService.ImageUploadResponse> call,
                                               @NonNull Response<ApiService.ImageUploadResponse> response) {
                            if (response.isSuccessful() && response.body() != null) {
                                uploadedImageUrl = response.body().image_url;
                                Log.d(TAG, "이미지 업로드 성공: " + uploadedImageUrl);
                                createPost(uploadedImageUrl);
                            } else {
                                setLoading(false);
                                Log.e(TAG, "이미지 업로드 실패: " + response.code());
                                Toast.makeText(PostCreateActivity.this,
                                        "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(@NonNull Call<ApiService.ImageUploadResponse> call, @NonNull Throwable t) {
                            setLoading(false);
                            Log.e(TAG, "이미지 업로드 네트워크 오류", t);
                            Toast.makeText(PostCreateActivity.this,
                                    "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (IOException e) {
            setLoading(false);
            Log.e(TAG, "이미지 처리 오류", e);
            Toast.makeText(this, "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private void createPost(String imageUrl) {
        String title = titleEdit.getText().toString().trim();
        String author = authorEdit.getText().toString().trim();
        String content = contentEdit.getText().toString().trim();
        String formattedDate = DateUtils.formatToApiDate(mealDate);

        ApiService.CreatePostRequest request = new ApiService.CreatePostRequest(
                title, content, author, formattedDate, mealType, imageUrl
        );

        networkManager.getApiService().createPost(request)
                .enqueue(new Callback<Post>() {
                    @Override
                    public void onResponse(@NonNull Call<Post> call, @NonNull Response<Post> response) {
                        setLoading(false);

                        if (response.isSuccessful()) {
                            Log.d(TAG, "게시글 작성 성공");
                            Toast.makeText(PostCreateActivity.this,
                                    "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Log.e(TAG, "게시글 작성 실패: " + response.code());
                            Toast.makeText(PostCreateActivity.this,
                                    "게시글 작성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<Post> call, @NonNull Throwable t) {
                        setLoading(false);
                        Log.e(TAG, "게시글 작성 네트워크 오류", t);
                        Toast.makeText(PostCreateActivity.this,
                                "인터넷 연결을 확인해주세요.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void setLoading(boolean loading) {
        isLoading = loading;
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);

        // 메뉴 아이템 업데이트
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveItem = menu.findItem(R.id.action_save);
        if (saveItem != null) {
            saveItem.setEnabled(!isLoading);
            saveItem.setTitle(isLoading ? "작성 중..." : "등록");
        }
        return super.onPrepareOptionsMenu(menu);
    }
}