package kr.ac.kopo.android_school_meal;
import com.google.gson.annotations.SerializedName;
public class Comment {
    private int id;
    private String content;
    private String author;

    @SerializedName("created_at")
    private String createdAt;

    private int likes;

    // 생성자
    public Comment() {}

    public Comment(int id, String content, String author, String createdAt, int likes) {
        this.id = id;
        this.content = content;
        this.author = author;
        this.createdAt = createdAt;
        this.likes = likes;
    }

    // Getter 메서드들
    public int getId() {
        return id;
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

    // Setter 메서드들
    public void setId(int id) {
        this.id = id;
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

    @Override
    public String toString() {
        return "Comment{" +
                "id=" + id +
                ", content='" + content + '\'' +
                ", author='" + author + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", likes=" + likes +
                '}';
    }
}
