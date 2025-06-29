package kr.ac.kopo.android_school_meal;

public class ApiConfig {
    // 서버 IP를 현재 개발 환경에 맞게 변경하세요
    private static final String SERVER_IP = "192.168.26.165"; // 실제 서버 IP로 변경

    public static final String BASE_URL = "http://" + SERVER_IP + ":5000/api/";
    public static final String IMAGE_BASE_URL = "http://" + SERVER_IP + ":5000/api";

    // API 엔드포인트들
    public static final String MENU_ENDPOINT = "menu";
    public static final String POSTS_ENDPOINT = "posts";
    public static final String UPLOAD_IMAGE_ENDPOINT = "upload-image-base64";

    // 이미지 URL 생성
    public static String getImageUrl(String imagePath) {
        if (imagePath == null || imagePath.isEmpty()) {
            return null;
        }

        if (imagePath.startsWith("http")) {
            return imagePath;
        }

        return IMAGE_BASE_URL + imagePath;
    }
}