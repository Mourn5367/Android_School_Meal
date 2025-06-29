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
        private MaterialCardView mealsContainerCard;
        private View colorIndicator;
        private ImageView expandIcon;
        private MaterialCardView headerCard;

        private boolean isExpanded = false;

        public DateGroupViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            mealCountText = itemView.findViewById(R.id.mealCountText);
            mealsContainer = itemView.findViewById(R.id.mealsContainer);
            mealsContainerCard = itemView.findViewById(R.id.mealsContainerCard);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            expandIcon = itemView.findViewById(R.id.expandIcon);
            headerCard = itemView.findViewById(R.id.headerCard);
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
            mealsContainerCard.setCardBackgroundColor(backgroundColor);

            // 헤더 클릭 리스너 (ExpansionTile 동작)
            headerCard.setOnClickListener(v -> toggleExpansion(dateGroup));

            // 초기 상태는 접힌 상태
            resetExpansionState();
        }

        private void resetExpansionState() {
            isExpanded = false;
            mealsContainerCard.setVisibility(View.GONE);
            expandIcon.setRotation(0f); // 아래쪽 화살표
        }

        private void toggleExpansion(DateGroup dateGroup) {
            if (isExpanded) {
                // 접기
                collapseWithAnimation();
            } else {
                // 펼치기
                expandWithAnimation(dateGroup);
            }
            isExpanded = !isExpanded;
        }

        private void expandWithAnimation(DateGroup dateGroup) {
            // 메뉴 아이템들 추가 (펼쳐질 때만)
            mealsContainer.removeAllViews();
            for (Meal meal : dateGroup.meals) {
                View mealView = createSimpleMealView(meal, dateGroup.date);
                mealsContainer.addView(mealView);
            }

            // 표시하고 애니메이션
            mealsContainerCard.setVisibility(View.VISIBLE);

            // 화살표 회전 애니메이션
            ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(expandIcon, "rotation", 0f, 180f);
            rotateAnimator.setDuration(300);
            rotateAnimator.start();
        }

        private void collapseWithAnimation() {
            // 메뉴 컨테이너 숨기기
            mealsContainerCard.setVisibility(View.GONE);

            // 화살표 회전 애니메이션
            ObjectAnimator rotateAnimator = ObjectAnimator.ofFloat(expandIcon, "rotation", 180f, 0f);
            rotateAnimator.setDuration(300);
            rotateAnimator.start();
        }

        private View createSimpleMealView(Meal meal, String date) {
            View view = LayoutInflater.from(itemView.getContext())
                    .inflate(R.layout.item_meal, mealsContainer, false);

            MaterialCardView mealCard = view.findViewById(R.id.mealCard);
            MaterialTextView mealTypeText = view.findViewById(R.id.mealTypeText);
            MaterialTextView contentText = view.findViewById(R.id.contentText);
            View mealTypeIndicator = view.findViewById(R.id.mealTypeIndicator);

            // 식사 유형 표시
            mealTypeText.setText(meal.getMealType());

            // 기존처럼 전체 메뉴 내용 표시 (• 형태)
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

        // 기존처럼 메뉴 내용 포맷팅
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

        // 요일별 배경색 (연한 버전)
        private int getWeekdayBackgroundColor(String dateStr) {
            try {
                Date date = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(dateStr);
                if (date == null) return Color.parseColor("#F5F5F5");

                int dayOfWeek = Integer.parseInt(new SimpleDateFormat("u", Locale.getDefault()).format(date));

                switch (dayOfWeek) {
                    case 1: return Color.parseColor("#FFEBEE"); // 월요일 - 연한 빨강
                    case 2: return Color.parseColor("#FFF3E0"); // 화요일 - 연한 주황
                    case 3: return Color.parseColor("#E8F5E8"); // 수요일 - 연한 초록
                    case 4: return Color.parseColor("#E3F2FD"); // 목요일 - 연한 파랑
                    case 5: return Color.parseColor("#F3E5F5"); // 금요일 - 연한 보라
                    default: return Color.parseColor("#F5F5F5"); // 기본 연한 회색
                }
            } catch (Exception e) {
                return Color.parseColor("#F5F5F5");
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