package kr.ac.kopo.android_school_meal;


import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // 메뉴 조회
    @GET("menu")
    Call<List<Meal>> getMeals();

    // 게시글 목록 조회
    @GET("posts")
    Call<List<Post>> getPosts(@Query("date") String date, @Query("meal_type") String mealType);

    // 게시글 상세 조회
    @GET("posts/{id}")
    Call<PostDetailResponse> getPostDetail(@Path("id") int postId);

    // 게시글 작성
    @POST("posts")
    Call<Post> createPost(@Body CreatePostRequest request);

    // 게시글 좋아요 토글
    @POST("posts/{id}/like")
    Call<LikeResponse> togglePostLike(@Path("id") int postId, @Body LikeRequest request);

    // 댓글 작성
    @POST("posts/{id}/comments")
    Call<Comment> createComment(@Path("id") int postId, @Body CreateCommentRequest request);

    // 댓글 좋아요 토글
    @POST("comments/{id}/like")
    Call<LikeResponse> toggleCommentLike(@Path("id") int commentId, @Body LikeRequest request);

    // 이미지 업로드
    @POST("upload-image-base64")
    Call<ImageUploadResponse> uploadImage(@Body ImageUploadRequest request);

    // 요청/응답 클래스들
    class CreatePostRequest {
        public String title;
        public String content;
        public String author;
        public String meal_date;
        public String meal_type;
        public String image_url;

        public CreatePostRequest(String title, String content, String author,
                                 String mealDate, String mealType, String imageUrl) {
            this.title = title;
            this.content = content;
            this.author = author;
            this.meal_date = mealDate;
            this.meal_type = mealType;
            this.image_url = imageUrl;
        }
    }

    class CreateCommentRequest {
        public String content;
        public String author;

        public CreateCommentRequest(String content, String author) {
            this.content = content;
            this.author = author;
        }
    }

    class LikeRequest {
        public String user_identifier;

        public LikeRequest(String userIdentifier) {
            this.user_identifier = userIdentifier;
        }
    }

    class ImageUploadRequest {
        public String image_data;
        public String filename;

        public ImageUploadRequest(String imageData, String filename) {
            this.image_data = imageData;
            this.filename = filename;
        }
    }

    class PostDetailResponse {
        public Post post;
        public List<Comment> comments;
    }

    class LikeResponse {
        public boolean liked;
        public int likes;
    }

    class ImageUploadResponse {
        public String image_url;
    }
}
