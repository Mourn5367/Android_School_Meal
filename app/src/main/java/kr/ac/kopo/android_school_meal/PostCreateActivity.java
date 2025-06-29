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

public class PostCreateActivity extends AppCompatActivity {
    private static final String TAG = "PostCreateActivity";
    private static final int REQUEST_CAMERA = 1001;
    private static final int REQUEST_GALLERY = 1002;

    private TextInputLayout titleLayout, authorLayout, contentLayout;
    private TextInputEditText titleEdit, authorEdit, contentEdit;
    private MaterialCardView mealInfoCard, imagePreviewCard;
    private MaterialTextView mealInfoText, mealTypeText;
    private ShapeableImageView imagePreview;
    private MaterialButton selectImageButton, removeImageButton;
    private MaterialButton toolbarSaveButton; // 툴바 저장 버튼 추가
    private View loadingOverlay;

    // 데이터
    private NetworkManager networkManager;
    private Uri selectedImageUri;
    private String uploadedImageUrl;
    private boolean isLoading = false;

    // 인텐트 데이터
    private int mealId;
    private String mealType;
    private String mealContent;
    private String mealDate;

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

        Log.d(TAG, "메뉴 정보 - ID: " + mealId + ", 타입: " + mealType + ", 날짜: " + mealDate);
    }

    private void initViews() {
        titleLayout = findViewById(R.id.titleLayout);
        authorLayout = findViewById(R.id.authorLayout);
        contentLayout = findViewById(R.id.contentLayout);
        titleEdit = findViewById(R.id.titleEdit);
        authorEdit = findViewById(R.id.authorEdit);
        contentEdit = findViewById(R.id.contentEdit);
        mealInfoCard = findViewById(R.id.mealInfoCard);
        mealInfoText = findViewById(R.id.mealInfoText);
        mealTypeText = findViewById(R.id.mealTypeText);
        imagePreviewCard = findViewById(R.id.imagePreviewCard);
        imagePreview = findViewById(R.id.imagePreview);
        selectImageButton = findViewById(R.id.selectImageButton);
        removeImageButton = findViewById(R.id.removeImageButton);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // 툴바 저장 버튼 추가
        toolbarSaveButton = findViewById(R.id.toolbarSaveButton);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false); // 기본 제목 완전히 숨기기
            getSupportActionBar().setTitle(""); // 제목을 빈 문자열로 설정
        }
    }

    private void setupButtons() {
        selectImageButton.setOnClickListener(v -> selectImage());
        removeImageButton.setOnClickListener(v -> removeImage());

        // 툴바 저장 버튼 클릭 이벤트
        if (toolbarSaveButton != null) {
            toolbarSaveButton.setOnClickListener(v -> {
                if (!isLoading) {
                    savePost();
                }
            });
        }
    }

    private void displayMealInfo() {
        // 메뉴 타입 표시
        mealTypeText.setText(mealType);

        // 메뉴 정보 표시 - 날짜와 식사 유형까지 크게
        String displayDate = DateUtils.formatForDisplay(mealDate);
        String formattedContent = formatMealContent(mealContent);

        // SpannableString을 사용해서 날짜와 식사 유형까지 크게 만들기
        String fullText = displayDate + " " + mealType + "\n" + formattedContent;
        android.text.SpannableString spannableString = new android.text.SpannableString(fullText);

        // 날짜 + 식사 유형 부분까지 크게 (첫 번째 줄 전체)
        int firstLineEndIndex = displayDate.length() + 1 + mealType.length(); // "3월 15일 (금) 아침"
        spannableString.setSpan(
                new android.text.style.RelativeSizeSpan(1.3f), // 30% 더 크게
                0,
                firstLineEndIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        // 날짜 + 식사 유형 부분을 볼드로도 만들기
        spannableString.setSpan(
                new android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0,
                firstLineEndIndex,
                android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );

        mealInfoText.setText(spannableString);

        // 메뉴 타입별 색상 설정
        int color = getMealTypeColor(mealType);
        MaterialCardView typeCard = mealInfoCard.findViewById(R.id.mealTypeText).getParent() instanceof MaterialCardView ?
                (MaterialCardView) mealInfoCard.findViewById(R.id.mealTypeText).getParent() : null;
        if (typeCard != null) {
            typeCard.setCardBackgroundColor(color);
        }
    }

    private String formatMealContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "메뉴 정보가 없습니다.";
        }
        return content.replace(",", ", ");
    }

    private int getMealTypeColor(String mealType) {
        switch (mealType) {
            case "아침":
                return getColor(R.color.breakfast_color);
            case "점심":
                return getColor(R.color.lunch_color);
            case "저녁":
                return getColor(R.color.dinner_color);
            default:
                return getColor(R.color.meal_type_color);
        }
    }

    // 메뉴를 완전히 비활성화
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 메뉴를 inflate하지 않음
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 뒤로가기 버튼만 처리
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void selectImage() {
        Dexter.withContext(this)
                .withPermissions(
                        Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                )
                .withListener(new MultiplePermissionsListener() {
                    @Override
                    public void onPermissionsChecked(MultiplePermissionsReport report) {
                        if (report.areAllPermissionsGranted()) {
                            showImageSourceDialog();
                        } else {
                            Toast.makeText(PostCreateActivity.this,
                                    "이미지 선택을 위해 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                                                                   PermissionToken token) {
                        token.continuePermissionRequest();
                    }
                }).check();
    }

    private void showImageSourceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("이미지 선택")
                .setItems(new String[]{"갤러리", "카메라"}, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            openGallery();
                            break;
                        case 1:
                            openCamera();
                            break;
                    }
                })
                .show();
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_GALLERY);
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_CAMERA);
        } else {
            Toast.makeText(this, "카메라를 사용할 수 없습니다.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            switch (requestCode) {
                case REQUEST_GALLERY:
                    selectedImageUri = data.getData();
                    showImagePreview();
                    break;

                case REQUEST_CAMERA:
                    Bundle extras = data.getExtras();
                    if (extras != null) {
                        Bitmap imageBitmap = (Bitmap) extras.get("data");
                        if (imageBitmap != null) {
                            imagePreview.setImageBitmap(imageBitmap);
                            showImagePreview();
                        }
                    }
                    break;
            }
        }
    }

    private void showImagePreview() {
        if (selectedImageUri != null) {
            Glide.with(this)
                    .load(selectedImageUri)
                    .centerCrop()
                    .into(imagePreview);

            imagePreviewCard.setVisibility(View.VISIBLE);
        }
    }

    private void removeImage() {
        selectedImageUri = null;
        imagePreviewCard.setVisibility(View.GONE);
        imagePreview.setImageDrawable(null);
    }

    private void savePost() {
        if (!validateInputs()) {
            return;
        }

        setLoading(true);

        if (selectedImageUri != null) {
            uploadImageAndSavePost();
        } else {
            createPost(null);
        }
    }

    private boolean validateInputs() {
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

    // 이미지 업로드
    private void uploadImageAndSavePost() {
        if (selectedImageUri == null) {
            createPost(null);
            return;
        }

        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
            String base64Image = bitmapToBase64(bitmap);
            String filename = "post_image_" + System.currentTimeMillis() + ".jpg";

            ApiService.ImageUploadRequest request = new ApiService.ImageUploadRequest(
                    "data:image/jpeg;base64," + base64Image, filename
            );

            Call<ApiService.ImageUploadResponse> call = networkManager.getApiService().uploadImage(request);

            NetworkRequestUtility.executeWithRetry(call, new NetworkRequestUtility.NetworkCallback<ApiService.ImageUploadResponse>() {
                @Override
                public void onSuccess(ApiService.ImageUploadResponse result) {
                    Log.d(TAG, "이미지 업로드 성공: " + result.image_url);
                    createPost(result.image_url);
                }

                @Override
                public void onFailure(String errorMessage) {
                    Log.e(TAG, "이미지 업로드 최종 실패: " + errorMessage);
                    if (!errorMessage.contains("서버 연결이 불안정")) {
                        Toast.makeText(PostCreateActivity.this,
                                "이미지 업로드에 실패했습니다. 이미지 없이 게시글을 작성합니다.", Toast.LENGTH_SHORT).show();
                    }
                    // 이미지 없이 게시글 작성 진행
                    createPost(null);
                }

                @Override
                public void onLoading(boolean isLoading) {
                    // 이미지 업로드는 createPost에서 이미 로딩 중이므로 별도 처리 안 함
                }
            }, "이미지 업로드");

        } catch (IOException e) {
            Log.e(TAG, "이미지 처리 오류", e);
            Toast.makeText(this, "이미지 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            setLoading(false);
        }
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // 게시글 작성
    private void createPost(String imageUrl) {
        String title = titleEdit.getText().toString().trim();
        String author = authorEdit.getText().toString().trim();
        String content = contentEdit.getText().toString().trim();
        String formattedDate = DateUtils.formatToApiDate(mealDate);

        ApiService.CreatePostRequest request = new ApiService.CreatePostRequest(
                title, content, author, formattedDate, mealType, imageUrl
        );

        Call<Post> call = networkManager.getApiService().createPost(request);

        NetworkRequestUtility.executeWithRetry(call, new NetworkRequestUtility.NetworkCallback<Post>() {
            @Override
            public void onSuccess(Post result) {
                Log.d(TAG, "게시글 작성 성공: " + result.getTitle());
                Toast.makeText(PostCreateActivity.this,
                        "게시글이 작성되었습니다.", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            }

            @Override
            public void onFailure(String errorMessage) {
                Log.e(TAG, "게시글 작성 최종 실패: " + errorMessage);
                if (!errorMessage.contains("서버 연결이 불안정")) {
                    Toast.makeText(PostCreateActivity.this,
                            "게시글 작성에 실패했습니다.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onLoading(boolean isLoading) {
                setLoading(isLoading);
            }
        }, "게시글 작성");
    }

    // 개선된 로딩 상태 관리
    private void setLoading(boolean loading) {
        isLoading = loading;
        loadingOverlay.setVisibility(loading ? View.VISIBLE : View.GONE);

        // 입력 필드 비활성화/활성화
        titleEdit.setEnabled(!loading);
        authorEdit.setEnabled(!loading);
        contentEdit.setEnabled(!loading);
        selectImageButton.setEnabled(!loading);

        // 툴바 저장 버튼 상태 업데이트
        updateSaveButtonState();
    }

    // 버튼 상태 업데이트 메서드
    private void updateSaveButtonState() {
        if (toolbarSaveButton == null) return;

        if (isLoading) {
            toolbarSaveButton.setText("작성 중...");
            toolbarSaveButton.setEnabled(false);
            toolbarSaveButton.setAlpha(0.6f);
            toolbarSaveButton.setIcon(null); // 로딩 중에는 아이콘 숨기기
        } else {
            toolbarSaveButton.setText("등록");
            toolbarSaveButton.setEnabled(true);
            toolbarSaveButton.setAlpha(1.0f);
        }
    }
}