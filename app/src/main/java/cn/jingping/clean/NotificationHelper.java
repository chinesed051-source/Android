package cn.jingping.clean;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

public final class NotificationHelper {
    private static final String CHANNEL = "jingping_protection";
    private static final int ID = 1001;

    private NotificationHelper() {}

    public static void show(Context context) {
        if (Build.VERSION.SDK_INT >= 33 && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "净屏保护", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("显示广告自动处理状态");
            c.setShowBadge(false);
            nm.createNotificationChannel(c);
        }

        Intent open = new Intent(context, MainActivity.class);
        PendingIntent openPi = PendingIntent.getActivity(context, 1, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Intent toggle = new Intent(context, NotificationActionReceiver.class).setAction(NotificationActionReceiver.ACTION_TOGGLE);
        PendingIntent togglePi = PendingIntent.getBroadcast(context, 2, toggle, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        boolean enabled = Prefs.isEnabled(context);
        String state = enabled ? "保护运行中" : "已暂停";
        String text = "已自动处理 " + Prefs.getCount(context) + " 次 · " + (Prefs.isAggressive(context) ? "激进模式" : "保守模式");

        Notification.Builder b = Build.VERSION.SDK_INT >= 26
                ? new Notification.Builder(context, CHANNEL)
                : new Notification.Builder(context);
        Notification n = b
                .setSmallIcon(R.drawable.ic_shield)
                .setContentTitle("净屏 · " + state)
                .setContentText(text)
                .setContentIntent(openPi)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setCategory(Notification.CATEGORY_SERVICE)
                .addAction(new Notification.Action.Builder(0, enabled ? "暂停" : "开启", togglePi).build())
                .build();
        nm.notify(ID, n);
    }

    public static void cancel(Context context) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.cancel(ID);
    }
}
