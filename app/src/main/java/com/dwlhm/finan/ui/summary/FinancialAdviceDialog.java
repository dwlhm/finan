package com.dwlhm.finan.ui.summary;

import android.app.Dialog;
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

    public static void show(Context context, FinancialAdvisor.Advice advice, MonthlySummary summary, com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode mode) {
        if (advice == null || summary == null) return;
        
        Dialog dialog = new Dialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_financial_advice, null);
        dialog.setContentView(view);
        
        TextView titleText = view.findViewById(R.id.advice_dialog_title);
        TextView messageText = view.findViewById(R.id.advice_dialog_message);
        TextView incomeText = view.findViewById(R.id.advice_dialog_income);
        TextView expenseText = view.findViewById(R.id.advice_dialog_expense);
        TextView ratioText = view.findViewById(R.id.advice_dialog_ratio);
        Button closeBtn = view.findViewById(R.id.advice_dialog_close);
        
        titleText.setText(advice.title);
        
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            messageText.setText(android.text.Html.fromHtml(advice.message, android.text.Html.FROM_HTML_MODE_LEGACY));
        } else {
            messageText.setText(android.text.Html.fromHtml(advice.message));
        }
        
        long income = summary.getMonthIncomeMinor();
        long expense = summary.getMonthExpenseMinor();
        
        if (mode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.MASKED) {
            incomeText.setText("Rp ***");
            expenseText.setText("Rp ***");
        } else if (mode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.PERCENTAGE) {
            incomeText.setText("100%");
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
