package kr.ac.kopo.android_school_meal;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Post {
    private int id;
    private String title;
    private String content;
    private String author;

    @SerializedName("created_at")
    private String createdAt;

    private int likes;

    @SerializedName("comment_count")
    private int commentCount;

    @SerializedName("image_url")
    private String imageUrl;

    // 생성자
    public Post() {}

    public Post(int id, String title, String content, String author,
                String createdAt, int likes, int commentCount, String imageUrl) {
        this.id = id;
        this.title = title;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
        this.likes = likes;
        this.commentCount = commentCount;
        this.imageUrl = imageUrl;
    }

    // Getter 메서드들
    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }

    public String getAuthor() {
        return author;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public int getLikes() {
        return likes;
    }

    public int getCommentCount() {
        return commentCount;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    // Setter 메서드들
    public void setId(int id) {
        this.id = id;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public void setLikes(int likes) {
        this.likes = likes;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", content='" + content + '\'' +
                ", author='" + author + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", likes=" + likes +
                ", commentCount=" + commentCount +
                ", imageUrl='" + imageUrl + '\'' +
                '}';
    }
}
