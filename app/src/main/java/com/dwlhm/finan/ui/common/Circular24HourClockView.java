package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import com.dwlhm.finan.R;

import java.util.Locale;

public class Circular24HourClockView extends View {

  private static final String[] MINUTE_LABELS = {
      "00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55"
  };

  private static final String[] MINUTE_LABELS_ALL = new String[60];

  static {
    for (int i = 0; i < 60; i++) {
      MINUTE_LABELS_ALL[i] = String.format(Locale.US, "%02d", i);
    }
  }

  public enum Mode {
    HOUR,
    MINUTE
  }

  public interface OnTimeChangedListener {
    void onTimeChanged(int hour, int minute);
  }

  public interface OnModeChangedListener {
    void onModeChanged(Mode mode);
  }

  private Mode mode = Mode.HOUR;
  private int hour = 12;
  private int minute = 0;

  private OnTimeChangedListener timeListener;
  private OnModeChangedListener modeListener;

  private final Paint bgPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint handPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint selectorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint pivotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private final Paint textInnerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textOuterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
  private final Paint textSelectedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

  private final float selectorRadius = dp(16);
  private final float pivotRadius = dp(4);

  public Circular24HourClockView(Context context) {
    this(context, null);
  }

  public Circular24HourClockView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public Circular24HourClockView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  private void init(Context context) {
    int colorPrimary = ContextCompat.getColor(context, R.color.finan_primary);
    int colorTextPrimary = ContextCompat.getColor(context, R.color.finan_text_primary);
    int colorTextSecondary = ContextCompat.getColor(context, R.color.finan_text_secondary);
    int colorDivider = ContextCompat.getColor(context, R.color.finan_divider);
    int colorControlBg = ContextCompat.getColor(context, R.color.finan_control_bg);
    int colorChipTextSelected = ContextCompat.getColor(context, R.color.finan_chip_text_selected);

    Typeface baseTypeface = ResourcesCompat.getFont(context, R.font.plus_jakarta_sans_family);
    if (baseTypeface == null) {
      baseTypeface = Typeface.DEFAULT;
    }
    Typeface boldTypeface = Typeface.create(baseTypeface, Typeface.BOLD);

    bgPaint.setStyle(Paint.Style.FILL);
    bgPaint.setColor(colorControlBg);

    borderPaint.setStyle(Paint.Style.STROKE);
    borderPaint.setColor(colorDivider);
    borderPaint.setStrokeWidth(dp(1));

    ringPaint.setStyle(Paint.Style.STROKE);
    ringPaint.setColor(colorDivider);
    ringPaint.setStrokeWidth(dp(1));

    handPaint.setStyle(Paint.Style.STROKE);
    handPaint.setColor(colorPrimary);
    handPaint.setStrokeWidth(dp(2));

    selectorPaint.setStyle(Paint.Style.FILL);
    selectorPaint.setColor(colorPrimary);

    pivotPaint.setStyle(Paint.Style.FILL);
    pivotPaint.setColor(colorPrimary);

    textInnerPaint.setStyle(Paint.Style.FILL);
    textInnerPaint.setColor(colorTextPrimary);
    textInnerPaint.setTypeface(baseTypeface);
    textInnerPaint.setTextAlign(Paint.Align.CENTER);
    textInnerPaint.setTextSize(sp(13));

    textOuterPaint.setStyle(Paint.Style.FILL);
    textOuterPaint.setColor(colorTextSecondary);
    textOuterPaint.setTypeface(baseTypeface);
    textOuterPaint.setTextAlign(Paint.Align.CENTER);
    textOuterPaint.setTextSize(sp(11));

    textSelectedPaint.setStyle(Paint.Style.FILL);
    textSelectedPaint.setColor(colorChipTextSelected);
    textSelectedPaint.setTypeface(boldTypeface);
    textSelectedPaint.setTextAlign(Paint.Align.CENTER);
    textSelectedPaint.setTextSize(sp(13));
    textSelectedPaint.setFakeBoldText(true);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int defaultSize = Math.round(dp(240));
    int width = resolveSize(defaultSize, widthMeasureSpec);
    int height = resolveSize(defaultSize, heightMeasureSpec);
    int size = Math.min(width, height);
    setMeasuredDimension(size, size);
  }

