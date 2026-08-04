package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class CustomEmojiKeyboardView extends LinearLayout {

  public interface OnEmojiSelectedListener {
    void onEmojiSelected(String emoji);
  }

  public interface OnCloseClickListener {
    void onCloseClick();
  }

  private OnEmojiSelectedListener onEmojiSelectedListener;
  private OnCloseClickListener onCloseClickListener;

  private HorizontalScrollView categoryScrollView;
  private LinearLayout categoryChipContainer;
  private GridView emojiGridView;
  private EmojiAdapter emojiAdapter;

  private final List<String> currentEmojis = new ArrayList<>();
  private String selectedEmoji = "";
  private int activeCategoryIndex = 0; // 0 = Semua

  public CustomEmojiKeyboardView(Context context) {
    this(context, null);
  }

  public CustomEmojiKeyboardView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public CustomEmojiKeyboardView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    setOrientation(VERTICAL);
    setBackgroundResource(R.drawable.bg_emoji_keyboard_sheet);
    setElevation(dp(16));
    int p = dp(12);
    setPadding(p, p, p, p);
    initViews(context);
  }

  private void initViews(Context context) {
    // 1. Header Bar: Title + Quick Actions (Random, Close)
    LinearLayout headerLayout = new LinearLayout(context);
    headerLayout.setOrientation(HORIZONTAL);
    headerLayout.setGravity(Gravity.CENTER_VERTICAL);

    TextView title = new TextView(context);
    title.setText("Pilih Ikon Emoji");
    title.setTextAppearance(R.style.Finan_Text_SectionLabel);
    title.setTextColor(ContextCompat.getColor(context, R.color.finan_primary));
    title.setTextSize(14);
    title.setTypeface(null, android.graphics.Typeface.BOLD);

    LayoutParams titleParams = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
    headerLayout.addView(title, titleParams);

    // Random Button
    TextView randomBtn = new TextView(context);
    randomBtn.setText("🔀 Acak");
    randomBtn.setTextSize(13);
    randomBtn.setTextColor(ContextCompat.getColor(context, R.color.finan_primary));
    randomBtn.setPadding(dp(8), dp(4), dp(8), dp(4));
    randomBtn.setBackgroundResource(R.drawable.bg_chip);
    randomBtn.setOnClickListener(v -> pickRandomEmoji());
    headerLayout.addView(randomBtn);

    // Close Button
    TextView closeBtn = new TextView(context);
    closeBtn.setText("✕ Tutup");
    closeBtn.setTextSize(13);
    closeBtn.setTextColor(ContextCompat.getColor(context, R.color.finan_text_secondary));
    closeBtn.setPadding(dp(12), dp(4), dp(4), dp(4));
    closeBtn.setOnClickListener(v -> {
      if (onCloseClickListener != null) {
        onCloseClickListener.onCloseClick();
      }
    });
    headerLayout.addView(closeBtn);

    addView(headerLayout, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    // 2. Category Filter Chips
    categoryScrollView = new HorizontalScrollView(context);
    categoryScrollView.setHorizontalScrollBarEnabled(false);
    categoryScrollView.setOverScrollMode(OVER_SCROLL_NEVER);

    categoryChipContainer = new LinearLayout(context);
    categoryChipContainer.setOrientation(HORIZONTAL);
    categoryChipContainer.setPadding(0, dp(8), 0, dp(8));

    categoryScrollView.addView(categoryChipContainer, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
    
    LayoutParams scrollParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    scrollParams.topMargin = dp(4);
    addView(categoryScrollView, scrollParams);

    // 3. Emoji GridView
    emojiGridView = new GridView(context);
    emojiGridView.setNumColumns(6);
    emojiGridView.setVerticalSpacing(dp(6));
    emojiGridView.setHorizontalSpacing(dp(6));
    emojiGridView.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
    emojiGridView.setSelector(android.R.color.transparent);
    emojiGridView.setOverScrollMode(OVER_SCROLL_NEVER);

    emojiAdapter = new EmojiAdapter(context);
    emojiGridView.setAdapter(emojiAdapter);

    emojiGridView.setOnItemClickListener((parent, view, position, id) -> {
      String emoji = currentEmojis.get(position);
      setSelectedEmoji(emoji);
      if (onEmojiSelectedListener != null) {
        onEmojiSelectedListener.onEmojiSelected(emoji);
      }
    });

    LayoutParams gridParams = new LayoutParams(LayoutParams.MATCH_PARENT, dp(180));
    gridParams.topMargin = dp(4);
    addView(emojiGridView, gridParams);

    setupCategoryChips(context);
    loadCategoryEmojis(0);
  }

  private void setupCategoryChips(Context context) {
    categoryChipContainer.removeAllViews();

    // Index 0: Semua
    addCategoryChip(context, 0, "Semua ✨");

    for (int i = 0; i < EmojiConstants.CATEGORIES.length; i++) {
      EmojiConstants.EmojiCategory cat = EmojiConstants.CATEGORIES[i];
      addCategoryChip(context, i + 1, cat.getIcon() + " " + cat.getName());
    }
  }

  private void addCategoryChip(Context context, int index, String label) {
    TextView chip = new TextView(context);
    chip.setText(label);
    chip.setTextSize(12);
    chip.setPadding(dp(10), dp(5), dp(10), dp(5));
    
    LayoutParams params = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    params.setMargins(0, 0, dp(6), 0);
    chip.setLayoutParams(params);

    updateChipStyle(chip, index == activeCategoryIndex);

    chip.setOnClickListener(v -> {
      activeCategoryIndex = index;
      for (int i = 0; i < categoryChipContainer.getChildCount(); i++) {
        View child = categoryChipContainer.getChildAt(i);
        if (child instanceof TextView) {
          updateChipStyle((TextView) child, i == activeCategoryIndex);
        }
      }
      loadCategoryEmojis(index);
    });

    categoryChipContainer.addView(chip);
  }

  private void updateChipStyle(TextView chip, boolean isSelected) {
    Context context = chip.getContext();
    if (isSelected) {
      chip.setBackgroundResource(R.drawable.bg_chip_selected);
      chip.setTextColor(ContextCompat.getColor(context, R.color.finan_chip_text_selected));
      chip.setTypeface(null, android.graphics.Typeface.BOLD);
    } else {
      chip.setBackgroundResource(R.drawable.bg_chip);
      chip.setTextColor(ContextCompat.getColor(context, R.color.finan_chip_text));
      chip.setTypeface(null, android.graphics.Typeface.NORMAL);
    }
  }

  private void loadCategoryEmojis(int categoryIndex) {
    currentEmojis.clear();
    if (categoryIndex == 0) {
      // Semua
      currentEmojis.addAll(Arrays.asList(EmojiConstants.CATEGORY_EMOJIS));
    } else {
      int catIdx = categoryIndex - 1;
      if (catIdx >= 0 && catIdx < EmojiConstants.CATEGORIES.length) {
        currentEmojis.addAll(Arrays.asList(EmojiConstants.CATEGORIES[catIdx].getEmojis()));
      }
    }
    emojiAdapter.notifyDataSetChanged();
  }

  private void pickRandomEmoji() {
    if (!currentEmojis.isEmpty()) {
      int randomIndex = new Random().nextInt(currentEmojis.size());
      String emoji = currentEmojis.get(randomIndex);
      setSelectedEmoji(emoji);
      if (onEmojiSelectedListener != null) {
        onEmojiSelectedListener.onEmojiSelected(emoji);
      }
    }
  }

  public void setSelectedEmoji(String emoji) {
    this.selectedEmoji = emoji;
    emojiAdapter.notifyDataSetChanged();
  }

  public String getSelectedEmoji() {
    return selectedEmoji;
  }

  public void setOnEmojiSelectedListener(OnEmojiSelectedListener listener) {
    this.onEmojiSelectedListener = listener;
  }

  public void setOnCloseClickListener(OnCloseClickListener listener) {
    this.onCloseClickListener = listener;
  }
  public void showWithAnimation() {
    if (getVisibility() == VISIBLE) return;
    setVisibility(VISIBLE);
    setTranslationY(dp(300));
    animate()
        .translationY(0)
        .setDuration(250)
        .setInterpolator(new android.view.animation.DecelerateInterpolator(1.5f))
        .setListener(null)
        .start();
  }

  public void hideWithAnimation() {
    if (getVisibility() != VISIBLE) return;
    animate()
        .translationY(getHeight() > 0 ? getHeight() : dp(300))
        .setDuration(200)
        .setInterpolator(new android.view.animation.AccelerateInterpolator(1.5f))
        .withEndAction(() -> setVisibility(GONE))
        .start();
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }

  private class EmojiAdapter extends BaseAdapter {
    private final Context context;

    EmojiAdapter(Context context) {
      this.context = context;
    }

    @Override
    public int getCount() {
      return currentEmojis.size();
    }

    @Override
    public Object getItem(int position) {
      return currentEmojis.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      TextView textView;
      if (convertView == null) {
        textView = new TextView(context);
        textView.setLayoutParams(new GridView.LayoutParams(dp(44), dp(44)));
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(22);
      } else {
        textView = (TextView) convertView;
      }

      String emoji = currentEmojis.get(position);
      textView.setText(emoji);

      if (emoji.equals(selectedEmoji)) {
        textView.setBackgroundResource(R.drawable.bg_emoji_key_selected);
      } else {
        textView.setBackgroundResource(R.drawable.bg_emoji_key);
      }

      return textView;
    }
  }
}
