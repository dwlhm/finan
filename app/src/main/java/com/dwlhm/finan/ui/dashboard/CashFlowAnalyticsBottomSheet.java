package com.dwlhm.finan.ui.dashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.domain.model.CashFlowActivityTotal;
import com.dwlhm.finan.domain.model.CashFlowReport;
import com.dwlhm.finan.domain.model.CashFlowReportResult;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.domain.model.HistoryTotals;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.common.UiComponentStyles;
import com.dwlhm.finan.ui.summary.WeeklyDetailBottomSheetDialog;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@SuppressLint("SetTextI18n")
public final class CashFlowAnalyticsBottomSheet extends BottomSheetDialog {

    private static final String[] DAY_LABELS = {"Sn", "Sl", "Rb", "Km", "Jm", "Sb", "Mn"};

    private static final int[] EXPENSE_PALETTE = {
            0xFFE74C3C, 0xFFE67E22, 0xFFD35400, 0xFFC0392B, 0xFFE91E63, 0xFF9C27B0, 0xFF795548
    };

    private static final int[] INCOME_PALETTE = {
            0xFF27AE60, 0xFF2ECC71, 0xFF16A085, 0xFF1ABC9C, 0xFF2980B9, 0xFF3498DB, 0xFF4CAF50
    };

    private final AppServices services;
    private final CashFlowReportResult reportResult;
    private final HistoryTotals totals;
    private final DashboardViewModel.DisplayMode displayMode;
    private final int year;
    private final int month;
    @Nullable private final Long walletId;

    public CashFlowAnalyticsBottomSheet(
            @NonNull Context context,
            @NonNull AppServices services,
            @NonNull CashFlowReportResult reportResult,
            @NonNull HistoryTotals totals,
            @NonNull DashboardViewModel.DisplayMode displayMode,
            int year,
            int month,
            @Nullable Long walletId) {
        super(context, R.style.Finan_BottomSheetDialog);
        this.services = services;
        this.reportResult = reportResult;
        this.totals = totals;
        this.displayMode = displayMode;
        this.year = year;
        this.month = month;
        this.walletId = walletId;
        initView();
    }

    public static void show(
            @NonNull Context context,
            @NonNull AppServices services,
            @NonNull CashFlowReportResult reportResult,
            @NonNull HistoryTotals totals,
            @NonNull DashboardViewModel.DisplayMode displayMode,
            int year,
            int month,
            @Nullable Long walletId) {
        CashFlowAnalyticsBottomSheet dialog = new CashFlowAnalyticsBottomSheet(
                context, services, reportResult, totals, displayMode, year, month, walletId);
        BottomSheetHelper.show(dialog);
    }

    private void initView() {
        Context context = getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_cash_flow_analytics, null);
        setContentView(view);

