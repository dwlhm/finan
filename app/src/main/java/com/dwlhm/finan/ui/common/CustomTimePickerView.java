package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;

public final class CustomTimePickerView extends LinearLayout {

  public interface OnTimeSelectedListener {
    void onTimeSelected(int hour, int minute);
  }

  private int hour, minute;
  private OnTimeSelectedListener listener;
  private final TextView hourDisplay, minuteDisplay;

  private static class PickerColumn {
    final LinearLayout root;
    final TextView value;
    PickerColumn(LinearLayout root, TextView value) {
      this.root = root;
      this.value = value;
    }
  }

  public CustomTimePickerView(Context context) {
    this(context, null);
  }

  public CustomTimePickerView(Context context, AttributeSet attrs) {
    super(context, attrs);
    setOrientation(HORIZONTAL);
    setBackgroundResource(R.drawable.bg_control_surface);
    setPadding(dp(16), dp(12), dp(16), dp(12));

    java.time.LocalTime now = java.time.LocalTime.now();
    hour = now.getHour();
    minute = now.getMinute();

    int textSec = ContextCompat.getColor(context, R.color.finan_text_secondary);
    int textPrim = ContextCompat.getColor(context, R.color.finan_text_primary);
    int selBg = UiComponentStyles.selectableItemBackground(context);

    PickerColumn hourCol = buildPicker("Jam", hour, textSec, textPrim, selBg,
        () -> { hour = (hour + 23) % 24; }, () -> { hour = (hour + 1) % 24; });
    hourDisplay = hourCol.value;
    addView(hourCol.root);

    TextView sep = new TextView(context);
    sep.setText(":");
    sep.setTextSize(28f);
    sep.setTextColor(textPrim);
    sep.setTypeface(sep.getTypeface(), android.graphics.Typeface.BOLD);
    sep.setGravity(Gravity.CENTER);
    sep.setLayoutParams(new LayoutParams(0, -1, 0.3f));
    addView(sep);

    PickerColumn minCol = buildPicker("Menit", minute, textSec, textPrim, selBg,
        () -> { minute = (minute + 59) % 60; }, () -> { minute = (minute + 1) % 60; });
    minuteDisplay = minCol.value;
    addView(minCol.root);
  }

  public void setTime(int hour, int minute) {
    this.hour = hour;
    this.minute = minute;
    refreshDisplay();
  }

  public void setOnTimeSelectedListener(OnTimeSelectedListener l) {
    this.listener = l;
  }

  private void refreshDisplay() {
    hourDisplay.setText(String.format("%02d", hour));
    minuteDisplay.setText(String.format("%02d", minute));
    if (listener != null) listener.onTimeSelected(hour, minute);
  }

  private PickerColumn buildPicker(String label, int initial, int textSec, int textPrim, int selBg,
      Runnable dec, Runnable inc) {
    Context ctx = getContext();

    LinearLayout col = new LinearLayout(ctx);
    col.setOrientation(VERTICAL);
    col.setGravity(Gravity.CENTER);
    col.setLayoutParams(new LayoutParams(0, -1, 1f));

    TextView lbl = new TextView(ctx);
    lbl.setText(label);
    lbl.setTextSize(12f);
    lbl.setTextColor(textSec);
    lbl.setGravity(Gravity.CENTER);
    lbl.setLayoutParams(new LayoutParams(-1, dp(20)));
    col.addView(lbl);

    ImageButton upBtn = makeArrowBtn(R.drawable.ic_arrow_up, selBg);
    upBtn.setOnClickListener(v -> { inc.run(); refreshDisplay(); });
    col.addView(upBtn);

    TextView val = new TextView(ctx);
    val.setText(String.format("%02d", initial));
    val.setTextSize(28f);
    val.setTextColor(textPrim);
    val.setTypeface(val.getTypeface(), android.graphics.Typeface.BOLD);
    val.setGravity(Gravity.CENTER);
    val.setMinWidth(dp(64));
    val.setLayoutParams(new LayoutParams(-1, dp(52)));
    col.addView(val);

    ImageButton downBtn = makeArrowBtn(R.drawable.ic_arrow_down, selBg);
    downBtn.setOnClickListener(v -> { dec.run(); refreshDisplay(); });
    col.addView(downBtn);

    return new PickerColumn(col, val);
  }

  private ImageButton makeArrowBtn(int drawableRes, int selBg) {
    ImageButton btn = new ImageButton(getContext());
    btn.setImageResource(drawableRes);
    btn.setBackgroundResource(0);
    btn.setScaleType(android.widget.ImageView.ScaleType.CENTER);
    btn.setLayoutParams(new LayoutParams(dp(40), dp(40)));
    btn.setForeground(ContextCompat.getDrawable(getContext(), selBg));
    return btn;
  }

  private int dp(int v) {
    return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
  }
}
