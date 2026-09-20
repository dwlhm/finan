package com.dwlhm.finan.ui.summary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.CashFlowReport;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.dashboard.DashboardViewModel;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.text.SimpleDateFormat;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@SuppressLint("SetTextI18n")
public class WeeklyDetailBottomSheetDialog {
    /** Read-only category amount rendered inside an expanded weekly day row. */
    public static class CategoryDetail {
        private final String name;
        private final String icon;
        private final long amount;

        /** Creates a category detail; the icon may be absent for unresolved categories. */
        public CategoryDetail(String name, @Nullable String icon, long amount) {
            this.name = name;
            this.icon = icon;
            this.amount = amount;
        }

        /** Returns the category display name or its unresolved-ID fallback. */
        public String getName() { return name; }
        /** Returns the optional category icon. */
        @Nullable public String getIcon() { return icon; }
        /** Returns the category amount in minor currency units. */
        public long getAmount() { return amount; }
    }

    /**
     * Shows the read-only weekly detail sheet for the selected display mode.
     *
     * @param mode nominal, percentage, or masked amount presentation
     * @param triggerView nullable opener used for guarded focus restoration
     */
    @SuppressLint("InflateParams")
    public static void show(Context context, CashFlowReport.WeekSummary weekSummary,
            boolean isIncome, DashboardViewModel.DisplayMode mode,
            long percentageDenominatorMinor,
            Map<Long, Map<Long, List<CategoryDetail>>> detailsByWeek,
            List<CashFlowReport.WeekSummary> navigableWeeks, boolean isYearly,
            @Nullable View triggerView) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View sheetView = LayoutInflater.from(context).inflate(R.layout.dialog_weekly_detail, null);
        dialog.setContentView(sheetView);
        RecyclerView recycler = sheetView.findViewById(R.id.weekly_detail_recycler);
        WeeklyDetailAdapter adapter = new WeeklyDetailAdapter(context, weekSummary, isIncome, mode,
                percentageDenominatorMinor, null, isYearly, false);
        recycler.setLayoutManager(new LinearLayoutManager(context));
        recycler.setAdapter(adapter);
        int selectedIndex = navigableWeeks.indexOf(weekSummary);
        if (selectedIndex < 0) selectedIndex = 0;
        bindWeek(context, dialog, sheetView, weekSummary, selectedIndex, isIncome, mode,
                percentageDenominatorMinor, detailsByWeek, navigableWeeks, isYearly, adapter);
        sheetView.findViewById(R.id.weekly_detail_close).setOnClickListener(v -> dialog.dismiss());
        dialog.setOnDismissListener(d -> {
            if (triggerView != null && triggerView.isAttachedToWindow()) triggerView.requestFocus();
        });
        BottomSheetHelper.show(dialog);
    }

    private static void bindWeek(Context context, BottomSheetDialog dialog, View sheetView,
            CashFlowReport.WeekSummary week, int selectedIndex, boolean isIncome,
            DashboardViewModel.DisplayMode mode, long percentageDenominatorMinor,
            Map<Long, Map<Long, List<CategoryDetail>>> detailsByWeek,
            List<CashFlowReport.WeekSummary> navigableWeeks, boolean isYearly,
            WeeklyDetailAdapter adapter) {
        TextView title = sheetView.findViewById(R.id.weekly_detail_title);
        TextView dateRange = sheetView.findViewById(R.id.weekly_detail_date_range);
        TextView total = sheetView.findViewById(R.id.weekly_detail_total);
        TextView comparison = sheetView.findViewById(R.id.weekly_detail_comparison);
        TextView comparisonLabel = sheetView.findViewById(R.id.weekly_detail_comparison_label);
        TextView state = sheetView.findViewById(R.id.weekly_detail_state);
        title.setText(R.string.weekly_detail_activity);
        if (isYearly) {
            String yearLabel = context.getString(R.string.weekly_detail_year, week.getStartDate().getYear());
            dateRange.setText(yearLabel);
        } else {
            SimpleDateFormat format = new SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"));
            String rangeLabel = format.format(java.sql.Date.valueOf(week.getStartDate().toString()))
                    + " - " + format.format(java.sql.Date.valueOf(week.getEndDate().toString()));
            dateRange.setText(rangeLabel);
        }

        long current = isIncome ? week.getWeekIncome() : week.getWeekExpense();
        renderAmount(total, current, mode, percentageDenominatorMinor, isIncome);
        Long previousValue = null;
        if (selectedIndex > 0) {
            CashFlowReport.WeekSummary previousWeek = navigableWeeks.get(selectedIndex - 1);
            previousValue = isIncome ? previousWeek.getWeekIncome() : previousWeek.getWeekExpense();
        }
        long signedCurrent = isIncome ? current : -current;
        Long signedPrevious = previousValue == null ? null
                : (isIncome ? previousValue : -previousValue);
        renderComparison(comparison, comparisonLabel, signedCurrent, signedPrevious, mode);

        long weekKey = week.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        Map<Long, List<CategoryDetail>> dayDetails = detailsByWeek.get(weekKey);
        boolean detailError = detailsByWeek.size() < navigableWeeks.size() || dayDetails == null;
        state.setVisibility(detailError || week.getDays().isEmpty() ? View.VISIBLE : View.GONE);
        state.setText(detailError ? R.string.weekly_detail_error : R.string.weekly_detail_empty);
        adapter.setWeek(week, dayDetails, detailError);

    }

    private static void renderAmount(TextView target, long amountMinor,
            DashboardViewModel.DisplayMode mode, long denominatorMinor, boolean isIncome) {
        long signedAmount = isIncome ? amountMinor : -amountMinor;
        if (mode == DashboardViewModel.DisplayMode.MASKED) {
            target.setText(R.string.weekly_detail_masked_amount);
            target.setTextColor(ContextCompat.getColor(target.getContext(), R.color.finan_text_secondary));
        } else if (mode == DashboardViewModel.DisplayMode.PERCENTAGE) {
            if (denominatorMinor <= 0) {
                target.setText("-");
                target.setTextColor(ContextCompat.getColor(target.getContext(), R.color.finan_text_secondary));
            } else if (signedAmount == 0) {
                target.setText("0.0%");
                target.setTextColor(ContextCompat.getColor(target.getContext(), R.color.finan_text_secondary));
            } else {
                target.setText(String.format(Locale.getDefault(), "%+.1f%%",
                        signedAmount * 100f / denominatorMinor));
                setAmountTextColor(target, signedAmount);
            }
        } else {
            target.setText(signedAmount > 0 ? "+" + MoneyFormatter.format(signedAmount)
                    : MoneyFormatter.format(signedAmount));
            setAmountTextColor(target, signedAmount);
        }
    }

    private static void setAmountTextColor(TextView target, long signedAmount) {
        int color = signedAmount == 0 ? R.color.finan_text_secondary
                : signedAmount > 0 ? R.color.finan_income : R.color.finan_expense;
        target.setTextColor(ContextCompat.getColor(target.getContext(), color));
    }

    private static void renderComparison(TextView value, TextView label, long currentMinor,
            @Nullable Long previousMinor, DashboardViewModel.DisplayMode mode) {
        label.setText(R.string.weekly_detail_comparison_label);
        if (mode == DashboardViewModel.DisplayMode.MASKED || previousMinor == null) {
            value.setText(mode == DashboardViewModel.DisplayMode.MASKED
                    ? R.string.weekly_detail_masked_amount : R.string.weekly_detail_no_comparison);
            setComparisonStyle(value, R.color.finan_text_secondary);
        } else if (previousMinor == 0) {
            if (currentMinor == 0) value.setText("0%");
            else value.setText(R.string.weekly_detail_new_value);
            setComparisonStyle(value, R.color.finan_text_secondary);
        } else {
            float change = (currentMinor - previousMinor) * 100f / Math.abs(previousMinor);
            if (change > 0) {
                value.setText(value.getContext().getString(R.string.weekly_detail_change_up, change));
                setComparisonStyle(value, R.color.finan_income);
            } else if (change < 0) {
                value.setText(value.getContext().getString(R.string.weekly_detail_change_down, -change));
                setComparisonStyle(value, R.color.finan_expense);
            } else {
                value.setText("0%");
                setComparisonStyle(value, R.color.finan_text_secondary);
            }
        }
    }

    private static void setComparisonStyle(TextView value, int textColorRes) {
        value.setTextColor(ContextCompat.getColor(value.getContext(), textColorRes));
    }

    private static class WeeklyDetailAdapter extends RecyclerView.Adapter<WeeklyDetailAdapter.ViewHolder> {
        private final Context context;
        private final boolean isIncome;
        private final DashboardViewModel.DisplayMode mode;
        private final long denominatorMinor;
        private final boolean isYearly;
        private final Set<Long> expandedDays = new HashSet<>();
        private final SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.forLanguageTag("id-ID"));
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("id-ID"));
        private CashFlowReport.WeekSummary weekSummary;
        private Map<Long, List<CategoryDetail>> categoryDetails;
        private boolean detailError;

        WeeklyDetailAdapter(Context context, CashFlowReport.WeekSummary weekSummary, boolean isIncome,
                DashboardViewModel.DisplayMode mode, long denominatorMinor,
                @Nullable Map<Long, List<CategoryDetail>> categoryDetails, boolean isYearly,
                boolean detailError) {
            this.context = context;
            this.weekSummary = weekSummary;
            this.isIncome = isIncome;
            this.mode = mode;
            this.denominatorMinor = denominatorMinor;
            this.categoryDetails = categoryDetails;
            this.isYearly = isYearly;
            this.detailError = detailError;
        }

        void setWeek(CashFlowReport.WeekSummary week, @Nullable Map<Long, List<CategoryDetail>> details,
                boolean error) {
            weekSummary = week;
            categoryDetails = details;
            detailError = error;
            expandedDays.clear();
            notifyDataSetChanged();
        }

        void toggleDay(long dateMillis) {
            if (expandedDays.contains(dateMillis)) expandedDays.remove(dateMillis);
            else expandedDays.add(dateMillis);
            int position = findDayPosition(dateMillis);
            if (position != RecyclerView.NO_POSITION) notifyItemChanged(position);
        }

        private int findDayPosition(long dateMillis) {
            for (int i = 0; i < weekSummary.getDays().size(); i++) {
                if (weekSummary.getDays().get(i).getDateMillis() == dateMillis) return i;
            }
            return RecyclerView.NO_POSITION;
        }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(context)
                    .inflate(R.layout.item_weekly_detail_day, parent, false));
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CashFlowReport.DailyTotal day = weekSummary.getDays().get(position);
            long amount = isIncome ? day.getIncomeMinor() : day.getExpenseMinor();
            Date date = new Date(day.getDateMillis());
            holder.dayName.setText(isYearly
                    ? new SimpleDateFormat("MMMM", Locale.forLanguageTag("id-ID")).format(date)
                    : dayFormat.format(date));
            holder.dayDate.setText(isYearly
                    ? new SimpleDateFormat("yyyy", Locale.forLanguageTag("id-ID")).format(date)
                    : dateFormat.format(date));
            renderAmount(holder.amount, amount, mode, denominatorMinor, isIncome);
            List<CategoryDetail> categories = categoryDetails == null || detailError
                    ? null : categoryDetails.get(day.getDateMillis());
            boolean canExpand = categories != null && !categories.isEmpty();
            boolean expanded = expandedDays.contains(day.getDateMillis()) && canExpand;
            holder.expand.setVisibility(canExpand ? View.VISIBLE : View.GONE);
            holder.expand.setRotation(expanded ? 180f : 0f);
            holder.categoriesContainer.removeAllViews();
            holder.categoriesContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
            if (expanded) {
                for (CategoryDetail category : categories) {
                    holder.categoriesContainer.addView(createCategoryRow(category));
                }
            }
            holder.itemView.setClickable(true);
            holder.itemView.setFocusable(true);
            holder.itemView.setOnClickListener(v -> { if (canExpand) toggleDay(day.getDateMillis()); });
        }

        private View createCategoryRow(CategoryDetail category) {
            LinearLayout categoryView = new LinearLayout(context);
            categoryView.setOrientation(LinearLayout.HORIZONTAL);
            categoryView.setGravity(android.view.Gravity.CENTER_VERTICAL);
            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            rowParams.bottomMargin = com.dwlhm.finan.ui.common.UiComponentStyles.dp(context, 8);
            categoryView.setLayoutParams(rowParams);

            TextView icon = new TextView(context);
            String iconValue = category.getIcon();
            icon.setText(iconValue == null || iconValue.trim().isEmpty() ? "📂" : iconValue);
            icon.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
            icon.setTextSize(16f);
            icon.setGravity(android.view.Gravity.CENTER);
            icon.setIncludeFontPadding(false);
            icon.setLayoutParams(new LinearLayout.LayoutParams(
                    com.dwlhm.finan.ui.common.UiComponentStyles.dp(context, 24),
                    com.dwlhm.finan.ui.common.UiComponentStyles.dp(context, 24)));

            TextView name = new TextView(context);
            name.setText(category.getName());
            name.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
            name.setTextSize(12f);
            LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            nameParams.setMarginStart(com.dwlhm.finan.ui.common.UiComponentStyles.dp(context, 8));
            name.setLayoutParams(nameParams);

            TextView value = new TextView(context);
            value.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
            value.setTextSize(12f);
            LinearLayout.LayoutParams valueParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            valueParams.setMarginStart(com.dwlhm.finan.ui.common.UiComponentStyles.dp(context, 8));
            value.setLayoutParams(valueParams);
            renderAmount(value, category.getAmount(), mode, denominatorMinor, isIncome);

            categoryView.addView(icon);
            categoryView.addView(name);
            categoryView.addView(value);
            return categoryView;
        }

        @Override public int getItemCount() { return weekSummary == null ? 0 : weekSummary.getDays().size(); }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView dayName, dayDate, amount;
            final ImageView expand;
            final LinearLayout categoriesContainer;
            ViewHolder(View itemView) {
                super(itemView);
                dayName = itemView.findViewById(R.id.weekly_detail_day_name);
                dayDate = itemView.findViewById(R.id.weekly_detail_day_date);
                amount = itemView.findViewById(R.id.weekly_detail_day_amount);
                expand = itemView.findViewById(R.id.weekly_detail_day_expand);
                categoriesContainer = itemView.findViewById(R.id.weekly_detail_categories_container);
            }
        }
    }
}
