package kr.ac.kopo.android_school_meal;

import android.animation.ObjectAnimator;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textview.MaterialTextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class MealGroupAdapter extends RecyclerView.Adapter<MealGroupAdapter.DateGroupViewHolder> {

    private List<DateGroup> dateGroups = new ArrayList<>();
    private OnMealClickListener listener;
    // 펼쳐진 날짜들을 저장하는 Set
    private Set<String> expandedDates = new HashSet<>();

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
        private ImageView expandIcon;
        private View dividerLine;
        private LinearLayout headerLayout;

        public DateGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            mealCountText = itemView.findViewById(R.id.mealCountText);
            mealsContainer = itemView.findViewById(R.id.mealsContainer);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            dividerLine = itemView.findViewById(R.id.dividerLine);
            headerLayout = itemView.findViewById(R.id.headerLayout);
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

            // 현재 펼침 상태 확인
            boolean isExpanded = expandedDates.contains(dateGroup.date);

            // 메뉴 아이템들 추가
            mealsContainer.removeAllViews();
            for (Meal meal : dateGroup.meals) {
                View mealView = createMealView(meal, dateGroup.date);
                mealsContainer.addView(mealView);
            }

            // 펼침/접힘 상태 설정
            if (isExpanded) {
                mealsContainer.setVisibility(View.VISIBLE);
                dividerLine.setVisibility(View.VISIBLE);
                expandIcon.setRotation(180f);
            } else {
                mealsContainer.setVisibility(View.GONE);
                dividerLine.setVisibility(View.GONE);
                expandIcon.setRotation(0f);
            }

            // 헤더 클릭 이벤트 (펼치기/접기)
            headerLayout.setOnClickListener(v -> {
                toggleExpansion(dateGroup.date);
            });
        }

        private void toggleExpansion(String date) {
            boolean isCurrentlyExpanded = expandedDates.contains(date);

            if (isCurrentlyExpanded) {
                // 접기
                expandedDates.remove(date);

                // 애니메이션
                animateArrow(180f, 0f);

                // 메뉴 컨테이너 숨기기
                mealsContainer.setVisibility(View.GONE);
                dividerLine.setVisibility(View.GONE);

            } else {
                // 펼치기
                expandedDates.add(date);

                // 애니메이션
                animateArrow(0f, 180f);

                // 메뉴 컨테이너 보이기
                mealsContainer.setVisibility(View.VISIBLE);
                dividerLine.setVisibility(View.VISIBLE);
            }
        }

        private void animateArrow(float fromRotation, float toRotation) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(expandIcon, "rotation", fromRotation, toRotation);
            animator.setDuration(200);
            animator.start();
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

            // 식사 유형별 색상 - 리소스에서 가져오기
            int mealColor = getMealTypeColorFromResource(meal.getMealType());

            // 작은 원형 인디케이터에 색상 적용
            mealTypeIndicator.setBackgroundColor(mealColor);

            // 식사 유형 배지 배경색도 변경
            MaterialCardView mealTypeBadge = (MaterialCardView) mealTypeText.getParent().getParent();
            if (mealTypeBadge != null) {
                mealTypeBadge.setCardBackgroundColor(mealColor);
            }

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
                if (date == null) return itemView.getContext().getColor(R.color.gray_medium);

                int dayOfWeek = Integer.parseInt(new SimpleDateFormat("u", Locale.getDefault()).format(date));

                switch (dayOfWeek) {
                    case 1: return itemView.getContext().getColor(R.color.monday_color);    // 월요일 - 빨강
                    case 2: return itemView.getContext().getColor(R.color.tuesday_color);   // 화요일 - 주황
                    case 3: return itemView.getContext().getColor(R.color.wednesday_color); // 수요일 - 초록
                    case 4: return itemView.getContext().getColor(R.color.thursday_color);  // 목요일 - 파랑
                    case 5: return itemView.getContext().getColor(R.color.friday_color);    // 금요일 - 보라
                    default: return itemView.getContext().getColor(R.color.gray_medium);    // 기본 회색
                }
            } catch (Exception e) {
                return itemView.getContext().getColor(R.color.gray_medium);
            }
        }

        // 요일별 배경색 (연한 버전)
        private int getWeekdayBackgroundColor(String dateStr) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
                if (date == null) return itemView.getContext().getColor(R.color.gray_lighter);

                int dayOfWeek = Integer.parseInt(new SimpleDateFormat("u", Locale.getDefault()).format(date));

                switch (dayOfWeek) {
                    case 1: return itemView.getContext().getColor(R.color.monday_light);    // 월요일 - 연한 빨강
                    case 2: return itemView.getContext().getColor(R.color.tuesday_light);   // 화요일 - 연한 주황
                    case 3: return itemView.getContext().getColor(R.color.wednesday_light); // 수요일 - 연한 초록
                    case 4: return itemView.getContext().getColor(R.color.thursday_light);  // 목요일 - 연한 파랑
                    case 5: return itemView.getContext().getColor(R.color.friday_light);    // 금요일 - 연한 보라
                    default: return itemView.getContext().getColor(R.color.gray_lighter);   // 기본 연한 회색
                }
            } catch (Exception e) {
                return itemView.getContext().getColor(R.color.gray_lighter);
            }
        }



        private int getMealTypeColorFromResource(String mealType) {
            switch (mealType) {
                case "아침":
                    return itemView.getContext().getColor(R.color.breakfast_color); // 주황
                case "점심":
                    return itemView.getContext().getColor(R.color.lunch_color);     // 초록
                case "저녁":
                    return itemView.getContext().getColor(R.color.dinner_color);   // 파랑
                default:
                    return itemView.getContext().getColor(R.color.gray_medium);
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