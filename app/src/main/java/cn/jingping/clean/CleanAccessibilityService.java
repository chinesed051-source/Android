package cn.jingping.clean;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

public class CleanAccessibilityService extends AccessibilityService {
    private RuleEngine ruleEngine;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        ruleEngine = new RuleEngine(this);
        NotificationHelper.show(this);
        UiUpdater.refresh(this);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (!Prefs.isEnabled(this) || ruleEngine == null || event == null) return;
        CharSequence pkg = event.getPackageName();
        if (pkg == null) return;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        try {
            ruleEngine.process(root, pkg.toString(), Prefs.isAggressive(this));
        } finally {
            root.recycle();
        }
    }

    @Override
    public void onInterrupt() {
        // No continuous gesture is held, so nothing is required here.
    }

    @Override
    public void onDestroy() {
        NotificationHelper.cancel(this);
        super.onDestroy();
    }
}