  @Override
  protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);

    float cx = getWidth() / 2f;
    float cy = getHeight() / 2f;
    float radius = Math.min(cx, cy) - selectorRadius;
    if (radius <= 0) {
      return;
    }

    // Lingkaran latar jam (finan_control_bg dengan stroke finan_divider)
    canvas.drawCircle(cx, cy, radius, bgPaint);
    canvas.drawCircle(cx, cy, radius, borderPaint);

    if (mode == Mode.HOUR) {
      float rOuter = radius * 0.82f;
      float rInner = radius * 0.54f;
      float rDivider = (rOuter + rInner) / 2f;

      // Lingkaran pemisah konsentris di antara keduanya ((rOuter + rInner) / 2)
      canvas.drawCircle(cx, cy, rDivider, ringPaint);

      // Tentukan target jarum dan lingkaran pemilih:
      // Jika hour == 0 || hour >= 13: target di layer luar pada rOuter, sudut ((hour == 0 ? 12 : hour - 12) * 30 - 90)
      // Jika 1 <= hour <= 12: target di layer dalam pada rInner, sudut (hour * 30 - 90)
      boolean isOuter = (hour == 0 || hour >= 13);
      float targetR = isOuter ? rOuter : rInner;
      int targetStep = isOuter ? (hour == 0 ? 12 : hour - 12) : hour;
      double targetAngleDeg = targetStep * 30.0 - 90.0;
      double targetAngleRad = Math.toRadians(targetAngleDeg);
      float targetX = (float) (cx + targetR * Math.cos(targetAngleRad));
      float targetY = (float) (cy + targetR * Math.sin(targetAngleRad));

      // Gambar garis jarum dari (cx, cy) ke target (finan_primary, stroke 2dp)
      canvas.drawLine(cx, cy, targetX, targetY, handPaint);

      // Gambar lingkaran pemilih di target (finan_primary, radius 16dp)
      canvas.drawCircle(targetX, targetY, selectorRadius, selectorPaint);

      // Gambar pivot tengah di (cx, cy) (finan_primary, radius 4dp)
      canvas.drawCircle(cx, cy, pivotRadius, pivotPaint);

      // Gambar 12 angka layer luar (i=1..12): i==12 ? "00" : (i+12). Sudut: i * 30 - 90 derajat.
      for (int i = 1; i <= 12; i++) {
        String label = (i == 12) ? "00" : String.valueOf(i + 12);
        double angleDeg = i * 30.0 - 90.0;
        double angleRad = Math.toRadians(angleDeg);
        float x = (float) (cx + rOuter * Math.cos(angleRad));
        float y = (float) (cy + rOuter * Math.sin(angleRad));

        if (isOuter && targetStep == i) {
          drawCenteredText(canvas, label, x, y, textSelectedPaint);
        } else {
          drawCenteredText(canvas, label, x, y, textOuterPaint);
        }
      }

      // Gambar 12 angka layer dalam (i=1..12): String.valueOf(i). Sudut: i * 30 - 90 derajat.
      for (int i = 1; i <= 12; i++) {
        String label = String.valueOf(i);
        double angleDeg = i * 30.0 - 90.0;
        double angleRad = Math.toRadians(angleDeg);
        float x = (float) (cx + rInner * Math.cos(angleRad));
        float y = (float) (cy + rInner * Math.sin(angleRad));

        if (!isOuter && targetStep == i) {
          drawCenteredText(canvas, label, x, y, textSelectedPaint);
        } else {
          drawCenteredText(canvas, label, x, y, textInnerPaint);
        }
      }
    } else {
      // Mode.MINUTE
      float rMinute = radius * 0.80f;
      double minuteAngleDeg = minute * 6.0 - 90.0;
      double minuteAngleRad = Math.toRadians(minuteAngleDeg);
      float targetX = (float) (cx + rMinute * Math.cos(minuteAngleRad));
      float targetY = (float) (cy + rMinute * Math.sin(minuteAngleRad));

      // Gambar garis jarum dari (cx, cy) ke target
      canvas.drawLine(cx, cy, targetX, targetY, handPaint);

      // Gambar lingkaran pemilih di target (radius 16dp) berwarna finan_primary
      canvas.drawCircle(targetX, targetY, selectorRadius, selectorPaint);

      // Gambar pivot tengah
      canvas.drawCircle(cx, cy, pivotRadius, pivotPaint);

      // Gambar angka menit kelipatan 5: "00", "05", "10", "15", "20", "25", "30", "35", "40", "45", "50", "55"
      boolean isMultipleOfFive = (minute % 5 == 0);
      for (int i = 1; i <= 12; i++) {
        int mVal = (i * 5) % 60;
        String label = MINUTE_LABELS[i % 12];
        double angleDeg = i * 30.0 - 90.0;
        double angleRad = Math.toRadians(angleDeg);
        float x = (float) (cx + rMinute * Math.cos(angleRad));
        float y = (float) (cy + rMinute * Math.sin(angleRad));

        if (isMultipleOfFive && minute == mVal) {
          drawCenteredText(canvas, label, x, y, textSelectedPaint);
        } else {
          drawCenteredText(canvas, label, x, y, textInnerPaint);
        }
      }

      // Angka menit terpilih format %02d warna putih #FFFFFF di dalam lingkaran pemilih
      if (!isMultipleOfFive) {
        String selectedMinuteLabel = MINUTE_LABELS_ALL[minute];
        drawCenteredText(canvas, selectedMinuteLabel, targetX, targetY, textSelectedPaint);
      }
    }
  }

  private void drawCenteredText(Canvas canvas, String text, float x, float y, Paint paint) {
    Paint.FontMetrics fm = paint.getFontMetrics();
    float textY = y - (fm.ascent + fm.descent) / 2f;
    canvas.drawText(text, x, textY, paint);
  }

  @Override
  public boolean performClick() {
    return super.performClick();
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();
    if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
      if (getParent() != null) {
        getParent().requestDisallowInterceptTouchEvent(true);
      }

      float cx = getWidth() / 2f;
      float cy = getHeight() / 2f;
      float radius = Math.min(cx, cy) - dp(16);
      if (radius <= 0) {
        return true;
      }

      float rOuter = radius * 0.82f;
      float rInner = radius * 0.54f;
      float rThreshold = (rOuter + rInner) / 2f;

      float dx = event.getX() - cx;
      float dy = event.getY() - cy;
      double dist = Math.hypot(dx, dy);

      double degFrom3OClock = Math.toDegrees(Math.atan2(dy, dx));
      double deg = degFrom3OClock + 90.0;
      while (deg < 0) {
        deg += 360.0;
      }
      while (deg >= 360.0) {
        deg -= 360.0;
      }

      if (mode == Mode.HOUR) {
        int step = (int) (Math.round(deg / 30.0) % 12);
        if (dist > rThreshold) {
          hour = (step == 0 || step == 12) ? 0 : step + 12;
        } else {
          hour = (step == 0 || step == 12) ? 12 : step;
        }
        if (timeListener != null) {
          timeListener.onTimeChanged(hour, minute);
        }
        invalidate();

        if (action == MotionEvent.ACTION_UP) {
          performClick();
          setMode(Mode.MINUTE);
          if (modeListener != null) {
            modeListener.onModeChanged(Mode.MINUTE);
          }
        }
      } else {
        int m = (int) (Math.round(deg / 6.0) % 60);
        minute = m;
        if (timeListener != null) {
          timeListener.onTimeChanged(hour, minute);
        }
        invalidate();

        if (action == MotionEvent.ACTION_UP) {
          performClick();
        }
      }
      return true;
    }
    return super.onTouchEvent(event);
  }

  public void setTime(int hour, int minute) {
    this.hour = ((hour % 24) + 24) % 24;
    this.minute = ((minute % 60) + 60) % 60;
    invalidate();
  }

  public void setMode(Mode mode) {
    if (this.mode != mode) {
      this.mode = mode;
      invalidate();
    }
  }

  public Mode getMode() {
    return mode;
  }

  public int getHour() {
    return hour;
  }

  public int getMinute() {
    return minute;
  }

  public void addMinutes(int deltaMinutes) {
    int total = ((this.hour * 60 + this.minute + deltaMinutes) % 1440 + 1440) % 1440;
    this.hour = total / 60;
    this.minute = total % 60;
    invalidate();
  }

  public void setOnTimeChangedListener(@Nullable OnTimeChangedListener listener) {
    this.timeListener = listener;
  }

  public void setOnModeChangedListener(@Nullable OnModeChangedListener listener) {
    this.modeListener = listener;
  }

  private float dp(float dp) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
  }

  private float sp(float sp) {
    return TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
  }
}
