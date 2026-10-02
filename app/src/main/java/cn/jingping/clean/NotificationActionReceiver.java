package cn.jingping.clean;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationActionReceiver extends BroadcastReceiver {
    public static final String ACTION_TOGGLE = "cn.jingping.clean.TOGGLE";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && ACTION_TOGGLE.equals(intent.getAction())) {
            Prefs.setEnabled(context, !Prefs.isEnabled(context));
            UiUpdater.refresh(context);
        }
    }
}