        ImageView btnClose = view.findViewById(R.id.analytics_btn_close);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dismiss());
        }

        TextView tvSubtitle = view.findViewById(R.id.analytics_period_subtitle);
        if (tvSubtitle != null) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"));
            if (month == -1) {
                tvSubtitle.setText(String.valueOf(year));
            } else if (!reportResult.getAllReports().isEmpty()) {
                CashFlowReport rep = reportResult.getAllReports().get(0);
                tvSubtitle.setText(rep.getStartDate().format(fmt) + " – " + rep.getEndDate().format(fmt));
            } else {
                tvSubtitle.setText(LocalDate.of(year, month, 1)
                        .format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"))));
            }
        }

        View weeklyContainer = view.findViewById(R.id.analytics_weekly_container);
        TextView weeklyTitle = view.findViewById(R.id.analytics_weekly_title);
        LinearLayout weeklyRows = view.findViewById(R.id.analytics_weekly_rows);

        LinearLayout catExpenseRows = view.findViewById(R.id.analytics_category_expense_rows);
        LinearLayout catIncomeRows = view.findViewById(R.id.analytics_category_income_rows);

        bindWeeklyActivity(weeklyContainer, weeklyTitle, weeklyRows);
        bindCategoryDistributions(catExpenseRows, catIncomeRows);
    }

    private void bindWeeklyActivity(View container, TextView titleView, LinearLayout rowsLayout) {
        if (container == null || rowsLayout == null) return;
        rowsLayout.removeAllViews();

        List<CashFlowReport.WeekSummary> navigableWeeks = new ArrayList<>();
        for (CashFlowReport report : reportResult.getAllReports()) {
            navigableWeeks.addAll(report.getWeekSummaries());
        }

        if (navigableWeeks.isEmpty()) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);

        if (titleView != null) {
            titleView.setText(month == -1 ? "Aktivitas Bulanan" : "Aktivitas Mingguan");
        }

        long maxDailyAmount = 0L;
        for (CashFlowReport.WeekSummary week : navigableWeeks) {
            if (week.getDays() != null) {
                for (CashFlowReport.DailyTotal d : week.getDays()) {
                    if (d.getIncomeMinor() > maxDailyAmount) maxDailyAmount = d.getIncomeMinor();
                    if (d.getExpenseMinor() > maxDailyAmount) maxDailyAmount = d.getExpenseMinor();
                }
            }
        }
        if (maxDailyAmount <= 0L) maxDailyAmount = 1L;

        for (CashFlowReport.WeekSummary week : navigableWeeks) {
            rowsLayout.addView(createDualWeeklyRow(week, maxDailyAmount, navigableWeeks));
        }
    }

    private View createDualWeeklyRow(
            CashFlowReport.WeekSummary week,
            long maxDailyAmount,
            List<CashFlowReport.WeekSummary> navigableWeeks) {
        Context context = getContext();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(
                UiComponentStyles.dp(context, 10),
                UiComponentStyles.dp(context, 8),
                UiComponentStyles.dp(context, 10),
                UiComponentStyles.dp(context, 8));

        android.util.TypedValue outValue = new android.util.TypedValue();
        context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
        row.setBackgroundResource(outValue.resourceId);
        row.setClickable(true);
        row.setFocusable(true);

        row.setOnClickListener(v -> {
            boolean isIncome = week.getWeekIncome() > week.getWeekExpense();
            long totalIncome = totals.getIncomeMinor();
            services.dbWorker.compute(
                    () -> loadNavigableWeeklyCategoryDetails(navigableWeeks, isIncome, walletId),
                    result -> WeeklyDetailBottomSheetDialog.show(
                            context,
                            week,
                            isIncome,
                            displayMode,
                            totalIncome,
                            result.detailsByWeek,
                            navigableWeeks,
                            month == -1,
                            row));
        });

        // Top Header of row: Period Label + Income/Expense Summary Totals + Drill-down arrow
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView periodLabel = new TextView(context);
        if (month == -1) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID"));
            periodLabel.setText(week.getStartDate().format(fmt));
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("d MMM", Locale.forLanguageTag("id-ID"));
            periodLabel.setText(week.getStartDate().format(fmt) + " – " + week.getEndDate().format(fmt));
        }
        periodLabel.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
        periodLabel.setTextSize(12f);
        periodLabel.setTypeface(periodLabel.getTypeface(), Typeface.BOLD);
        headerRow.addView(periodLabel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Summary Totals (+Rp X / -Rp Y)
        LinearLayout totalsLayout = new LinearLayout(context);
        totalsLayout.setOrientation(LinearLayout.HORIZONTAL);
        totalsLayout.setGravity(Gravity.CENTER_VERTICAL);
        totalsLayout.setPadding(0, 0, UiComponentStyles.dp(context, 6), 0);

        long weekIncome = week.getWeekIncome();
        long weekExpense = week.getWeekExpense();

        if (weekIncome > 0 || displayMode == DashboardViewModel.DisplayMode.MASKED) {
            TextView incomeView = new TextView(context);
            incomeView.setTextSize(11f);
            incomeView.setTypeface(incomeView.getTypeface(), Typeface.BOLD);
            incomeView.setTextColor(ContextCompat.getColor(context, R.color.finan_income));
            if (displayMode == DashboardViewModel.DisplayMode.MASKED) {
                incomeView.setText("+••••••");
            } else if (displayMode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                long totalInc = totals.getIncomeMinor();
                if (totalInc > 0) {
                    double pct = (weekIncome * 100.0) / totalInc;
                    incomeView.setText(String.format(Locale.getDefault(), "+%.0f%%", pct));
                } else {
                    incomeView.setText("+0%");
                }
            } else {
                incomeView.setText("+" + MoneyFormatter.format(weekIncome));
            }
            totalsLayout.addView(incomeView);
        }

        if (weekExpense > 0 || displayMode == DashboardViewModel.DisplayMode.MASKED) {
            TextView expenseView = new TextView(context);
            expenseView.setTextSize(11f);
            expenseView.setTypeface(expenseView.getTypeface(), Typeface.BOLD);
            expenseView.setTextColor(ContextCompat.getColor(context, R.color.finan_expense));
            if (weekIncome > 0 || displayMode == DashboardViewModel.DisplayMode.MASKED) {
                LinearLayout.LayoutParams expLp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                expLp.setMarginStart(UiComponentStyles.dp(context, 6));
                expenseView.setLayoutParams(expLp);
            }
            if (displayMode == DashboardViewModel.DisplayMode.MASKED) {
                expenseView.setText("-••••••");
            } else if (displayMode == DashboardViewModel.DisplayMode.PERCENTAGE) {
                long totalExp = totals.getExpenseMinor();
                if (totalExp > 0) {
                    double pct = (weekExpense * 100.0) / totalExp;
                    expenseView.setText(String.format(Locale.getDefault(), "-%.0f%%", pct));
                } else {
                    expenseView.setText("-0%");
                }
            } else {
                expenseView.setText("-" + MoneyFormatter.format(weekExpense));
            }
            totalsLayout.addView(expenseView);
        }

        headerRow.addView(totalsLayout);

        TextView drillArrow = new TextView(context);
        drillArrow.setText("›");
        drillArrow.setTextSize(14f);
        drillArrow.setTypeface(drillArrow.getTypeface(), Typeface.BOLD);
        drillArrow.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
        headerRow.addView(drillArrow);

        row.addView(headerRow);

        // 7-Day Micro Bar Grid
        row.addView(createDailyBarGrid(context, week, maxDailyAmount));

        return row;
    }

    private View createDailyBarGrid(
            Context context,
            CashFlowReport.WeekSummary week,
            long maxDailyAmount) {
        LinearLayout grid = new LinearLayout(context);
        grid.setOrientation(LinearLayout.HORIZONTAL);
        grid.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        grid.setPadding(0, UiComponentStyles.dp(context, 8), 0, UiComponentStyles.dp(context, 4));

        boolean isWeeklyMode = (month != -1);
        int columnCount = isWeeklyMode ? 7 : (week.getDays() != null ? week.getDays().size() : 0);
        if (columnCount == 0) return grid;

        CashFlowReport.DailyTotal[] slots;
        String[] labels;

        if (isWeeklyMode) {
            slots = new CashFlowReport.DailyTotal[7];
            labels = DAY_LABELS;
            ZoneId zoneId = ZoneId.systemDefault();
            if (week.getDays() != null) {
                for (CashFlowReport.DailyTotal day : week.getDays()) {
                    if (day == null) continue;
                    LocalDate date = Instant.ofEpochMilli(day.getDateMillis())
                            .atZone(zoneId)
                            .toLocalDate();
                    int dow = date.getDayOfWeek().getValue(); // 1 (Mon) .. 7 (Sun)
                    if (dow >= 1 && dow <= 7) {
                        CashFlowReport.DailyTotal existing = slots[dow - 1];
                        if (existing == null) {
                            slots[dow - 1] = day;
                        } else {
                            slots[dow - 1] = new CashFlowReport.DailyTotal(
                                    day.getDateMillis(),
                                    existing.getIncomeMinor() + day.getIncomeMinor(),
                                    existing.getExpenseMinor() + day.getExpenseMinor()
                            );
                        }
                    }
                }
            }
        } else {
            slots = new CashFlowReport.DailyTotal[columnCount];
            labels = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                slots[i] = week.getDays().get(i);
                labels[i] = "M" + (i + 1);
            }
        }

        int barWidth = UiComponentStyles.dp(context, 4);
        int cornerRadius = UiComponentStyles.dp(context, 2);
        int minBarHeight = UiComponentStyles.dp(context, 3);
        int maxBarHeight = UiComponentStyles.dp(context, 32);
        int barAreaHeight = UiComponentStyles.dp(context, 34);

        for (int i = 0; i < columnCount; i++) {
            CashFlowReport.DailyTotal day = slots[i];
            long income = day != null ? day.getIncomeMinor() : 0L;
            long expense = day != null ? day.getExpenseMinor() : 0L;
            String label = labels[i];

            LinearLayout column = new LinearLayout(context);
            column.setOrientation(LinearLayout.VERTICAL);
            column.setGravity(Gravity.CENTER_HORIZONTAL);
            LinearLayout.LayoutParams colLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
            column.setLayoutParams(colLp);

            // Bar container: bottom-aligned
            LinearLayout barContainer = new LinearLayout(context);
            barContainer.setOrientation(LinearLayout.HORIZONTAL);
            barContainer.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            barContainer.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, barAreaHeight));

            if (income <= 0 && expense <= 0) {
                // Neutral empty indicator dot/strip
                View emptyDot = new View(context);
                GradientDrawable dotShape = new GradientDrawable();
                dotShape.setShape(GradientDrawable.RECTANGLE);
                dotShape.setColor(ContextCompat.getColor(context, R.color.finan_divider));
                dotShape.setCornerRadius(UiComponentStyles.dp(context, 1));
                emptyDot.setBackground(dotShape);
                LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                        UiComponentStyles.dp(context, 8), UiComponentStyles.dp(context, 2));
                dotLp.bottomMargin = UiComponentStyles.dp(context, 2);
                barContainer.addView(emptyDot, dotLp);
            } else {
                if (income > 0) {
                    View inBar = new View(context);
                    GradientDrawable inShape = new GradientDrawable();
                    inShape.setShape(GradientDrawable.RECTANGLE);
                    inShape.setColor(ContextCompat.getColor(context, R.color.finan_income));
                    inShape.setCornerRadii(new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0, 0, 0, 0});
                    inBar.setBackground(inShape);
                    int h = Math.max(minBarHeight, (int) (maxBarHeight * ((float) income / maxDailyAmount)));
                    LinearLayout.LayoutParams inLp = new LinearLayout.LayoutParams(barWidth, h);
                    barContainer.addView(inBar, inLp);
                }
                if (expense > 0) {
                    View exBar = new View(context);
                    GradientDrawable exShape = new GradientDrawable();
                    exShape.setShape(GradientDrawable.RECTANGLE);
                    exShape.setColor(ContextCompat.getColor(context, R.color.finan_expense));
                    exShape.setCornerRadii(new float[]{cornerRadius, cornerRadius, cornerRadius, cornerRadius, 0, 0, 0, 0});
                    exBar.setBackground(exShape);
                    int h = Math.max(minBarHeight, (int) (maxBarHeight * ((float) expense / maxDailyAmount)));
                    LinearLayout.LayoutParams exLp = new LinearLayout.LayoutParams(barWidth, h);
                    if (income > 0) {
                        exLp.setMarginStart(UiComponentStyles.dp(context, 2));
                    }
                    barContainer.addView(exBar, exLp);
                }
            }
            column.addView(barContainer);

            // Day label
            TextView dayLabel = new TextView(context);
            dayLabel.setText(label);
            dayLabel.setTextSize(9.5f);
            dayLabel.setGravity(Gravity.CENTER);
            dayLabel.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
            LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lblLp.topMargin = UiComponentStyles.dp(context, 4);
            column.addView(dayLabel, lblLp);

            grid.addView(column);
        }

        return grid;
    }

    private void bindCategoryDistributions(LinearLayout expenseLayout, LinearLayout incomeLayout) {
        if (expenseLayout != null) {
            expenseLayout.removeAllViews();
            renderCategorySection(expenseLayout, false);
        }
        if (incomeLayout != null) {
            incomeLayout.removeAllViews();
            renderCategorySection(incomeLayout, true);
        }
    }

    private void renderCategorySection(LinearLayout container, boolean isIncome) {
        Context context = getContext();
        Map<Long, Long> amountByCatId = new HashMap<>();
        Map<Long, String> nameByCatId = new HashMap<>();

        long totalAmount = 0;
        long maxAmount = 0;

        for (CashFlowReport report : reportResult.getAllReports()) {
            for (CashFlowActivityTotal act : report.getActivityTotals()) {
                List<CategoryTotal> cats = isIncome ? act.getIncomeCategories() : act.getExpenseCategories();
                for (CategoryTotal cat : cats) {
                    Long current = amountByCatId.getOrDefault(cat.getCategoryId(), 0L);
                    long newVal = (current != null ? current : 0L) + cat.getTotalMinor();
                    amountByCatId.put(cat.getCategoryId(), newVal);
                    nameByCatId.put(cat.getCategoryId(), cat.getCategoryName());
                    if (newVal > maxAmount) maxAmount = newVal;
                }
            }
        }

        for (long amount : amountByCatId.values()) {
            totalAmount += amount;
        }

        if (amountByCatId.isEmpty() || totalAmount == 0) {
            TextView emptyText = new TextView(context);
            emptyText.setText(isIncome ? "Belum ada pemasukan" : "Belum ada pengeluaran");
            emptyText.setTextColor(ContextCompat.getColor(context, R.color.finan_text_hint));
            emptyText.setTextSize(11f);
            emptyText.setPadding(0, UiComponentStyles.dp(context, 4), 0, UiComponentStyles.dp(context, 4));
            container.addView(emptyText);
            return;
        }

        List<Map.Entry<Long, Long>> sorted = new ArrayList<>(amountByCatId.entrySet());
        sorted.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        int[] palette = isIncome ? INCOME_PALETTE : EXPENSE_PALETTE;
        int i = 0;
        for (Map.Entry<Long, Long> entry : sorted) {
            String catName = nameByCatId.get(entry.getKey());
            long val = entry.getValue();
            if (val > 0) {
                container.addView(createCategoryBarRow(catName, val, maxAmount, totalAmount, palette[i % palette.length]));
                i++;
            }
        }
    }

    private View createCategoryBarRow(
            String categoryName,
            long amount,
            long maxAmount,
            long totalAmount,
            int color) {
        Context context = getContext();
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, UiComponentStyles.dp(context, 5), 0, UiComponentStyles.dp(context, 5));

        TextView label = new TextView(context);
        label.setText(categoryName);
        label.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
        label.setTextSize(11f);
        label.setTypeface(label.getTypeface(), Typeface.BOLD);
        label.setMaxLines(1);
        label.setEllipsize(android.text.TextUtils.TruncateAt.END);

        LinearLayout.LayoutParams labelLp = new LinearLayout.LayoutParams(
                UiComponentStyles.dp(context, 84), ViewGroup.LayoutParams.WRAP_CONTENT);
        labelLp.setMarginEnd(UiComponentStyles.dp(context, 8));
        row.addView(label, labelLp);

        int barHeight = UiComponentStyles.dp(context, 20);
        int cornerRadius = UiComponentStyles.dp(context, 5);

        LinearLayout barContainer = new LinearLayout(context);
        barContainer.setOrientation(LinearLayout.HORIZONTAL);
        barContainer.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        barContainer.setWeightSum((float) maxAmount);

        LinearLayout bar = new LinearLayout(context);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setLayoutParams(new LinearLayout.LayoutParams(0, barHeight, (float) amount));
        bar.setMinimumHeight(barHeight);

        View segment = new View(context);
        GradientDrawable shape = new GradientDrawable();
        shape.setShape(GradientDrawable.RECTANGLE);
        shape.setColor(color);
        shape.setCornerRadius(cornerRadius);
        segment.setBackground(shape);
        segment.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bar.addView(segment);

        barContainer.addView(bar);
        row.addView(barContainer);

        TextView amountLabel = new TextView(context);
        if (displayMode == DashboardViewModel.DisplayMode.MASKED) {
            amountLabel.setText("••••••");
        } else if (displayMode == DashboardViewModel.DisplayMode.PERCENTAGE) {
            double percent = totalAmount > 0 ? (amount * 100.0 / totalAmount) : 0;
            amountLabel.setText(String.format(Locale.getDefault(), "%.0f%%", percent));
        } else {
            double percent = totalAmount > 0 ? (amount * 100.0 / totalAmount) : 0;
            amountLabel.setText(MoneyFormatter.format(amount) + " (" + String.format(Locale.getDefault(), "%.0f%%", percent) + ")");
        }
        amountLabel.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
        amountLabel.setTextSize(11f);
        amountLabel.setTypeface(amountLabel.getTypeface(), Typeface.BOLD);
        amountLabel.setGravity(Gravity.END);
        LinearLayout.LayoutParams amtLp = new LinearLayout.LayoutParams(
                UiComponentStyles.dp(context, 110), ViewGroup.LayoutParams.WRAP_CONTENT);
        amtLp.setMarginStart(UiComponentStyles.dp(context, 8));
        row.addView(amountLabel, amtLp);

        return row;
    }

    private Map<Long, List<WeeklyDetailBottomSheetDialog.CategoryDetail>>
            loadWeeklyCategoryDetails(CashFlowReport.WeekSummary week, boolean isIncome,
                    @Nullable Long walletId) {
        Map<Long, List<WeeklyDetailBottomSheetDialog.CategoryDetail>> result = new HashMap<>();
        String type = isIncome ? "INCOME" : "EXPENSE";
        for (CashFlowReport.DailyTotal day : week.getDays()) {
            long startOfDay = day.getDateMillis();
            long endOfDay = startOfDay + 86400000L;
            List<WeeklyDetailBottomSheetDialog.CategoryDetail> details = new ArrayList<>();
            for (com.dwlhm.finan.data.dao.SummaryDao.CategorySumRow sumRow
                    : services.summaryDao.categoryTotalsBetween(type, startOfDay, endOfDay, walletId, 50)) {
                if (sumRow.categoryId <= 0) continue;
                Category category = services.categoryDao.findById(sumRow.categoryId);
                String name = category != null ? category.getName() : ("#" + sumRow.categoryId);
                String icon = category != null ? category.getIcon() : null;
                details.add(new WeeklyDetailBottomSheetDialog.CategoryDetail(name, icon, sumRow.totalMinor));
            }
            result.put(startOfDay, details);
        }
        return result;
    }

    private WeeklyCategoryDetailsResult loadNavigableWeeklyCategoryDetails(
            List<CashFlowReport.WeekSummary> weeks, boolean isIncome, @Nullable Long walletId) {
        WeeklyCategoryDetailsResult result = new WeeklyCategoryDetailsResult();
        for (CashFlowReport.WeekSummary week : weeks) {
            long key = week.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            try {
                result.detailsByWeek.put(key, loadWeeklyCategoryDetails(week, isIncome, walletId));
            } catch (RuntimeException error) {
                result.hasError = true;
            }
        }
        return result;
    }

    private static final class WeeklyCategoryDetailsResult {
        private final Map<Long, Map<Long, List<WeeklyDetailBottomSheetDialog.CategoryDetail>>> detailsByWeek =
                new HashMap<>();
        private boolean hasError;
    }
}
