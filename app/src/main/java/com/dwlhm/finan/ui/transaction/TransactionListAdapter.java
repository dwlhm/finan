package com.dwlhm.finan.ui.transaction;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;
import com.dwlhm.finan.data.entity.Category;
import com.dwlhm.finan.data.entity.Merchant;
import com.dwlhm.finan.data.entity.Tag;
import com.dwlhm.finan.data.entity.Wallet;
import com.dwlhm.finan.domain.model.Transaction;
import com.dwlhm.finan.util.money.MoneyFormatter;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TransactionListAdapter extends BaseAdapter {

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
  private final LayoutInflater inflater;
  private Map<Long, Category> categoriesById = Collections.emptyMap();
  private Map<Long, Wallet> walletsById = Collections.emptyMap();
  private Map<Long, Tag> tagsById = Collections.emptyMap();
  private Map<Long, Merchant> merchantsById = Collections.emptyMap();
  private final List<Transaction> items = new ArrayList<>();
  private final SimpleDateFormat dateFormat =
      new SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("id-ID"));

  public TransactionListAdapter(Context context) {
    this.context = context;
    this.inflater = LayoutInflater.from(context);
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
    notifyDataSetChanged();
  }

  public void setTransactions(List<Transaction> transactions) {
    items.clear();
    if (transactions != null) {
      items.addAll(transactions);
    }
    notifyDataSetChanged();
  }

  @Override
  public int getCount() {
    return items.size();
  }

  @Override
  public Transaction getItem(int position) {
    return items.get(position);
  }

  @Override
  public long getItemId(int position) {
    return items.get(position).getId();
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent) {
    ViewHolder holder;
    if (convertView == null) {
      convertView = inflater.inflate(R.layout.item_transaction, parent, false);
      holder = new ViewHolder();
      holder.categoryIndicator = convertView.findViewById(R.id.item_transaction_category_indicator);
      holder.walletIndicator = convertView.findViewById(R.id.item_transaction_wallet_indicator);
      holder.category = convertView.findViewById(R.id.item_transaction_category);
      holder.amount = convertView.findViewById(R.id.item_transaction_amount);
      holder.meta = convertView.findViewById(R.id.item_transaction_meta);
      holder.note = convertView.findViewById(R.id.item_transaction_note);
      convertView.setTag(holder);
    } else {
      holder = (ViewHolder) convertView.getTag();
    }

    Transaction transaction = getItem(position);
    Category category = categoriesById.get(transaction.getCategoryId());
    Wallet wallet = walletsById.get(transaction.getWalletId());
    Merchant merchant =
        transaction.getMerchantId() == null
            ? null
            : merchantsById.get(transaction.getMerchantId());

    holder.category.setText(TransactionRowLabels.title(context, transaction, category));
    setIndicatorColor(
        holder.categoryIndicator,
        colorFor(
            CATEGORY_COLORS,
            transaction.getCategoryId(),
            category != null ? category.getName() : ""));
    setIndicatorColor(
        holder.walletIndicator,
        colorFor(
            WALLET_COLORS,
            transaction.getWalletId(),
            wallet != null ? wallet.getName() : ""));

    String formatted = MoneyFormatter.format(transaction.getAmountMinor());
    holder.amount.setText(
        context.getString(TransactionRowLabels.amountFormat(transaction.getType()), formatted));
    holder.amount.setTextColor(
        ContextCompat.getColor(context, TransactionRowLabels.amountColor(transaction.getType())));

    String walletName = wallet != null ? wallet.getName() : "";
    String when = dateFormat.format(new Date(transaction.getOccurredAt()));
    holder.meta.setText(TransactionRowLabels.formatMeta(merchant, walletName, when));

    String tagLine = TransactionRowLabels.formatTagLine(transaction, tagsById);
    String secondary = TransactionRowLabels.formatSecondaryLine(transaction, tagLine);
    boolean hasSecondary = !TextUtils.isEmpty(secondary);
    holder.note.setVisibility(hasSecondary ? View.VISIBLE : View.GONE);
    if (hasSecondary) {
      holder.note.setText(secondary);
    }

    return convertView;
  }

  private void setIndicatorColor(View indicator, int color) {
    GradientDrawable background = new GradientDrawable();
    background.setColor(color);
    background.setCornerRadius(
        2f * context.getResources().getDisplayMetrics().density);
    indicator.setBackground(background);
  }

  private int colorFor(int[] palette, long id, String name) {
    int hash = Long.hashCode(id);
    if (!TextUtils.isEmpty(name)) {
      hash = (31 * hash) + name.hashCode();
    }
    return palette[Math.floorMod(hash, palette.length)];
  }

  private static class ViewHolder {
    View categoryIndicator;
    View walletIndicator;
    TextView category;
    TextView amount;
    TextView meta;
    TextView note;
  }
}
