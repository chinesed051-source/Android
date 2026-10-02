package cn.jingping.clean;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class CleanWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_WIDGET_TOGGLE = "cn.jingping.clean.WIDGET_TOGGLE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        updateAll(context, appWidgetManager, appWidgetIds);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent != null && ACTION_WIDGET_TOGGLE.equals(intent.getAction())) {
            Prefs.setEnabled(context, !Prefs.isEnabled(context));
            UiUpdater.refresh(context);
        }
    }

    static void updateAll(Context context, AppWidgetManager manager, int[] ids) {
        boolean enabled = Prefs.isEnabled(context);
        for (int id : ids) {
            RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.widget_clean_screen);
            rv.setTextViewText(R.id.widget_title, enabled ? "净屏 · 保护中" : "净屏 · 已暂停");
            rv.setTextViewText(R.id.widget_count, "已自动处理 " + Prefs.getCount(context) + " 次");
            rv.setTextViewText(R.id.widget_toggle, enabled ? "暂停" : "开启");

            Intent toggle = new Intent(context, CleanWidgetProvider.class).setAction(ACTION_WIDGET_TOGGLE);
            PendingIntent togglePi = PendingIntent.getBroadcast(context, id, toggle, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.widget_toggle, togglePi);

            Intent open = new Intent(context, MainActivity.class);
            PendingIntent openPi = PendingIntent.getActivity(context, 10000 + id, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            rv.setOnClickPendingIntent(R.id.widget_title, openPi);
            rv.setOnClickPendingIntent(R.id.widget_count, openPi);
            manager.updateAppWidget(id, rv);
        }
    }
}
