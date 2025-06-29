package kr.ac.kopo.android_school_meal;

import com.google.gson.annotations.SerializedName;
import java.util.Date;

public class Meal {
    private int id;
    private String date;

    @SerializedName("meal_type")
    private String mealType;

    private String content;

    public Meal(int id, String date, String mealType, String content) {
        this.id = id;
        this.date = date != null ? date : getCurrentDate();
        this.mealType = mealType != null ? mealType : "정보 없음";
        this.content = content != null ? content : "정보 없음";
    }

    // Getter 메서드들
    public int getId() {
        return id;
    }

    public String getDate() {
        return date;
    }

    public String getMealType() {
        return mealType;
    }

    public String getContent() {
        return content;
    }

    // Setter 메서드들
    public void setId(int id) {
        this.id = id;
    }

    public void setDate(String date) {
        this.date = date != null ? date : getCurrentDate();
    }

    public void setMealType(String mealType) {
        this.mealType = mealType != null ? mealType : "정보 없음";
    }

    public void setContent(String content) {
        this.content = content != null ? content : "정보 없음";
    }

    // 현재 날짜를 YYYY-MM-DD 형식으로 반환
    private String getCurrentDate() {
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    // toString 메서드
    @Override
    public String toString() {
        return "Meal{" +
                "id=" + id +
                ", date='" + date + '\'' +
                ", mealType='" + mealType + '\'' +
                ", content='" + content + '\'' +
                '}';
    }
}