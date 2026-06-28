package com.dwlhm.finan.ui.common;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Typeface;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.dwlhm.finan.R;

public final class LabeledEditTextView extends LinearLayout {

  private final EditText editText;

  public LabeledEditTextView(Context context) {
    this(context, null);
  }

  public LabeledEditTextView(Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    setOrientation(VERTICAL);

    CharSequence labelText = null;
    CharSequence fieldHint = null;
    int inputType = InputType.TYPE_CLASS_TEXT;
    if (attrs != null) {
      try (TypedArray typedArray =
          context.obtainStyledAttributes(attrs, R.styleable.LabeledEditTextView)) {
        labelText = typedArray.getText(R.styleable.LabeledEditTextView_labelText);
        fieldHint = typedArray.getText(R.styleable.LabeledEditTextView_fieldHint);
        inputType =
            typedArray.getInt(R.styleable.LabeledEditTextView_android_inputType, inputType);
      }
    }

    TextView labelView = new TextView(context);
    labelView.setTextAppearance(R.style.Finan_Text_SectionLabel);
    labelView.setText(labelText);
    addView(labelView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));

    editText = new EditText(context);
    editText.setBackgroundResource(R.drawable.bg_control_surface);
    editText.setHint(fieldHint);
    editText.setImportantForAutofill(IMPORTANT_FOR_AUTOFILL_NO);
    editText.setInputType(inputType);
    editText.setMinHeight(dp(48));
    editText.setPadding(dp(12), dp(10), dp(12), dp(10));
    editText.setSingleLine(true);
    editText.setTextColor(ContextCompat.getColor(context, R.color.finan_text_primary));
    editText.setHintTextColor(ContextCompat.getColor(context, R.color.finan_text_hint));
    
    updateTypeface(editText.getText().toString());
    editText.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {}

      @Override
      public void afterTextChanged(Editable s) {
        updateTypeface(s.toString());
      }
    });

    LayoutParams inputParams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
    inputParams.topMargin = dp(6);
    addView(editText, inputParams);
  }

  private void updateTypeface(String text) {
    if (text.isEmpty()) {
      editText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.ITALIC));
    } else {
      editText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
    }
  }

  public EditText getEditText() {
    return editText;
  }

  private int dp(int value) {
    return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
  }
}
