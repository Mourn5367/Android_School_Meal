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
        selectImageButton.setOnClickListener(v -> checkPermissionsAndPickImage());
        removeImageButton.setOnClickListener(v -> removeSelectedImage());
    }

    private void displayMealInfo() {
        String displayDate = DateUtils.formatForDisplay(mealDate);
        mealInfoText.setText(displayDate + " " + mealType);
        mealTypeText.setText(mealType);

        // 메뉴 내용을 리스트 형태로 표시
        String formattedContent = formatMealContent(mealContent);
        MaterialTextView mealContentText = findViewById(R.id.mealContentText);
        if (mealContentText != null) {
            mealContentText.setText(formattedContent);
        }
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

    private void checkPermissionsAndPickImage() {
        // Android 버전에 따라 다른 권한 요청
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) { // API 33+
            // Android 13 이상: 새로운 미디어 권한 사용
            Dexter.withContext(this)
                    .withPermissions(
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_MEDIA_IMAGES
                    )
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if (report.areAllPermissionsGranted()) {
                                showImagePickerOptions();
                            } else {
                                handlePermissionDenied(report);
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                                                                       PermissionToken token) {
                            showPermissionRationale(token);
                        }
                    })
                    .check();
        } else {
            // Android 12 이하: 기존 권한 사용
            Dexter.withContext(this)
                    .withPermissions(
                            Manifest.permission.CAMERA,
                            Manifest.permission.READ_EXTERNAL_STORAGE
                    )
                    .withListener(new MultiplePermissionsListener() {
                        @Override
                        public void onPermissionsChecked(MultiplePermissionsReport report) {
                            if (report.areAllPermissionsGranted()) {
                                showImagePickerOptions();
                            } else {
                                handlePermissionDenied(report);
                            }
                        }

                        @Override
                        public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissions,
                                                                       PermissionToken token) {
                            showPermissionRationale(token);
                        }
                    })
                    .check();
        }
    }
    private void handlePermissionDenied(MultiplePermissionsReport report) {
        if (report.isAnyPermissionPermanentlyDenied()) {
            // 권한이 영구적으로 거부된 경우
            new AlertDialog.Builder(this)
                    .setTitle("권한 필요")
                    .setMessage("이미지를 선택하려면 카메라와 저장소 접근 권한이 필요합니다.\n\n설정에서 권한을 허용해주세요.")
                    .setPositiveButton("설정으로 이동", (dialog, which) -> {
                        // 앱 설정으로 이동
                        Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("취소", null)
                    .show();
        } else {
            // 일반적인 권한 거부
            Toast.makeText(this, "이미지 선택을 위해 권한이 필요합니다", Toast.LENGTH_LONG).show();
        }
    }
    private void showPermissionRationale(PermissionToken token) {
        new AlertDialog.Builder(this)
                .setTitle("권한이 필요합니다")
                .setMessage("사진을 선택하고 촬영하기 위해 다음 권한이 필요합니다:\n\n" +
                        "• 카메라: 사진 촬영\n" +
                        "• 저장소: 갤러리에서 사진 선택")
                .setPositiveButton("허용", (dialog, which) -> token.continuePermissionRequest())
                .setNegativeButton("거부", (dialog, which) -> token.cancelPermissionRequest())
                .show();
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
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        if (galleryIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(galleryIntent, REQUEST_GALLERY);
        }
    }

    private void takePictureFromCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(cameraIntent, REQUEST_CAMERA);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_GALLERY && data != null) {
                selectedImageUri = data.getData();
                displaySelectedImage();
            } else if (requestCode == REQUEST_CAMERA && data != null) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        // Bitmap을 임시 파일로 저장하고 Uri 생성
                        selectedImageUri = getImageUriFromBitmap(imageBitmap);
                        displaySelectedImage();
                    }
                }
            }
        }
    }

    private Uri getImageUriFromBitmap(Bitmap bitmap) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, "Camera_Image", null);
        return Uri.parse(path);
    }

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

    // 개선된 이미지 업로드 (NetworkRequestUtility 사용)
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

    // 개선된 게시글 작성 (NetworkRequestUtility 사용)
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

        // 메뉴 아이템 업데이트
        invalidateOptionsMenu();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem saveItem = menu.findItem(R.id.action_save);
        if (saveItem != null) {
            saveItem.setEnabled(!isLoading);
            if (isLoading) {
                saveItem.setTitle("작성 중...");
            } else {
                saveItem.setTitle("등록");
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }
}