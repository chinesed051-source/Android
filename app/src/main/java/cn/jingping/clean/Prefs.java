package cn.jingping.clean;

import android.content.Context;
import android.content.SharedPreferences;

public final class Prefs {
    private static final String NAME = "jingping_prefs";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_AGGRESSIVE = "aggressive";
    private static final String KEY_COUNT = "handled_count";

    private Prefs() {}

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static boolean isEnabled(Context c) {
        return sp(c).getBoolean(KEY_ENABLED, true);
    }

    public static void setEnabled(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_ENABLED, value).apply();
    }

    public static boolean isAggressive(Context c) {
        return sp(c).getBoolean(KEY_AGGRESSIVE, false);
    }

    public static void setAggressive(Context c, boolean value) {
        sp(c).edit().putBoolean(KEY_AGGRESSIVE, value).apply();
    }

    public static int getCount(Context c) {
        return sp(c).getInt(KEY_COUNT, 0);
    }

    public static int incrementCount(Context c) {
        int next = getCount(c) + 1;
        sp(c).edit().putInt(KEY_COUNT, next).apply();
        return next;
    }
}
