package com.dwlhm.finan.ui.summary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FinancialAdviceDialog {

    public static class RawAdviceParts {
        public final String mainMessageHtml;
        @Nullable public final String tipsTitle;
        @Nullable public final String tipsContentHtml;

        public RawAdviceParts(String mainMessageHtml, @Nullable String tipsTitle, @Nullable String tipsContentHtml) {
            this.mainMessageHtml = mainMessageHtml;
            this.tipsTitle = tipsTitle;
            this.tipsContentHtml = tipsContentHtml;
        }
    }

    public static class ParsedAdvice {
        public final CharSequence mainMessage;
        @Nullable public final String tipsTitle;
        @Nullable public final CharSequence tipsContent;

        public ParsedAdvice(CharSequence mainMessage, @Nullable String tipsTitle, @Nullable CharSequence tipsContent) {
            this.mainMessage = mainMessage;
            this.tipsTitle = tipsTitle;
            this.tipsContent = tipsContent;
        }
    }

    public static class PaceCalculation {
        public final int elapsedDays;
        public final int totalDays;
        public final float timePercent;
        public final float expensePercent;
        public final boolean hasTransactions;
        public final boolean isControlled;

        public PaceCalculation(int elapsedDays, int totalDays, float timePercent, float expensePercent, boolean hasTransactions, boolean isControlled) {
            this.elapsedDays = elapsedDays;
            this.totalDays = totalDays;
            this.timePercent = timePercent;
            this.expensePercent = expensePercent;
            this.hasTransactions = hasTransactions;
            this.isControlled = isControlled;
        }
    }

    public static void show(Context context, FinancialAdvisor.Advice advice, MonthlySummary summary, DisplayMode mode) {
        show(context, advice, summary, mode, null);
    }

    @SuppressLint("InflateParams")
    public static void show(Context context, FinancialAdvisor.Advice advice, MonthlySummary summary, DisplayMode mode, @Nullable LocalDate referenceDate) {
        if (advice == null || summary == null || context == null) return;

        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Finan_BottomSheetDialog);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_financial_advice, null);
        dialog.setContentView(view);

        // Header Row
        TextView titleText = view.findViewById(R.id.advice_dialog_title);
        TextView subtitleText = view.findViewById(R.id.advice_dialog_subtitle);
        ImageView btnClose = view.findViewById(R.id.advice_dialog_btn_close);

        if (titleText != null) {
            titleText.setText(advice.title != null && !advice.title.isEmpty()
                    ? advice.title
                    : context.getString(R.string.hc_dialog_financial_advice_nasihat_keuangan));
        }

        if (subtitleText != null) {
            int year = summary.getYear();
            int month = summary.getMonth();
            if (year > 0 && month >= 1 && month <= 12) {
                YearMonth ym = YearMonth.of(year, month);
                String monthYear = ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("id-ID")));
                subtitleText.setText(context.getString(R.string.advice_dialog_subtitle_format, monthYear));
            } else {
                subtitleText.setText(context.getString(R.string.advice_dialog_subtitle_default));
            }
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        // Section 1: Insight / Evaluasi Card & Tips Callout
        TextView messageText = view.findViewById(R.id.advice_dialog_message);
        View tipsContainer = view.findViewById(R.id.advice_dialog_tips_container);
        TextView tipsTitleText = view.findViewById(R.id.advice_dialog_tips_title);
        TextView tipsContentText = view.findViewById(R.id.advice_dialog_tips_text);

        ParsedAdvice parsedAdvice = parseAdviceMessage(advice.message);
        if (messageText != null) {
            messageText.setText(parsedAdvice.mainMessage);
        }

        if (tipsContainer != null) {
            if (parsedAdvice.tipsContent != null && parsedAdvice.tipsContent.length() > 0) {
                tipsContainer.setVisibility(View.VISIBLE);
                if (tipsTitleText != null) {
                    tipsTitleText.setText(parsedAdvice.tipsTitle != null
                            ? parsedAdvice.tipsTitle
                            : context.getString(R.string.advice_dialog_tips_header_default));
                }
                if (tipsContentText != null) {
                    tipsContentText.setText(parsedAdvice.tipsContent);
                }
            } else {
                tipsContainer.setVisibility(View.GONE);
            }
        }

        // Section 2: Pace Meter (Waktu vs Pengeluaran)
        TextView paceStatusBadge = view.findViewById(R.id.advice_dialog_pace_status_badge);
        TextView paceStatusText = view.findViewById(R.id.advice_dialog_pace_status_text);
        TextView paceTimeLabel = view.findViewById(R.id.advice_dialog_pace_time_label);
        ProgressBar paceTimeProgress = view.findViewById(R.id.advice_dialog_pace_time_progress);
        TextView paceExpenseLabel = view.findViewById(R.id.advice_dialog_pace_expense_label);
        ProgressBar paceExpenseProgress = view.findViewById(R.id.advice_dialog_pace_expense_progress);
        TextView paceTimeText = view.findViewById(R.id.advice_dialog_pace_time_text);

        PaceCalculation pace = calculatePace(summary, referenceDate);

        if (paceTimeLabel != null) {
            paceTimeLabel.setText(context.getString(R.string.advice_dialog_pace_time_label_format,
                    pace.elapsedDays, pace.totalDays, pace.timePercent));
        }
        if (paceTimeProgress != null) {
            paceTimeProgress.setProgress(Math.min(100, Math.max(0, Math.round(pace.timePercent))));
            paceTimeProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.finan_primary)));
        }

        long income = summary.getMonthIncomeMinor();
        long expense = summary.getMonthExpenseMinor();

        if (paceExpenseLabel != null) {
            if (income > 0) {
                paceExpenseLabel.setText(String.format(Locale.getDefault(), "%.1f%%", pace.expensePercent));
            } else if (expense > 0) {
                paceExpenseLabel.setText("> 100%");
            } else {
                paceExpenseLabel.setText("0%");
            }
        }
        if (paceExpenseProgress != null) {
            paceExpenseProgress.setProgress(Math.min(100, Math.max(0, Math.round(pace.expensePercent))));
        }

        if (paceTimeText != null) {
            paceTimeText.setText(context.getString(R.string.advice_dialog_pace_time_format,
                    pace.elapsedDays, pace.totalDays, pace.timePercent));
        }

        if (!pace.hasTransactions) {
            if (paceStatusText != null) {
                paceStatusText.setText(R.string.advice_dialog_pace_status_nodata);
            }
            if (paceStatusBadge != null) {
                paceStatusBadge.setText(R.string.advice_dialog_pace_badge_nodata);
                paceStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
            }
            if (paceExpenseProgress != null) {
                paceExpenseProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.finan_text_secondary)));
            }
        } else if (pace.isControlled) {
            if (paceStatusText != null) {
                paceStatusText.setText(R.string.advice_dialog_pace_status_controlled);
            }
            if (paceStatusBadge != null) {
                paceStatusBadge.setText(R.string.advice_dialog_pace_badge_controlled);
                paceStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.finan_income));
            }
            if (paceExpenseProgress != null) {
                paceExpenseProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.finan_income)));
            }
        } else {
            if (paceStatusText != null) {
                paceStatusText.setText(R.string.advice_dialog_pace_status_overpace);
            }
            if (paceStatusBadge != null) {
                paceStatusBadge.setText(R.string.advice_dialog_pace_badge_overpace);
                paceStatusBadge.setTextColor(ContextCompat.getColor(context, R.color.finan_expense));
            }
            if (paceExpenseProgress != null) {
                paceExpenseProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.finan_expense)));
            }
        }

        // Section 3: Data Pendukung
        TextView incomeText = view.findViewById(R.id.advice_dialog_income);
        TextView expenseText = view.findViewById(R.id.advice_dialog_expense);
        TextView ratioText = view.findViewById(R.id.advice_dialog_ratio);
        ProgressBar ratioProgress = view.findViewById(R.id.advice_dialog_ratio_progress);

        if (incomeText != null && expenseText != null) {
            if (mode == DisplayMode.MASKED) {
                incomeText.setText(R.string.java_FinancialAdviceDialog_rp);
                expenseText.setText(R.string.java_FinancialAdviceDialog_rp);
            } else if (mode == DisplayMode.PERCENTAGE) {
                incomeText.setText(R.string.java_FinancialAdviceDialog_100);
                if (income > 0) {
                    float ratio = (expense * 100f) / income;
                    expenseText.setText(String.format(Locale.getDefault(), "%.0f%%", ratio));
                } else {
                    expenseText.setText("-");
                }
            } else {
                incomeText.setText(MoneyFormatter.format(income));
                expenseText.setText(MoneyFormatter.format(expense));
            }
        }

        if (ratioText != null) {
            if (income > 0) {
                float ratio = (expense * 100f) / income;
                ratioText.setText(String.format(Locale.US, "%.1f%%", ratio));
                if (ratioProgress != null) {
                    ratioProgress.setProgress(Math.min(100, Math.max(0, Math.round(ratio))));
                    int tintColor = ratio <= pace.timePercent ? R.color.finan_income : R.color.finan_expense;
                    ratioProgress.setProgressTintList(ColorStateList.valueOf(ContextCompat.getColor(context, tintColor)));
                }
            } else {
                ratioText.setText("-");
                if (ratioProgress != null) {
                    ratioProgress.setProgress(0);
                }
            }
        }

        BottomSheetHelper.show(dialog);
    }

    @VisibleForTesting
    public static String decodeHtml(@Nullable String text) {
        if (text == null) return "";
        String s = text;
        if (s.contains("&amp;lt;") || s.contains("&amp;gt;")) {
            s = s.replace("&amp;lt;", "<").replace("&amp;gt;", ">");
        }
        if (s.contains("&lt;") || s.contains("&gt;")) {
            s = s.replace("&lt;", "<").replace("&gt;", ">");
        }
        if (s.contains("&amp;amp;")) {
            s = s.replace("&amp;amp;", "&");
        }
        if (s.contains("&amp;quot;")) {
            s = s.replace("&amp;quot;", "\"");
        }
        if (s.contains("&quot;")) {
            s = s.replace("&quot;", "\"");
        }
        return s;
    }

    @VisibleForTesting
    public static CharSequence toSpanned(@Nullable String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        try {
            CharSequence spanned = Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY);
            return trimSpanned(spanned);
        } catch (Throwable t) {
            return trimSpanned(html.replaceAll("<[^>]+>", ""));
        }
    }

    private static CharSequence trimSpanned(CharSequence source) {
        if (source == null) return "";
        int start = 0;
        int end = source.length();
        while (start < end && Character.isWhitespace(source.charAt(start))) {
            start++;
        }
        while (end > start && Character.isWhitespace(source.charAt(end - 1))) {
            end--;
        }
        return source.subSequence(start, end);
    }

    @VisibleForTesting
    public static RawAdviceParts extractRawAdviceParts(@Nullable String rawMessage) {
        if (rawMessage == null || rawMessage.trim().isEmpty()) {
            return new RawAdviceParts("", null, null);
        }
        String cleanMessage = decodeHtml(rawMessage);

        Pattern tipsPattern = Pattern.compile("(?i)(?:<br\\s*/?>\\s*)*(?:<b>)?\\s*(Tips\\s+(?:Keuangan|Aplikasi)):?\\s*(?:</b>)?(?:<br\\s*/?>\\s*)*");
        Matcher tipsMatcher = tipsPattern.matcher(cleanMessage);

        if (tipsMatcher.find()) {
            String tipsTitle = tipsMatcher.group(1).trim();
            int tipsStart = tipsMatcher.start();
            int tipsEnd = tipsMatcher.end();

            Pattern nextSectionPattern = Pattern.compile("(?i)<b>\\s*(Sorotan\\s+Pengeluaran):?\\s*</b>");
            Matcher nextMatcher = nextSectionPattern.matcher(cleanMessage);

            String tipsContentHtml;
            String mainMessageHtml;

            if (nextMatcher.find(tipsEnd)) {
                int nextStart = nextMatcher.start();
                String rawTips = cleanMessage.substring(tipsEnd, nextStart);
                tipsContentHtml = rawTips.replaceAll("(?i)(?:<br\\s*/?>\\s*)+$", "").trim();

                String beforeTips = cleanMessage.substring(0, tipsStart).replaceAll("(?i)(?:<br\\s*/?>\\s*)+$", "").trim();
                String afterTips = cleanMessage.substring(nextStart).trim();
                mainMessageHtml = beforeTips.isEmpty() ? afterTips : (beforeTips + "<br/><br/>" + afterTips);
            } else {
                tipsContentHtml = cleanMessage.substring(tipsEnd).replaceAll("(?i)(?:<br\\s*/?>\\s*)+$", "").trim();
                mainMessageHtml = cleanMessage.substring(0, tipsStart).replaceAll("(?i)(?:<br\\s*/?>\\s*)+$", "").trim();
            }

            return new RawAdviceParts(mainMessageHtml, tipsTitle, tipsContentHtml.isEmpty() ? null : tipsContentHtml);
        }

        return new RawAdviceParts(cleanMessage, null, null);
    }

    @VisibleForTesting
    public static ParsedAdvice parseAdviceMessage(@Nullable String rawMessage) {
        RawAdviceParts raw = extractRawAdviceParts(rawMessage);
        CharSequence mainSpanned = toSpanned(raw.mainMessageHtml);
        CharSequence tipsSpanned = raw.tipsContentHtml != null ? toSpanned(raw.tipsContentHtml) : null;
        return new ParsedAdvice(mainSpanned, raw.tipsTitle, (tipsSpanned != null && tipsSpanned.length() > 0) ? tipsSpanned : null);
    }

    @VisibleForTesting
    public static PaceCalculation calculatePace(@Nullable MonthlySummary summary, @Nullable LocalDate referenceDate) {
        if (summary == null) {
            return new PaceCalculation(1, 30, 0f, 0f, false, true);
        }

        int year = summary.getYear();
        int month = summary.getMonth();

        int totalDays;
        int elapsedDays;

        if (year > 0 && month >= 1 && month <= 12) {
            YearMonth ym = YearMonth.of(year, month);
            totalDays = ym.lengthOfMonth();
            LocalDate now = referenceDate != null ? referenceDate : LocalDate.now();
            if (now.getYear() == year && now.getMonthValue() == month) {
                elapsedDays = Math.min(Math.max(now.getDayOfMonth(), 1), totalDays);
            } else if (now.isAfter(ym.atEndOfMonth())) {
                elapsedDays = totalDays;
            } else if (now.isBefore(ym.atDay(1))) {
                elapsedDays = 1;
            } else {
                elapsedDays = 1;
            }
        } else {
            totalDays = 30;
            elapsedDays = 15;
        }

        float timePercent = totalDays > 0 ? (elapsedDays * 100f) / totalDays : 0f;

        long income = summary.getMonthIncomeMinor();
        long expense = summary.getMonthExpenseMinor();
        boolean hasTransactions = (income > 0 || expense > 0);

        float expensePercent = 0f;
        if (income > 0) {
            expensePercent = (expense * 100f) / income;
        } else if (expense > 0) {
            expensePercent = 100f;
        }

        boolean isControlled = (expensePercent <= timePercent);
        return new PaceCalculation(elapsedDays, totalDays, timePercent, expensePercent, hasTransactions, isControlled);
    }
}
