package com.dwlhm.finan.ui.dashboard;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.ForwardCashFlowSummary;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.domain.model.TransactionType;
import com.dwlhm.finan.domain.model.UpcomingObligation;
import com.dwlhm.finan.ui.common.AppServices;
import com.dwlhm.finan.util.money.MoneyFormatter;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@SuppressLint("SetTextI18n")
public final class UpcomingDetailBottomSheet extends BottomSheetDialog {

  private final AppServices services;
  private final ForwardCashFlowSummary summary;
  private final DashboardViewModel.DisplayMode displayMode;
  private final Runnable onDataChangedCallback;

  public UpcomingDetailBottomSheet(
      @NonNull Context context,
      @NonNull AppServices services,
      @NonNull ForwardCashFlowSummary summary,
      @NonNull DashboardViewModel.DisplayMode displayMode,
      @Nullable Runnable onDataChangedCallback) {
    super(context, R.style.Finan_BottomSheetDialog);
    this.services = services;
    this.summary = summary;
    this.displayMode = displayMode;
    this.onDataChangedCallback = onDataChangedCallback;
    initView();
  }

  public static void show(
      @NonNull Context context,
      @NonNull AppServices services,
      @NonNull ForwardCashFlowSummary summary,
      @NonNull DashboardViewModel.DisplayMode displayMode,
      @Nullable Runnable onDataChangedCallback) {
    UpcomingDetailBottomSheet dialog =
        new UpcomingDetailBottomSheet(context, services, summary, displayMode, onDataChangedCallback);
    dialog.show();
  }

  private void initView() {
    View view = LayoutInflater.from(getContext()).inflate(R.layout.bottom_sheet_upcoming_detail, null);
    setContentView(view);

    ImageView btnClose = view.findViewById(R.id.btn_upcoming_detail_close);
    if (btnClose != null) {
      btnClose.setOnClickListener(v -> dismiss());
    }

    TextView tvHorizonInfo = view.findViewById(R.id.tv_upcoming_horizon_info);
    if (tvHorizonInfo != null) {
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.forLanguageTag("id-ID"));
      String dateStr = summary.getHorizonDate().format(formatter);
      tvHorizonInfo.setText("Proyeksi hingga " + dateStr + " (" + summary.getHorizonDays() + " hari lagi)");
    }

    TextView tvRemaining = view.findViewById(R.id.tv_sheet_remaining_after_plans);
    TextView tvCurrentBalance = view.findViewById(R.id.tv_sheet_current_balance);
    TextView tvOutflow = view.findViewById(R.id.tv_sheet_scheduled_outflow);
    TextView tvInflow = view.findViewById(R.id.tv_sheet_scheduled_inflow);
    TextView tvProjected = view.findViewById(R.id.tv_sheet_projected_end_balance);

    boolean masked = displayMode == DashboardViewModel.DisplayMode.MASKED;

    if (tvRemaining != null) {
      tvRemaining.setText(masked ? "••••••" : MoneyFormatter.format(summary.getRemainingAfterPlansMinor()));
      if (!masked && summary.getRemainingAfterPlansMinor() < 0) {
        tvRemaining.setTextColor(ContextCompat.getColor(getContext(), R.color.finan_expense));
      }
    }
    if (tvCurrentBalance != null) {
      tvCurrentBalance.setText(masked ? "••••••" : MoneyFormatter.format(summary.getCurrentActualBalanceMinor()));
    }
    if (tvOutflow != null) {
      tvOutflow.setText(masked ? "••••••" : ("-" + MoneyFormatter.format(summary.getScheduledOutflowMinor())));
    }
    if (tvInflow != null) {
      tvInflow.setText(masked ? "••••••" : ("+" + MoneyFormatter.format(summary.getScheduledInflowMinor())));
    }
    if (tvProjected != null) {
      tvProjected.setText(masked ? "••••••" : MoneyFormatter.format(summary.getProjectedEndBalanceMinor()));
    }

    RecyclerView rv = view.findViewById(R.id.rv_upcoming_obligations);
    View emptyLayout = view.findViewById(R.id.layout_upcoming_empty);

