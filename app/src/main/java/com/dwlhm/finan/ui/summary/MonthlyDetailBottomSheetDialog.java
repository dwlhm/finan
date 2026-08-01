package com.dwlhm.finan.ui.summary;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.CashFlowReport;
import com.dwlhm.finan.ui.dashboard.DashboardViewModel;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.time.ZoneId;
import java.time.LocalDate;

public class MonthlyDetailBottomSheetDialog {

    public static class CategoryDetail {
        public String name;
        public long amount;
        public CategoryDetail(String name, long amount) {
            this.name = name;
            this.amount = amount;
        }
    }

    public static void show(Context context, CashFlowReport.WeekSummary monthSummary, boolean isIncome, DashboardViewModel.DisplayMode mode, long monthTotalIncomeMinor, Map<Long, List<CategoryDetail>> categoryDetails) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_weekly_detail, null);
        dialog.setContentView(view);

        View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);

        TextView title = view.findViewById(R.id.weekly_detail_title);
        ImageView closeBtn = view.findViewById(R.id.weekly_detail_close);
        TextView dateRange = view.findViewById(R.id.weekly_detail_date_range);
        TextView totalText = view.findViewById(R.id.weekly_detail_total);
        RecyclerView recyclerView = view.findViewById(R.id.weekly_detail_recycler);

        closeBtn.setOnClickListener(v -> dialog.dismiss());

        title.setText(isIncome ? "Pemasukan Bulanan" : "Pengeluaran Bulanan");
        SimpleDateFormat rangeFormat = new SimpleDateFormat("MMMM yyyy", new Locale("id", "ID"));
        String dateStr = rangeFormat.format(java.sql.Date.valueOf(monthSummary.getStartDate().toString()));
        dateRange.setText(dateStr);

        long monthTotal = isIncome ? monthSummary.getWeekIncome() : monthSummary.getWeekExpense();

        if (mode == DashboardViewModel.DisplayMode.MASKED) {
            totalText.setText("Rp ***");
            totalText.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
        } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
            if (monthTotalIncomeMinor > 0) {
                float pct = (monthTotal * 100f) / monthTotalIncomeMinor;
                totalText.setText(String.format(Locale.getDefault(), "%.1f%%", pct));
            } else {
                totalText.setText("-");
            }
            totalText.setTextColor(ContextCompat.getColor(context, isIncome ? R.color.finan_income : R.color.finan_expense));
        } else {
            totalText.setText((isIncome ? "+" : "") + MoneyFormatter.format(monthTotal));
            totalText.setTextColor(ContextCompat.getColor(context, isIncome ? R.color.finan_income : R.color.finan_expense));
        }

        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(new MonthlyDetailAdapter(context, monthSummary, isIncome, mode, monthTotalIncomeMinor, categoryDetails));

        dialog.show();
    }

    private static class MonthlyDetailAdapter extends RecyclerView.Adapter<MonthlyDetailAdapter.ViewHolder> {
        private final Context context;
        private final CashFlowReport.WeekSummary monthSummary;
        private final boolean isIncome;
        private final DashboardViewModel.DisplayMode mode;
        private final long monthTotalIncomeMinor;
        private final Map<Long, List<CategoryDetail>> categoryDetails;
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy", new Locale("id", "ID"));

        public MonthlyDetailAdapter(Context context, CashFlowReport.WeekSummary monthSummary, boolean isIncome, DashboardViewModel.DisplayMode mode, long monthTotalIncomeMinor, Map<Long, List<CategoryDetail>> categoryDetails) {
            this.context = context;
            this.monthSummary = monthSummary;
            this.isIncome = isIncome;
            this.mode = mode;
            this.monthTotalIncomeMinor = monthTotalIncomeMinor;
            this.categoryDetails = categoryDetails;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_weekly_detail_day, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CashFlowReport.DailyTotal weekData = monthSummary.getDays().get(position);
            long amount = isIncome ? weekData.getIncomeMinor() : weekData.getExpenseMinor();

            holder.dayName.setText("Minggu " + (position + 1));
            
            long startOfWeek = weekData.getDateMillis();
            LocalDate weekStart = java.time.Instant.ofEpochMilli(startOfWeek).atZone(ZoneId.systemDefault()).toLocalDate();
            LocalDate nextSunday = weekStart.with(java.time.temporal.TemporalAdjusters.nextOrSame(java.time.DayOfWeek.SUNDAY));
            LocalDate monthEnd = monthSummary.getEndDate();
            LocalDate weekEnd = nextSunday.isBefore(monthEnd) ? nextSunday : monthEnd;
            String startStr = dateFormat.format(java.sql.Date.valueOf(weekStart.toString()));
            String endStr = dateFormat.format(java.sql.Date.valueOf(weekEnd.toString()));
            holder.dayDate.setText(startStr + " - " + endStr);

            if (mode == DashboardViewModel.DisplayMode.MASKED) {
                holder.amount.setText("Rp ***");
                holder.amount.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
            } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                if (monthTotalIncomeMinor > 0) {
                    float pct = (amount * 100f) / monthTotalIncomeMinor;
                    holder.amount.setText(String.format(Locale.getDefault(), "%.1f%%", pct));
                } else {
                    holder.amount.setText("-");
                }
                holder.amount.setTextColor(ContextCompat.getColor(context, isIncome ? R.color.finan_income : R.color.finan_expense));
            } else {
                holder.amount.setText((isIncome ? "+" : "") + MoneyFormatter.format(amount));
                holder.amount.setTextColor(ContextCompat.getColor(context, isIncome ? R.color.finan_income : R.color.finan_expense));
            }

            holder.categoriesContainer.removeAllViews();
            List<CategoryDetail> cats = categoryDetails != null ? categoryDetails.get(startOfWeek) : null;
            if (cats != null && !cats.isEmpty()) {
                holder.categoriesContainer.setVisibility(View.VISIBLE);
                for (CategoryDetail cat : cats) {
                    LinearLayout catRow = new LinearLayout(context);
                    catRow.setOrientation(LinearLayout.HORIZONTAL);
                    catRow.setPadding(0, 0, 0, 8); // 8px padding roughly

                    TextView catName = new TextView(context);
                    catName.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    catName.setText("- " + cat.name);
                    catName.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
                    catName.setTextSize(12f);

                    TextView catAmount = new TextView(context);
                    catAmount.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                    catAmount.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
                    catAmount.setTextSize(12f);

                    if (mode == DashboardViewModel.DisplayMode.MASKED) {
                        catAmount.setText("Rp ***");
                    } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                        if (monthTotalIncomeMinor > 0) {
                            float pct = (cat.amount * 100f) / monthTotalIncomeMinor;
                            catAmount.setText(String.format(Locale.getDefault(), "%.1f%%", pct));
                        } else {
                            catAmount.setText("-");
                        }
                    } else {
                        catAmount.setText(MoneyFormatter.format(cat.amount));
                    }

                    catRow.addView(catName);
                    catRow.addView(catAmount);
                    holder.categoriesContainer.addView(catRow);
                }
            } else {
                holder.categoriesContainer.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return monthSummary.getDays().size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView dayName;
            TextView dayDate;
            TextView amount;
            LinearLayout categoriesContainer;

            ViewHolder(View itemView) {
                super(itemView);
                dayName = itemView.findViewById(R.id.weekly_detail_day_name);
                dayDate = itemView.findViewById(R.id.weekly_detail_day_date);
                amount = itemView.findViewById(R.id.weekly_detail_day_amount);
                categoriesContainer = itemView.findViewById(R.id.weekly_detail_categories_container);
            }
        }
    }
}
