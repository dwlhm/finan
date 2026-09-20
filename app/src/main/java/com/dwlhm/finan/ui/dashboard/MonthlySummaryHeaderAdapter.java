package com.dwlhm.finan.ui.dashboard;

import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

/**
 * Provides the single summary item that precedes the transaction details.
 */
final class MonthlySummaryHeaderAdapter
        extends RecyclerView.Adapter<MonthlySummaryHeaderAdapter.ViewHolder> {

    private final View summaryView;

    MonthlySummaryHeaderAdapter(@NonNull View summaryView) {
        this.summaryView = summaryView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(summaryView);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        // The summary view is bound by MonthlyDashboardFragment's data callback.
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    public static final class ViewHolder extends RecyclerView.ViewHolder {
        ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
