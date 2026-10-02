package cn.jingping.clean;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

public final class UiUpdater {
    private UiUpdater() {}

    public static void refresh(Context context) {
        NotificationHelper.show(context);
        AppWidgetManager awm = AppWidgetManager.getInstance(context);
        int[] ids = awm.getAppWidgetIds(new ComponentName(context, CleanWidgetProvider.class));
        if (ids != null && ids.length > 0) CleanWidgetProvider.updateAll(context, awm, ids);

        if (Build.VERSION.SDK_INT >= 24) {
            try {
                TileService.requestListeningState(context, new ComponentName(context, QuickToggleTileService.class));
            } catch (Throwable ignored) {}
        }
        context.sendBroadcast(new Intent(MainActivity.ACTION_REFRESH));
    }
}
