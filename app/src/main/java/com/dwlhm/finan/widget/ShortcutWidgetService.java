package com.dwlhm.finan.widget;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import com.dwlhm.finan.R;
import com.dwlhm.finan.domain.model.TransactionTemplate;
import com.dwlhm.finan.ui.common.AppServices;

import java.util.List;

public class ShortcutWidgetService extends RemoteViewsService {

    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new ShortcutRemoteViewsFactory(this.getApplicationContext(), intent);
    }

    public static class ShortcutRemoteViewsFactory implements RemoteViewsService.RemoteViewsFactory {

        private final Context context;
        private List<TransactionTemplate> templates;

        public ShortcutRemoteViewsFactory(Context context, Intent intent) {
            this.context = context;
        }

        @Override
        public void onCreate() {
            AppServices services = AppServices.create(context);
            templates = services.transactionTemplateDao.findAll();
        }

        @Override
        public void onDataSetChanged() {
            AppServices services = AppServices.create(context);
            templates = services.transactionTemplateDao.findAll();
        }

        @Override
        public void onDestroy() {
            if (templates != null) {
                templates.clear();
            }
        }

        @Override
        public int getCount() {
            return templates != null ? templates.size() : 0;
        }

        @Override
        public RemoteViews getViewAt(int position) {
            if (templates == null || position < 0 || position >= templates.size()) {
                return null;
            }

            TransactionTemplate template = templates.get(position);
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.item_widget_shortcut_grid);

            boolean isCancelledActive = WidgetStateStore.isPendingShortcutCancelledActive(context, template.getId());
            boolean isUndoActive = !isCancelledActive && WidgetStateStore.isPendingShortcutUndoActive(context) && template.getId() == WidgetStateStore.getPendingShortcutUndoId(context);

            if (isCancelledActive) {
                views.setViewVisibility(R.id.layout_item_normal, View.GONE);
                views.setViewVisibility(R.id.layout_item_undo, View.GONE);
                views.setViewVisibility(R.id.layout_item_cancelled, View.VISIBLE);
            } else if (isUndoActive) {
                views.setViewVisibility(R.id.layout_item_normal, View.GONE);
                views.setViewVisibility(R.id.layout_item_undo, View.VISIBLE);
                views.setViewVisibility(R.id.layout_item_cancelled, View.GONE);
                long deadline = WidgetStateStore.getPendingShortcutUndoDeadline(context);
                int sec = (int) Math.ceil((deadline - System.currentTimeMillis()) / 1000.0);
                views.setTextViewText(R.id.tv_btn_batal, "Batal (" + Math.max(sec, 1) + "s)");
                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(ShortcutWidgetProvider.EXTRA_SHORTCUT_ACTION, ShortcutWidgetProvider.ACTION_TYPE_UNDO);
                views.setOnClickFillInIntent(R.id.tv_btn_batal, fillInIntent);
            } else {
                views.setViewVisibility(R.id.layout_item_normal, View.VISIBLE);
                views.setViewVisibility(R.id.layout_item_undo, View.GONE);
                views.setViewVisibility(R.id.layout_item_cancelled, View.GONE);
                views.setTextViewText(R.id.tv_item_icon, template.getIcon() != null && !template.getIcon().isEmpty() ? template.getIcon() : "⚡");
                views.setTextViewText(R.id.tv_item_name, template.getName());
                views.setTextViewText(R.id.tv_item_amount, ShortcutWidgetProvider.formatCompactAmount(template.getAmountMinor()));
                Intent fillInIntent = new Intent();
                fillInIntent.putExtra(ShortcutWidgetProvider.EXTRA_SHORTCUT_ACTION, ShortcutWidgetProvider.ACTION_TYPE_EXECUTE);
                fillInIntent.putExtra(ShortcutWidgetProvider.EXTRA_SHORTCUT_ID, template.getId());
                views.setOnClickFillInIntent(R.id.layout_item_normal, fillInIntent);
            }
            return views;
        }

        @Override
        public RemoteViews getLoadingView() {
            return null;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }
}
