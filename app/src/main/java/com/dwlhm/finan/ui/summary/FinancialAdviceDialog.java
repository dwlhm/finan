package com.dwlhm.finan.ui.summary;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.ui.common.BottomSheetHelper;
import com.dwlhm.finan.util.money.MoneyFormatter;

public class FinancialAdviceDialog {

    @android.annotation.SuppressLint("InflateParams")
    public static void show(Context context, FinancialAdvisor.Advice advice, MonthlySummary summary, com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode mode) {
        if (advice == null || summary == null) return;
        
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.Finan_BottomSheetDialog);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_financial_advice, null);
        dialog.setContentView(view);
        
        TextView titleText = view.findViewById(R.id.advice_dialog_title);
        TextView messageText = view.findViewById(R.id.advice_dialog_message);
        TextView incomeText = view.findViewById(R.id.advice_dialog_income);
        TextView expenseText = view.findViewById(R.id.advice_dialog_expense);
        TextView ratioText = view.findViewById(R.id.advice_dialog_ratio);
        Button closeBtn = view.findViewById(R.id.advice_dialog_close);
        
        titleText.setText(advice.title);
        
        messageText.setText(android.text.Html.fromHtml(advice.message, android.text.Html.FROM_HTML_MODE_LEGACY));
        
        long income = summary.getMonthIncomeMinor();
        long expense = summary.getMonthExpenseMinor();
        
        if (mode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.MASKED) {
            incomeText.setText(R.string.java_FinancialAdviceDialog_rp);
            expenseText.setText(R.string.java_FinancialAdviceDialog_rp);
        } else if (mode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.PERCENTAGE) {
            incomeText.setText(R.string.java_FinancialAdviceDialog_100);
            if (income > 0) {
                float ratio = (expense * 100f) / income;
                expenseText.setText(String.format(java.util.Locale.getDefault(), "%.0f%%", ratio));
            } else {
                expenseText.setText("-");
            }
        } else {
            incomeText.setText(MoneyFormatter.format(income));
            expenseText.setText(MoneyFormatter.format(expense));
        }
        
        if (income > 0) {
            float ratio = (expense * 100f) / income;
            ratioText.setText(String.format(java.util.Locale.US, "%.1f%%", ratio));
        } else {
            ratioText.setText("-");
        }
        
        closeBtn.setOnClickListener(v -> dialog.dismiss());
        
        BottomSheetHelper.show(dialog);
    }
}
