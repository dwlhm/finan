package com.dwlhm.finan.ui.transaction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollRecyclerAdapter;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class TransactionRecyclerAdapter
    extends InfiniteScrollRecyclerAdapter<Transaction, TransactionItemViewHolder>
    implements StickyHeaderItemDecoration.StickyHeaderInterface {

  private static final int[] CATEGORY_COLORS = {
    Color.rgb(84, 105, 212),
    Color.rgb(205, 93, 127),
    Color.rgb(76, 144, 111),
    Color.rgb(205, 126, 70),
    Color.rgb(117, 95, 173),
    Color.rgb(56, 137, 162)
  };
  private static final int[] WALLET_COLORS = {
    Color.rgb(45, 106, 106),
    Color.rgb(158, 126, 58),
    Color.rgb(91, 119, 153),
    Color.rgb(143, 100, 85),
    Color.rgb(87, 131, 91),
    Color.rgb(129, 101, 157)
  };

  private final Context context;
  private com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode displayMode = com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.NOMINAL;
  private long totalIncomeMinor = 0L;
  private Map<Long, Category> categoriesById = Collections.emptyMap();
  private Map<Long, Wallet> walletsById = Collections.emptyMap();
  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"));
  private final SimpleDateFormat headerDateFormat =
      new SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID"));
  private OnTransactionClickListener clickListener;
  private final Map<String, Long> dailyTotals = new HashMap<>();

  public interface OnTransactionClickListener {
    void onTransactionClick(Transaction transaction, int position);
  }

  public TransactionRecyclerAdapter(Context context) {
    super(LayoutInflater.from(context), R.layout.item_infinite_scroll_loading);
    this.context = context;
  }

  public void setEntityLookups(
      Map<Long, Category> categoriesById,
      Map<Long, Wallet> walletsById) {
    Map<Long, Category> newCategories = categoriesById != null ? categoriesById : Collections.emptyMap();
    Map<Long, Wallet> newWallets = walletsById != null ? walletsById : Collections.emptyMap();
    if (this.categoriesById.equals(newCategories) && this.walletsById.equals(newWallets)) {
      return;
    }
    this.categoriesById = newCategories;
    this.walletsById = newWallets;
    notifyItemRangeChanged(0, getContentItemCount());
  }

  public void setOnTransactionClickListener(OnTransactionClickListener clickListener) {
    this.clickListener = clickListener;
  }

  public OnTransactionClickListener getClickListener() {
    return clickListener;
  }

  public void setDisplayMode(com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode mode, long totalIncome) {
    if (this.displayMode == mode && this.totalIncomeMinor == totalIncome) return;
    this.displayMode = mode;
    this.totalIncomeMinor = totalIncome;
    notifyItemRangeChanged(0, getContentItemCount());
  }

  @Override
  public void replaceItems(List<Transaction> items) {
    recomputeDailyTotals(items);
    super.replaceItems(items);
  }

  @Override
  public void appendItems(List<Transaction> items) {
    if (items != null) {
      for (Transaction t : items) {
        String header = headerDateFormat.format(new Date(t.getOccurredAt()));
        dailyTotals.put(header, getOrDefault(dailyTotals, header, 0L) + t.getAmountMinor());
      }
    }
    super.appendItems(items);
  }

  private void recomputeDailyTotals(List<Transaction> items) {
    dailyTotals.clear();
    if (items != null) {
      for (Transaction t : items) {
        String header = headerDateFormat.format(new Date(t.getOccurredAt()));
        dailyTotals.put(header, getOrDefault(dailyTotals, header, 0L) + t.getAmountMinor());
      }
    }
  }

  private Long getOrDefault(Map<String, Long> map, String key, Long defaultValue) {
    Long val = map.get(key);
    return val != null ? val : defaultValue;
  }

  public Transaction getTransactionAt(int position) {
    return getItemAt(position);
  }

  @NonNull
  @Override
  protected TransactionItemViewHolder onCreateContentViewHolder(@NonNull ViewGroup parent) {
    return new TransactionItemViewHolder(
        LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false), this);
  }

  @Override
  protected void onBindContentViewHolder(
      @NonNull TransactionItemViewHolder holder, int position, @NonNull Transaction transaction) {
    Category category = categoriesById.get(transaction.getCategoryId());
    Wallet wallet = walletsById.get(transaction.getWalletId());

    holder.category.setText(TransactionRowLabels.title(context, transaction, category));
    if (category != null && category.getIcon() != null && !category.getIcon().trim().isEmpty()) {
      holder.icon.setImageDrawable(null);
      holder.icon.setBackground(null);
      holder.emoji.setVisibility(View.VISIBLE);
      holder.emoji.setText(category.getIcon().trim());
    } else {
      GradientDrawable bg = (GradientDrawable) holder.iconBackground.mutate();
      bg.setColor(
          colorFor(
              CATEGORY_COLORS,
              transaction.getCategoryId(),
              category != null ? category.getName() : ""));
      holder.icon.setBackground(bg);
      holder.icon.setImageResource(R.drawable.ic_summary_filter);
      holder.emoji.setVisibility(View.GONE);
    }

    if (displayMode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.MASKED) {
      holder.amount.setText(R.string.java_TransactionRecyclerAdapter_rp);
      holder.amount.setTextColor(
          ContextCompat.getColor(context, R.color.finan_text_secondary));
    } else if (displayMode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.PERCENTAGE) {
      if (totalIncomeMinor > 0) {
          float pct = (transaction.getAmountMinor() * 100f) / totalIncomeMinor;
          holder.amount.setText(String.format(Locale.getDefault(), "%.1f%%", pct));
      } else {
          holder.amount.setText("-");
      }
      holder.amount.setTextColor(
          ContextCompat.getColor(context, TransactionRowLabels.amountColor(transaction.getType())));
    } else {
      String formatted = MoneyFormatter.format(transaction.getAmountMinor());
      holder.amount.setText(
          context.getString(TransactionRowLabels.amountFormat(transaction.getType()), formatted));
      holder.amount.setTextColor(
          ContextCompat.getColor(context, TransactionRowLabels.amountColor(transaction.getType())));
    }

    String walletName = wallet != null ? wallet.getName() : "";
    String when = dateFormat.format(new Date(transaction.getOccurredAt()));
    
    StringBuilder subtitle = new StringBuilder();
    subtitle.append(walletName);
    
    holder.wallet.setText(subtitle.toString());
    holder.wallet.setVisibility(TextUtils.isEmpty(subtitle.toString()) ? View.GONE : View.VISIBLE);
    holder.meta.setText(when);

    String note = transaction.getNote();
    boolean hasNote = !TextUtils.isEmpty(note) && !TextUtils.isEmpty(note.trim());
    holder.note.setVisibility(hasNote ? View.VISIBLE : View.GONE);
    if (hasNote) {
      holder.note.setText(note.trim());
    }
    
    boolean showHeader = false;
    String currentHeader = headerDateFormat.format(new Date(transaction.getOccurredAt()));
    if (position == 0) {
      showHeader = true;
    } else {
      Transaction prevTransaction = getItemAt(position - 1);
      String prevHeader = headerDateFormat.format(new Date(prevTransaction.getOccurredAt()));
      showHeader = !currentHeader.equals(prevHeader);
    }
    
    if (showHeader) {
      holder.dateHeader.setVisibility(View.VISIBLE);
      holder.dateLabel.setText(currentHeader);

      Long total = dailyTotals.get(currentHeader);
      long dailyTotal = total != null ? total : 0L;

      if (displayMode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.MASKED) {
        holder.dailyTotal.setText(R.string.java_TransactionRecyclerAdapter_rp);
        holder.dailyTotal.setTextColor(
            ContextCompat.getColor(context, R.color.finan_text_secondary));
      } else if (displayMode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.PERCENTAGE) {
        if (totalIncomeMinor > 0) {
            float pct = (Math.abs(dailyTotal) * 100f) / totalIncomeMinor;
            String sign = dailyTotal > 0 ? "+" : (dailyTotal < 0 ? "-" : "");
            holder.dailyTotal.setText(sign + String.format(Locale.getDefault(), "%.1f%%", pct));
        } else {
            holder.dailyTotal.setText("-");
        }
        if (dailyTotal >= 0) {
            holder.dailyTotal.setTextColor(ContextCompat.getColor(context, R.color.finan_income));
        } else {
            holder.dailyTotal.setTextColor(ContextCompat.getColor(context, R.color.finan_expense));
        }
      } else {
        String formatted = MoneyFormatter.format(dailyTotal);
        if (dailyTotal >= 0) {
          holder.dailyTotal.setText("+" + formatted);
          holder.dailyTotal.setTextColor(
              ContextCompat.getColor(context, R.color.finan_income));
        } else {
          holder.dailyTotal.setText(formatted);
          holder.dailyTotal.setTextColor(
              ContextCompat.getColor(context, R.color.finan_expense));
        }
      }
    } else {
      holder.dateHeader.setVisibility(View.GONE);
    }
  }

  private int colorFor(int[] palette, long id, String name) {
    int hash = Long.hashCode(id);
    if (!TextUtils.isEmpty(name)) {
      hash = (31 * hash) + name.hashCode();
    }
    return palette[Math.floorMod(hash, palette.length)];
  }

  @Override
  public int getHeaderPositionForItem(int itemPosition) {
    int headerPos = itemPosition;
    while (headerPos >= 0) {
      if (isHeader(headerPos)) {
        return headerPos;
      }
      headerPos--;
    }
    return 0;
  }

  @Override
  public int getHeaderLayout(int headerPosition) {
    return R.layout.item_transaction_header_only;
  }

  @Override
  public void bindHeaderData(View header, int headerPosition) {
    if (headerPosition < 0 || headerPosition >= getContentItemCount()) return;
    Transaction transaction = getItemAt(headerPosition);
    if (transaction == null) return;
    String currentHeader = headerDateFormat.format(new Date(transaction.getOccurredAt()));
    
    android.widget.TextView dateLabel = header.findViewById(R.id.item_transaction_date_label);
    android.widget.TextView dailyTotalTv = header.findViewById(R.id.item_transaction_daily_total);
    
    if (dateLabel != null) dateLabel.setText(currentHeader);
    
    if (dailyTotalTv != null) {
      Long total = dailyTotals.get(currentHeader);
      long dailyTotal = total != null ? total : 0L;
      if (displayMode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.MASKED) {
        dailyTotalTv.setText(R.string.java_TransactionRecyclerAdapter_rp);
        dailyTotalTv.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
      } else if (displayMode == com.dwlhm.finan.ui.dashboard.DashboardViewModel.DisplayMode.PERCENTAGE) {
        if (totalIncomeMinor > 0) {
            float pct = (Math.abs(dailyTotal) * 100f) / totalIncomeMinor;
            String sign = dailyTotal > 0 ? "+" : (dailyTotal < 0 ? "-" : "");
            dailyTotalTv.setText(sign + String.format(Locale.getDefault(), "%.1f%%", pct));
        } else {
            dailyTotalTv.setText("-");
        }
        if (dailyTotal >= 0) {
            dailyTotalTv.setTextColor(ContextCompat.getColor(context, R.color.finan_income));
        } else {
            dailyTotalTv.setTextColor(ContextCompat.getColor(context, R.color.finan_expense));
        }
      } else {
        String formatted = MoneyFormatter.format(dailyTotal);
        if (dailyTotal >= 0) {
          dailyTotalTv.setText("+" + formatted);
          dailyTotalTv.setTextColor(ContextCompat.getColor(context, R.color.finan_income));
        } else {
          dailyTotalTv.setText(formatted);
          dailyTotalTv.setTextColor(ContextCompat.getColor(context, R.color.finan_expense));
        }
      }
    }
  }

  @Override
  public boolean isHeader(int itemPosition) {
    if (itemPosition == 0) return true;
    if (itemPosition < 0 || itemPosition >= getContentItemCount()) return false;
    Transaction current = getItemAt(itemPosition);
    Transaction prev = getItemAt(itemPosition - 1);
    if (current == null || prev == null) return false;
    String currentHeader = headerDateFormat.format(new Date(current.getOccurredAt()));
    String prevHeader = headerDateFormat.format(new Date(prev.getOccurredAt()));
    return !currentHeader.equals(prevHeader);
  }
}
