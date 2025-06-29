package kr.ac.kopo.android_school_meal;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class DateUtils {
    private static final String TAG = "DateUtils";

    // 다양한 날짜 형식들
    private static final SimpleDateFormat[] DATE_FORMATS = {
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()),
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()),
            new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH), // RFC 2822
    };

    private static final SimpleDateFormat API_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private static final SimpleDateFormat DISPLAY_DATE_FORMAT = new SimpleDateFormat("M월 d일 (E)", Locale.KOREAN);

    // 다양한 날짜 형식을 파싱
    public static Date parseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            Log.w(TAG, "날짜 문자열이 null 또는 비어있음");
            return null;
        }

        Log.d(TAG, "날짜 파싱 시도: " + dateStr);

        for (SimpleDateFormat format : DATE_FORMATS) {
            try {
                // UTC 시간대로 설정
                if (dateStr.contains("GMT") || dateStr.contains("UTC")) {
                    format.setTimeZone(TimeZone.getTimeZone("UTC"));
                }
                Date parsed = format.parse(dateStr);
                Log.d(TAG, "날짜 파싱 성공: " + dateStr + " -> " + parsed);
                return parsed;
            } catch (ParseException e) {
                // 다음 형식으로 시도
            }
        }

        Log.e(TAG, "모든 날짜 형식 파싱 실패: " + dateStr);
        return null;
    }

    // API 전송용 날짜 포맷 (YYYY-MM-DD)
    public static String formatToApiDate(String dateStr) {
        Date date = parseDate(dateStr);
        if (date != null) {
            String formatted = API_DATE_FORMAT.format(date);
            Log.d(TAG, "API 날짜 포맷 변환: " + dateStr + " -> " + formatted);
            return formatted;
        }
        Log.w(TAG, "API 날짜 포맷 변환 실패, 원본 반환: " + dateStr);
        return dateStr;
    }

    // 화면 표시용 날짜 포맷
    public static String formatForDisplay(String dateStr) {
        Date date = parseDate(dateStr);
        if (date != null) {
            String formatted = DISPLAY_DATE_FORMAT.format(date);
            Log.d(TAG, "화면 표시용 날짜 포맷: " + dateStr + " -> " + formatted);
            return formatted;
        }
        Log.w(TAG, "화면 표시용 날짜 포맷 변환 실패, 원본 반환: " + dateStr);
        return dateStr;
    }

    // 상대 시간 계산
    public static String formatRelativeTime(String dateStr) {
        Date date = parseDate(dateStr);
        if (date == null) {
            return dateStr;
        }

        Date now = new Date();
        long diffMillis = now.getTime() - date.getTime();

        Log.d(TAG, "상대 시간 계산: 게시 시간=" + date + ", 현재 시간=" + now + ", 차이=" + diffMillis + "ms");

        // 미래 시간인 경우 (서버와 클라이언트 시간 차이)
        if (diffMillis < 0) {
            diffMillis = Math.abs(diffMillis);
        }

        long seconds = diffMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 30) {
            return "방금 전";
        } else if (minutes < 1) {
            return "1분 미만";
        } else if (minutes < 60) {
            return minutes + "분 전";
        } else if (hours < 24) {
            return hours + "시간 전";
        } else if (days < 7) {
            return days + "일 전";
        } else {
            // 일주일 이상은 구체적 날짜
            SimpleDateFormat format = new SimpleDateFormat("M.d HH:mm", Locale.getDefault());
            return format.format(date);
        }
    }

    // 현재 날짜 문자열 반환 (YYYY-MM-DD)
    public static String getCurrentDateString() {
        return API_DATE_FORMAT.format(new Date());
    }

    // 날짜 문자열을 Date 객체로 변환 (간단한 버전)
    public static Date stringToDate(String dateStr) {
        return parseDate(dateStr);
    }

    // Date 객체를 문자열로 변환
    public static String dateToString(Date date) {
        if (date == null) return null;
        return API_DATE_FORMAT.format(date);
    }
}