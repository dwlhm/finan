package com.dwlhm.finan.ui.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.dwlhm.finan.R;

public class CalculatorStripView extends LinearLayout {

    private TextView expressionText;

    public CalculatorStripView(@NonNull Context context) {
        super(context);
        init(context);
    }

    public CalculatorStripView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public CalculatorStripView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_calculator_strip, this, true);
        expressionText = findViewById(R.id.calculator_expression_text);
    }

    public void show(@NonNull String expression) {
        expressionText.setText(expression);
    }
}
