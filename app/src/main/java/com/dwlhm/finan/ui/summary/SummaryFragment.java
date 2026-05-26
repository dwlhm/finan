package com.dwlhm.finan.ui.summary;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.CategoryTotal;
import com.dwlhm.finan.domain.model.MonthlySummary;
import com.dwlhm.finan.domain.model.WalletBalance;
import com.dwlhm.finan.service.summary.SummaryService;
import com.dwlhm.finan.ui.common.ScreenFragment;
import com.dwlhm.finan.ui.common.ServicesProvider;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SummaryFragment extends ScreenFragment {

  private final ExecutorService executor = Executors.newSingleThreadExecutor();
  private final Handler mainHandler = new Handler(Looper.getMainLooper());

  private ProgressBar loading;
  private TextView monthExpense;
  private TextView monthIncome;
  private TextView todayExpense;
  private TextView todayIncome;
  private LinearLayout categoryList;
  private LinearLayout walletList;
  private TextView emptyMessage;

  @Override
  protected int getLayoutResId() {
    return R.layout.activity_summary;
  }

  @Override
  protected void onViewReady(@NonNull View view, @Nullable Bundle savedInstanceState) {
    loading = view.findViewById(R.id.summary_loading);
    monthExpense = view.findViewById(R.id.summary_month_expense);
    monthIncome = view.findViewById(R.id.summary_month_income);
    todayExpense = view.findViewById(R.id.summary_today_expense);
    todayIncome = view.findViewById(R.id.summary_today_income);
    categoryList = view.findViewById(R.id.summary_category_list);
    walletList = view.findViewById(R.id.summary_wallet_list);
    emptyMessage = view.findViewById(R.id.summary_empty);
  }

  @Override
  public void onResume() {
    super.onResume();
    loadSummaryAsync();
  }

  @Override
  public void onDestroy() {
    mainHandler.removeCallbacksAndMessages(null);
    executor.shutdownNow();
    super.onDestroy();
  }

  private void loadSummaryAsync() {
    loading.setVisibility(View.VISIBLE);
    emptyMessage.setVisibility(View.GONE);
    SummaryService summaryService = ServicesProvider.get(requireContext()).summaryService;
    executor.execute(
        () -> {
          MonthlySummary summary = summaryService.loadCurrentMonth();
          mainHandler.post(
              () -> {
                if (!isAdded() || getView() == null) {
                  return;
                }
                bindSummary(summary);
                loading.setVisibility(View.GONE);
              });
        });
  }

  private void bindSummary(MonthlySummary summary) {
    String monthLabel =
        String.format(
            Locale.forLanguageTag("id-ID"),
            "%d/%02d",
            summary.getYear(),
            summary.getMonth());
    monthExpense.setText(
        getString(
            R.string.summary_month_expense_format,
            monthLabel,
            format(summary.getMonthExpenseMinor())));
    monthIncome.setText(
        getString(R.string.summary_month_income_format, format(summary.getMonthIncomeMinor())));
    todayExpense.setText(
        getString(R.string.summary_today_expense_format, format(summary.getTodayExpenseMinor())));
    todayIncome.setText(
        getString(R.string.summary_today_income_format, format(summary.getTodayIncomeMinor())));

    categoryList.removeAllViews();
    if (summary.getTopExpenseCategories().isEmpty()) {
      emptyMessage.setVisibility(View.VISIBLE);
      emptyMessage.setText(R.string.summary_no_expense);
    } else {
      emptyMessage.setVisibility(View.GONE);
      for (CategoryTotal row : summary.getTopExpenseCategories()) {
        TextView line = new TextView(requireContext());
        line.setText(
            getString(
                R.string.summary_category_line_format,
                row.getCategoryName(),
                format(row.getTotalMinor())));
        line.setTextColor(requireContext().getColor(R.color.finan_text_primary));
        line.setTextSize(15f);
        line.setPadding(0, 0, 0, 12);
        categoryList.addView(line);
      }
    }

    walletList.removeAllViews();
    for (WalletBalance wallet : summary.getWalletBalances()) {
      TextView line = new TextView(requireContext());
      line.setText(
          getString(
              R.string.summary_wallet_line_format,
              wallet.getWalletName(),
              format(wallet.getBalanceMinor())));
      line.setTextColor(requireContext().getColor(R.color.finan_text_secondary));
      line.setTextSize(14f);
      line.setPadding(0, 0, 0, 8);
      walletList.addView(line);
    }
  }

  private String format(long amountMinor) {
    return MoneyFormatter.format(amountMinor);
  }
}
