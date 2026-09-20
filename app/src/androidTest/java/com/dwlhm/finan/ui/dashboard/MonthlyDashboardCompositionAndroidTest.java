package com.dwlhm.finan.ui.dashboard;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.view.LayoutInflater;
import android.view.View;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.dwlhm.finan.R;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MonthlyDashboardCompositionAndroidTest {

    @Test
    public void monthlyDashboardUsesOneSummaryHeaderAndNoTabbedSurfaces() {
        View root = LayoutInflater.from(ApplicationProvider.getApplicationContext())
                .inflate(R.layout.fragment_monthly_dashboard, null, false);
        View summary = LayoutInflater.from(ApplicationProvider.getApplicationContext())
                .inflate(R.layout.item_monthly_summary, null, false);

        MonthlySummaryHeaderAdapter adapter = new MonthlySummaryHeaderAdapter(summary);
        Context context = ApplicationProvider.getApplicationContext();

        assertEquals(1, adapter.getItemCount());
        assertEquals(0, context.getResources().getIdentifier(
                "monthly_tabs", "id", context.getPackageName()));
        assertEquals(0, context.getResources().getIdentifier(
                "monthly_summary_scroll", "id", context.getPackageName()));
        assertEquals(0, context.getResources().getIdentifier(
                "monthly_empty", "id", context.getPackageName()));
        assertNotNull(summary.findViewById(R.id.card_monthly_runway));
        assertNotNull(summary.findViewById(R.id.monthly_weekly_chart));
        assertNotNull(summary.findViewById(R.id.monthly_category_chart));
        TextView transactionsTitle = summary.findViewById(R.id.monthly_transactions_title);
        assertEquals(context.getString(R.string.monthly_transactions_section_title),
                transactionsTitle.getText().toString());
    }

    @Test
    public void weeklyDetailContainsRequiredSectionsAndCollapsedCategories() {
        Context context = ApplicationProvider.getApplicationContext();
        View sheet = LayoutInflater.from(context)
                .inflate(R.layout.dialog_weekly_detail, null, false);
        View day = LayoutInflater.from(context)
                .inflate(R.layout.item_weekly_detail_day, null, false);

        assertNotNull(sheet.findViewById(R.id.weekly_detail_header_icon));
        assertNotNull(sheet.findViewById(R.id.weekly_detail_date_range));
        assertNotNull(sheet.findViewById(R.id.weekly_detail_total));
        assertNotNull(sheet.findViewById(R.id.weekly_detail_comparison));
        assertNotNull(sheet.findViewById(R.id.weekly_detail_comparison_label));
        View dateControl = sheet.findViewById(R.id.weekly_detail_date_control);
        assertFalse(dateControl.isClickable());
        assertFalse(dateControl.isFocusable());
        ImageView headerIcon = sheet.findViewById(R.id.weekly_detail_header_icon);
        assertTrue(headerIcon instanceof ImageView);
        assertNotNull(headerIcon.getDrawable());
        assertNotNull(context.getDrawable(R.drawable.ic_weekly_detail_income));
        assertNotNull(context.getDrawable(R.drawable.ic_weekly_detail_expense));
        assertNotNull(sheet.findViewById(R.id.weekly_detail_recycler));
        assertEquals(View.GONE, day.findViewById(R.id.weekly_detail_categories_container)
                .getVisibility());
        assertTrue(day.findViewById(R.id.weekly_detail_day_expand) instanceof ImageView);
    }

    @Test
    public void weeklyDetailActionsExposeAtLeast48DpTargets() {
        Context context = ApplicationProvider.getApplicationContext();
        View sheet = LayoutInflater.from(context)
                .inflate(R.layout.dialog_weekly_detail, null, false);
        int minimum = (int) (48 * context.getResources().getDisplayMetrics().density);

        assertTargetAtLeast48dp(sheet.findViewById(R.id.weekly_detail_close), minimum);
        assertTargetAtLeast48dp(sheet.findViewById(R.id.weekly_detail_previous), minimum);
        assertTargetAtLeast48dp(sheet.findViewById(R.id.weekly_detail_next), minimum);
    }

    private static void assertTargetAtLeast48dp(View target, int minimum) {
        assertNotNull(target);
        assertEquals(minimum, target.getLayoutParams().width);
        assertEquals(minimum, target.getLayoutParams().height);
    }
}
