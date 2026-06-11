package com.dwlhm.finan.ui.transaction;

import android.text.TextUtils;

import com.dwlhm.finan.data.entity.Merchant;
import com.dwlhm.finan.data.entity.Tag;
import com.dwlhm.finan.domain.model.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TransactionRowLabels {

  private TransactionRowLabels() {}

  public static String formatMeta(
      Transaction transaction, Merchant merchant, String walletName, String when) {
    StringBuilder meta = new StringBuilder();
    if (merchant != null) {
      meta.append(merchant.getName());
    }
    if (!TextUtils.isEmpty(walletName)) {
      if (meta.length() > 0) {
        meta.append(" · ");
      }
      meta.append(walletName);
    }
    if (meta.length() > 0) {
      meta.append(" · ");
    }
    meta.append(when);
    return meta.toString();
  }

  public static String formatTagLine(Transaction transaction, Map<Long, Tag> tagsById) {
    List<String> names = new ArrayList<>();
    for (Long tagId : transaction.getTagIds()) {
      Tag tag = tagsById.get(tagId);
      if (tag != null) {
        names.add("#" + tag.getName());
      }
    }
    return TextUtils.join(" ", names);
  }

  public static String formatSecondaryLine(Transaction transaction, String tagLine) {
    String note = transaction.getNote();
    boolean hasNote = !TextUtils.isEmpty(note) && !TextUtils.isEmpty(note.trim());
    boolean hasTags = !TextUtils.isEmpty(tagLine);
    if (hasNote && hasTags) {
      return note.trim() + " · " + tagLine;
    }
    if (hasNote) {
      return note.trim();
    }
    if (hasTags) {
      return tagLine;
    }
    return "";
  }
}