    List<UpcomingObligation> obligations = summary.getUpcomingObligations();
    if (obligations.isEmpty()) {
      if (emptyLayout != null) emptyLayout.setVisibility(View.VISIBLE);
      if (rv != null) rv.setVisibility(View.GONE);
    } else {
      if (emptyLayout != null) emptyLayout.setVisibility(View.GONE);
      if (rv != null) {
        rv.setVisibility(View.VISIBLE);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        rv.setAdapter(new UpcomingObligationsAdapter(obligations));
      }
    }
  }

  private void recordObligation(UpcomingObligation obligation) {
    long walletId = obligation.getWalletId() != null && obligation.getWalletId() > 0 ? obligation.getWalletId() : 0L;
    if (walletId == 0L) {
      Wallet defaultWallet = services.walletDao.findDefault();
      if (defaultWallet != null) {
        walletId = defaultWallet.getId();
      } else {
        List<Wallet> wallets = services.walletDao.findAll();
        if (wallets != null && !wallets.isEmpty()) {
          walletId = wallets.get(0).getId();
        }
      }
    }

    long categoryId = obligation.getCategoryId() != null && obligation.getCategoryId() > 0 ? obligation.getCategoryId() : 0L;
    if (categoryId == 0L) {
      String typeFilter = obligation.getType() != null ? obligation.getType().name() : "EXPENSE";
      List<Category> categories = services.categoryDao.findByTypeFilterOrderByUsage(typeFilter);
      if (categories != null && !categories.isEmpty()) {
        categoryId = categories.get(0).getId();
      } else {
        Category defaultCat = services.categoryDao.findDefault();
        if (defaultCat != null) {
          categoryId = defaultCat.getId();
        } else {
          List<Category> allCats = services.categoryDao.findAllOrdered();
          if (allCats != null && !allCats.isEmpty()) {
            categoryId = allCats.get(0).getId();
          }
        }
      }
    }

    Transaction tx =
        new Transaction(
            0L,
            obligation.getAmountMinor(),
            obligation.getType() != null ? obligation.getType() : TransactionType.EXPENSE,
            walletId,
            categoryId,
            System.currentTimeMillis(),
            obligation.getName());

    services.transactionService.save(tx);
    services.transactionTemplateDao.markRecorded(obligation.getTemplateId(), System.currentTimeMillis());

    Toast.makeText(getContext(), obligation.getName() + " berhasil dicatat", Toast.LENGTH_SHORT).show();

    Intent broadcastIntent = new Intent("com.dwlhm.finan.ACTION_DATA_CHANGED");
    broadcastIntent.setPackage(getContext().getPackageName());
    getContext().sendBroadcast(broadcastIntent);

    if (onDataChangedCallback != null) {
      onDataChangedCallback.run();
    }
    dismiss();
  }

  private void skipObligation(UpcomingObligation obligation) {
    services.transactionTemplateDao.markSkipped(obligation.getTemplateId(), System.currentTimeMillis());

    Intent broadcastIntent = new Intent("com.dwlhm.finan.ACTION_DATA_CHANGED");
    broadcastIntent.setPackage(getContext().getPackageName());
    getContext().sendBroadcast(broadcastIntent);

    if (onDataChangedCallback != null) {
      onDataChangedCallback.run();
    }
    dismiss();
  }

  private final class UpcomingObligationsAdapter extends RecyclerView.Adapter<UpcomingObligationsAdapter.ViewHolder> {

    private final List<UpcomingObligation> items;

    public UpcomingObligationsAdapter(List<UpcomingObligation> items) {
      this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_upcoming_obligation, parent, false);
      return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      UpcomingObligation item = items.get(position);

      holder.tvIcon.setText(item.getIcon());
      holder.tvName.setText(item.getName());

      // Due badge calculation
      int days = item.getDaysRemaining();
      if (days == 0) {
        holder.tvDueBadge.setText("Hari ini");
      } else if (days == 1) {
        holder.tvDueBadge.setText("Besok");
      } else if (days > 1) {
        holder.tvDueBadge.setText("Dalam " + days + " hari");
      } else {
        holder.tvDueBadge.setText("Terlewat " + Math.abs(days) + " hari");
      }

      if (item.getCategoryName() != null && !item.getCategoryName().isEmpty()) {
        holder.tvCategory.setVisibility(View.VISIBLE);
        holder.tvCategory.setText(item.getCategoryName());
      } else {
        holder.tvCategory.setVisibility(View.GONE);
      }

      if (item.getWalletName() != null && !item.getWalletName().isEmpty()) {
        holder.tvWallet.setVisibility(View.VISIBLE);
        holder.tvWallet.setText("• " + item.getWalletName());
      } else {
        holder.tvWallet.setVisibility(View.GONE);
      }

      boolean masked = displayMode == DashboardViewModel.DisplayMode.MASKED;
      if (masked) {
        holder.tvAmount.setText("••••••");
      } else {
        boolean isIncome = item.getType() == TransactionType.INCOME;
        String prefix = isIncome ? "+" : "-";
        holder.tvAmount.setText(prefix + MoneyFormatter.format(item.getAmountMinor()));
        holder.tvAmount.setTextColor(
            ContextCompat.getColor(getContext(), isIncome ? R.color.finan_income : R.color.finan_expense));
      }

      holder.btnRecord.setOnClickListener(v -> recordObligation(item));
      if (holder.btnSkip != null) {
        holder.btnSkip.setOnClickListener(v -> skipObligation(item));
      }
    }

    @Override
    public int getItemCount() {
      return items.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
      TextView tvIcon;
      TextView tvName;
      TextView tvDueBadge;
      TextView tvCategory;
      TextView tvWallet;
      TextView tvAmount;
      MaterialButton btnRecord;
      MaterialButton btnSkip;

      ViewHolder(View itemView) {
        super(itemView);
        tvIcon = itemView.findViewById(R.id.tv_obligation_icon);
        tvName = itemView.findViewById(R.id.tv_obligation_name);
        tvDueBadge = itemView.findViewById(R.id.tv_obligation_due_badge);
        tvCategory = itemView.findViewById(R.id.tv_obligation_category);
        tvWallet = itemView.findViewById(R.id.tv_obligation_wallet);
        tvAmount = itemView.findViewById(R.id.tv_obligation_amount);
        btnRecord = itemView.findViewById(R.id.btn_record_obligation);
        btnSkip = itemView.findViewById(R.id.btn_skip_obligation);
      }
    }
  }
}
