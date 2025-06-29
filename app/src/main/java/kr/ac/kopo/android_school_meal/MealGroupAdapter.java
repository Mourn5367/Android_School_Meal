package kr.ac.kopo.android_school_meal;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class MealGroupAdapter extends RecyclerView.Adapter<MealGroupAdapter.DateGroupViewHolder> {

    private List<DateGroup> dateGroups = new ArrayList<>();
    private OnMealClickListener listener;

    public interface OnMealClickListener {
        void onMealClick(Meal meal, String date);
    }

    public MealGroupAdapter(OnMealClickListener listener) {
        this.listener = listener;
    }

    public void updateData(Map<String, List<Meal>> groupedMeals) {
        dateGroups.clear();

        // 날짜순으로 정렬
        Map<String, List<Meal>> sortedMap = new TreeMap<>(groupedMeals);

        for (Map.Entry<String, List<Meal>> entry : sortedMap.entrySet()) {
            dateGroups.add(new DateGroup(entry.getKey(), entry.getValue()));
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public DateGroupViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_date_group, parent, false);
        return new DateGroupViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateGroupViewHolder holder, int position) {
        DateGroup dateGroup = dateGroups.get(position);
        holder.bind(dateGroup);
    }

    @Override
    public int getItemCount() {
        return dateGroups.size();
    }

    class DateGroupViewHolder extends RecyclerView.ViewHolder {
        private MaterialTextView dateText;
        private MaterialTextView mealCountText;
        private LinearLayout mealsContainer;
        private View colorIndicator;

        public DateGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            mealCountText = itemView.findViewById(R.id.mealCountText);
            mealsContainer = itemView.findViewById(R.id.mealsContainer);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
        }

        public void bind(DateGroup dateGroup) {
            // 날짜 표시
            String displayDate = DateUtils.formatForDisplay(dateGroup.date);
            dateText.setText(displayDate);

            // 메뉴 개수 표시
            mealCountText.setText(dateGroup.meals.size() + "개 식사");

            // 요일별 색상 설정
            int color = getWeekdayColor(dateGroup.date);
            colorIndicator.setBackgroundColor(color);

            // 메뉴 컨테이너 배경색을 요일 색상의 연한 버전으로 설정
            int backgroundColor = getWeekdayBackgroundColor(dateGroup.date);
            mealsContainer.setBackgroundColor(backgroundColor);

            // 메뉴 아이템들 바로 표시 (접힘 없음)
            mealsContainer.removeAllViews();
            for (Meal meal : dateGroup.meals) {
                View mealView = createMealView(meal, dateGroup.date);
                mealsContainer.addView(mealView);
            }

            // 항상 표시되도록 설정
            mealsContainer.setVisibility(View.VISIBLE);
        }

        private View createMealView(Meal meal, String date) {
            View view = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_meal, mealsContainer, false);

            MaterialCardView mealCard = view.findViewById(R.id.mealCard);
            MaterialTextView mealTypeText = view.findViewById(R.id.mealTypeText);
            MaterialTextView contentText = view.findViewById(R.id.contentText);
            View mealTypeIndicator = view.findViewById(R.id.mealTypeIndicator);

            // 식사 유형 표시
            mealTypeText.setText(meal.getMealType());

            // 전체 메뉴 내용 표시 (• 형태)
            String formattedContent = formatMealContent(meal.getContent());
            contentText.setText(formattedContent);

            // 식사 유형별 색상
            int mealColor = getMealTypeColor(meal.getMealType());
            mealTypeIndicator.setBackgroundColor(mealColor);

            // 클릭 이벤트 (상세 페이지로 이동)
            mealCard.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onMealClick(meal, date);
                }
            });

            return view;
        }

        // 메뉴 내용 포맷팅
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

        private int getWeekdayColor(String dateStr) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
                if (date == null) return Color.GRAY;

                int dayOfWeek = Integer.parseInt(new SimpleDateFormat("u", Locale.getDefault()).format(date));

                switch (dayOfWeek) {
                    case 1: return Color.parseColor("#F44336"); // 월요일 - 빨강
                    case 2: return Color.parseColor("#FF9800"); // 화요일 - 주황
                    case 3: return Color.parseColor("#4CAF50"); // 수요일 - 초록
                    case 4: return Color.parseColor("#2196F3"); // 목요일 - 파랑
                    case 5: return Color.parseColor("#9C27B0"); // 금요일 - 보라
                    default: return Color.GRAY;
                }
            } catch (Exception e) {
                return Color.GRAY;
            }
        }

        // 요일별 배경색 (연한 버전) - 리소스에서 가져오기
        private int getWeekdayBackgroundColor(String dateStr) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
                if (date == null) return itemView.getContext().getColor(android.R.color.background_light);

                int dayOfWeek = Integer.parseInt(new SimpleDateFormat("u", Locale.getDefault()).format(date));

                switch (dayOfWeek) {
                    case 1: return itemView.getContext().getColor(R.color.monday_light);    // 월요일 - 연한 빨강
                    case 2: return itemView.getContext().getColor(R.color.tuesday_light);   // 화요일 - 연한 주황
                    case 3: return itemView.getContext().getColor(R.color.wednesday_light); // 수요일 - 연한 초록
                    case 4: return itemView.getContext().getColor(R.color.thursday_light);  // 목요일 - 연한 파랑
                    case 5: return itemView.getContext().getColor(R.color.friday_light);    // 금요일 - 연한 보라
                    default: return itemView.getContext().getColor(android.R.color.background_light); // 기본 연한 회색
                }
            } catch (Exception e) {
                return itemView.getContext().getColor(android.R.color.background_light);
            }
        }

        private int getMealTypeColor(String mealType) {
            switch (mealType) {
                case "아침":
                    return Color.parseColor("#FF9800"); // 주황
                case "점심":
                    return Color.parseColor("#4CAF50"); // 초록
                case "저녁":
                    return Color.parseColor("#2196F3"); // 파랑
                default:
                    return Color.GRAY;
            }
        }
    }

    private static class DateGroup {
        final String date;
        final List<Meal> meals;

        DateGroup(String date, List<Meal> meals) {
            this.date = date;
            this.meals = meals;
        }
    }
}