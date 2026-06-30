package com.dwlhm.finan.ui.transaction;

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
import com.dwlhm.finan.data.entity.Merchant;
import com.dwlhm.finan.data.entity.Tag;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.ui.common.infinitescroll.InfiniteScrollRecyclerAdapter;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public final class TransactionRecyclerAdapter
    extends InfiniteScrollRecyclerAdapter<Transaction, TransactionItemViewHolder> {

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
  private boolean masked;
  private Map<Long, Category> categoriesById = Collections.emptyMap();
  private Map<Long, Wallet> walletsById = Collections.emptyMap();
  private Map<Long, Tag> tagsById = Collections.emptyMap();
  private Map<Long, Merchant> merchantsById = Collections.emptyMap();
  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"));
  private final SimpleDateFormat headerDateFormat =
      new SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID"));
  private OnTransactionClickListener clickListener;

  public interface OnTransactionClickListener {
    void onTransactionClick(Transaction transaction, int position);
  }

  public TransactionRecyclerAdapter(Context context) {
    super(LayoutInflater.from(context), R.layout.item_infinite_scroll_loading);
    this.context = context;
  }

  public void setEntityLookups(
      Map<Long, Category> categoriesById,
      Map<Long, Wallet> walletsById,
      Map<Long, Tag> tagsById,
      Map<Long, Merchant> merchantsById) {
    this.categoriesById = categoriesById != null ? categoriesById : Collections.emptyMap();
    this.walletsById = walletsById != null ? walletsById : Collections.emptyMap();
    this.tagsById = tagsById != null ? tagsById : Collections.emptyMap();
    this.merchantsById = merchantsById != null ? merchantsById : Collections.emptyMap();
    notifyItemRangeChanged(0, getContentItemCount());
  }

  public void setOnTransactionClickListener(OnTransactionClickListener clickListener) {
    this.clickListener = clickListener;
  }

  public void setMaskedMode(boolean masked) {
    this.masked = masked;
    notifyItemRangeChanged(0, getContentItemCount());
  }

  public Transaction getTransactionAt(int position) {
    return getItemAt(position);
  }

  @NonNull
  @Override
  protected TransactionItemViewHolder onCreateContentViewHolder(@NonNull ViewGroup parent) {
    return new TransactionItemViewHolder(
        LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false));
  }

  @Override
  protected void onBindContentViewHolder(
      @NonNull TransactionItemViewHolder holder, int position, @NonNull Transaction transaction) {
    Category category = categoriesById.get(transaction.getCategoryId());
    Wallet wallet = walletsById.get(transaction.getWalletId());
    Merchant merchant =
        transaction.getMerchantId() == null
            ? null
            : merchantsById.get(transaction.getMerchantId());

    holder.category.setText(TransactionRowLabels.title(context, transaction, category));
    if (category != null && category.getIcon() != null && !category.getIcon().trim().isEmpty()) {
      holder.icon.setImageDrawable(null);
      holder.icon.setBackground(null);
      holder.emoji.setVisibility(View.VISIBLE);
      holder.emoji.setText(category.getIcon().trim());
    } else {
      setIndicatorColor(
          holder.icon,
          colorFor(
              CATEGORY_COLORS,
              transaction.getCategoryId(),
              category != null ? category.getName() : ""));
      holder.icon.setImageResource(R.drawable.ic_summary_filter);
      holder.emoji.setVisibility(View.GONE);
    }

    if (masked) {
      holder.amount.setText("Rp ***");
      holder.amount.setTextColor(
          ContextCompat.getColor(context, R.color.finan_text_secondary));
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
    if (merchant != null) {
      subtitle.append(merchant.getName());
      if (!TextUtils.isEmpty(walletName)) subtitle.append(" · ");
    }
    subtitle.append(walletName);
    
    holder.wallet.setText(subtitle.toString());
    holder.wallet.setVisibility(TextUtils.isEmpty(subtitle.toString()) ? View.GONE : View.VISIBLE);
    holder.meta.setText(when);

    String tagLine = TransactionRowLabels.formatTagLine(transaction, tagsById);
    String secondary = TransactionRowLabels.formatSecondaryLine(transaction, tagLine);
    boolean hasSecondary = !TextUtils.isEmpty(secondary);
    holder.note.setVisibility(hasSecondary ? View.VISIBLE : View.GONE);
    if (hasSecondary) {
      holder.note.setText(secondary);
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
      holder.dateHeader.setText(currentHeader);
    } else {
      holder.dateHeader.setVisibility(View.GONE);
    }

    holder.itemView.setOnClickListener(
        v -> {
          if (clickListener != null) {
            clickListener.onTransactionClick(transaction, position);
          }
        });
  }

  private void setIndicatorColor(View indicator, int color) {
    GradientDrawable background = new GradientDrawable();
    background.setColor(color);
    background.setCornerRadius(
        22f * context.getResources().getDisplayMetrics().density);
    indicator.setBackground(background);
  }

  private int colorFor(int[] palette, long id, String name) {
    int hash = Long.hashCode(id);
    if (!TextUtils.isEmpty(name)) {
      hash = (31 * hash) + name.hashCode();
    }
    return palette[Math.floorMod(hash, palette.length)];
  }
}
