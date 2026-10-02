package cn.jingping.clean;

import android.Manifest;
import android.app.Activity;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    public static final String ACTION_REFRESH = "cn.jingping.clean.REFRESH_UI";

    private TextView statusText;
    private TextView countText;
    private Switch enabledSwitch;
    private Switch aggressiveSwitch;

    private final BroadcastReceiver refreshReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) { refreshUi(); }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        buildUi();
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter f = new IntentFilter(ACTION_REFRESH);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(refreshReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(refreshReceiver, f);
    }

    @Override
    protected void onStop() {
        try { unregisterReceiver(refreshReceiver); } catch (Throwable ignored) {}
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.WHITE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(22), dp(26), dp(22), dp(36));
        scroll.addView(root, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = text("净屏", 34, Color.rgb(17,17,17), true);
        root.addView(title);
        TextView sub = text("不 Root · 不占 VPN · 本地处理", 15, Color.rgb(90,90,90), false);
        sub.setPadding(0, dp(5), 0, dp(20));
        root.addView(sub);

        LinearLayout statusCard = card();
        statusText = text("检查中…", 20, Color.rgb(20,20,20), true);
        statusCard.addView(statusText);
        countText = text("已自动处理 0 次", 14, Color.rgb(95,95,95), false);
        countText.setPadding(0, dp(8), 0, 0);
        statusCard.addView(countText);
        root.addView(statusCard, marginBottom(dp(14)));

        Button accessibility = button("开启 / 管理无障碍服务");
        accessibility.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)); }
            catch (Throwable t) { Toast.makeText(this, "无法打开无障碍设置", Toast.LENGTH_SHORT).show(); }
        });
        root.addView(accessibility, marginBottom(dp(12)));

        LinearLayout controls = card();
        enabledSwitch = new Switch(this);
        enabledSwitch.setText("保护总开关");
        enabledSwitch.setTextSize(17);
        enabledSwitch.setPadding(0, dp(2), 0, dp(10));
        enabledSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                Prefs.setEnabled(this, isChecked);
                UiUpdater.refresh(this);
                refreshUi();
            }
        });
        controls.addView(enabledSwitch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        aggressiveSwitch = new Switch(this);
        aggressiveSwitch.setText("激进模式（提高命中率）");
        aggressiveSwitch.setTextSize(16);
        aggressiveSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (buttonView.isPressed()) {
                Prefs.setAggressive(this, isChecked);
                UiUpdater.refresh(this);
            }
        });
        controls.addView(aggressiveSwitch, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView hint = text("保守模式优先处理明确的“跳过/关闭广告”；激进模式也会尝试处理右上角“关闭/×”，命中更多，但误触风险略高。", 13, Color.rgb(105,105,105), false);
        hint.setPadding(0, dp(10), 0, 0);
        controls.addView(hint);
        root.addView(controls, marginBottom(dp(14)));

        Button widget = button("添加桌面小组件");
        widget.setOnClickListener(v -> requestWidget());
        root.addView(widget, marginBottom(dp(10)));

        Button notify = button("刷新通知栏控制");
        notify.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 7);
            } else {
                NotificationHelper.show(this);
                Toast.makeText(this, "通知栏状态已刷新", Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(notify, marginBottom(dp(14)));

        LinearLayout privacy = card();
        privacy.addView(text("隐私设计", 17, Color.rgb(20,20,20), true));
        TextView p = text("• 当前版本没有 INTERNET 权限\n• 不上传屏幕内容\n• 不读取聊天记录\n• 拦截次数仅保存在本机\n• 不使用 VPN，因此不会和 v2rayNG / Clash 抢 VPN 槽", 14, Color.rgb(80,80,80), false);
        p.setPadding(0, dp(8), 0, 0);
        privacy.addView(p);
        root.addView(privacy);

        setContentView(scroll);
    }

    private void refreshUi() {
        boolean service = isAccessibilityServiceEnabled();
        boolean enabled = Prefs.isEnabled(this);
        statusText.setText(service ? (enabled ? "● 保护运行中" : "● 无障碍已开启 · 当前暂停") : "○ 需要开启无障碍服务");
        statusText.setTextColor(service && enabled ? Color.rgb(22, 120, 75) : Color.rgb(55,55,55));
        countText.setText("已自动处理 " + Prefs.getCount(this) + " 次");
        enabledSwitch.setChecked(enabled);
        aggressiveSwitch.setChecked(Prefs.isAggressive(this));
    }

    private boolean isAccessibilityServiceEnabled() {
        String expected = new ComponentName(this, CleanAccessibilityService.class).flattenToString();
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (enabled == null) return false;
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        while (splitter.hasNext()) {
            if (expected.equalsIgnoreCase(splitter.next())) return true;
        }
        return false;
    }

    private void requestWidget() {
        AppWidgetManager m = AppWidgetManager.getInstance(this);
        ComponentName provider = new ComponentName(this, CleanWidgetProvider.class);
        if (Build.VERSION.SDK_INT >= 26 && m.isRequestPinAppWidgetSupported()) {
            boolean requested = m.requestPinAppWidget(provider, null, null);
            if (!requested) Toast.makeText(this, "请长按桌面 → 小组件 → 净屏", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(this, "请长按桌面 → 小组件 → 净屏", Toast.LENGTH_LONG).show();
        }
    }

    private LinearLayout card() {
        LinearLayout l = new LinearLayout(this);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setPadding(dp(18), dp(17), dp(18), dp(17));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.rgb(247,249,248));
        bg.setCornerRadius(dp(20));
        bg.setStroke(dp(1), Color.rgb(228,233,230));
        l.setBackground(bg);
        return l;
    }

    private Button button(String label) {
        Button b = new Button(this);
        b.setText(label);
        b.setTextSize(16);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setMinHeight(dp(52));
        return b;
    }

    private TextView text(String value, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(t.getTypeface(), android.graphics.Typeface.BOLD);
        return t;
    }

    private LinearLayout.LayoutParams marginBottom(int px) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = px;
        return lp;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
