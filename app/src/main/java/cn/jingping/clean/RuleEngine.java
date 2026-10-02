package cn.jingping.clean;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Rect;
import android.os.SystemClock;
import android.graphics.Path;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.Locale;

final class RuleEngine {
    private final AccessibilityService service;
    private String lastPackage = "";
    private long packageStartAt = 0L;
    private long lastActionAt = 0L;

    RuleEngine(AccessibilityService service) {
        this.service = service;
    }

    boolean process(AccessibilityNodeInfo root, String packageName, boolean aggressive) {
        if (root == null || TextUtils.isEmpty(packageName) || isExcluded(packageName)) return false;

        long now = SystemClock.elapsedRealtime();
        if (now - lastActionAt < 700) return false;

        if (!packageName.equals(lastPackage)) {
            lastPackage = packageName;
            packageStartAt = now;
        }
        boolean launchWindow = now - packageStartAt <= 16000;

        DisplayMetrics dm = service.getResources().getDisplayMetrics();
        int sw = dm.widthPixels;
        int sh = dm.heightPixels;

        Candidate best = findBest(root, sw, sh, launchWindow, aggressive);
        int threshold = aggressive ? 5 : 7;
        if (best == null || best.score < threshold) return false;

        Rect target = new Rect();
        best.node.getBoundsInScreen(target);

        AccessibilityNodeInfo clickable = findClickable(best.node);
        boolean clicked = clickable != null && clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        if (!clicked && !target.isEmpty()) {
            clicked = gestureClick(target.centerX(), target.centerY());
        }

        if (clicked) {
            lastActionAt = now;
            Prefs.incrementCount(service);
            UiUpdater.refresh(service);
        }
        if (clickable != null) clickable.recycle();
        best.node.recycle();
        return clicked;
    }

    private Candidate findBest(AccessibilityNodeInfo root, int sw, int sh, boolean launchWindow, boolean aggressive) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        Candidate best = null;
        int visited = 0;

        while (!queue.isEmpty() && visited < 700) {
            AccessibilityNodeInfo n = queue.removeFirst();
            visited++;

            CharSequence t = n.getText();
            CharSequence d = n.getContentDescription();
            String label = normalize(!TextUtils.isEmpty(t) ? t.toString() : (!TextUtils.isEmpty(d) ? d.toString() : ""));

            if (!label.isEmpty()) {
                Rect r = new Rect();
                n.getBoundsInScreen(r);
                String viewId = n.getViewIdResourceName();
                int score = score(label, viewId, r, sw, sh, launchWindow, aggressive, n.isClickable());
                if (score > 0 && (best == null || score > best.score)) {
                    if (best != null) best.node.recycle();
                    best = new Candidate(AccessibilityNodeInfo.obtain(n), score);
                }
            }

            for (int i = 0; i < n.getChildCount(); i++) {
                AccessibilityNodeInfo child = n.getChild(i);
                if (child != null) queue.addLast(child);
            }
            if (n != root) n.recycle();
        }
        return best;
    }

    private int score(String label, String viewId, Rect r, int sw, int sh, boolean launchWindow, boolean aggressive, boolean clickable) {
        int score = 0;
        String lower = label.toLowerCase(Locale.ROOT);

        boolean explicitAdClose = label.equals("关闭广告") || label.equals("广告关闭") || label.equals("跳过广告");
        boolean skip = label.equals("跳过") || (label.startsWith("跳过") && label.length() <= 8);
        boolean tinyClose = label.equals("×") || label.equals("✕") || label.equals("✖") || lower.equals("x");
        boolean genericClose = label.equals("关闭") || label.equals("关掉");

        if (explicitAdClose) score += 9;
        if (skip && launchWindow) score += 7;
        if (tinyClose) score += aggressive ? 4 : (launchWindow ? 2 : 0);
        if (genericClose && aggressive) score += 4;

        if (viewId != null) {
            String id = viewId.toLowerCase(Locale.ROOT);
            boolean adish = id.contains("ad") || id.contains("splash") || id.contains("advert");
            if (id.contains("skip") && adish) score += 9;
            if (id.contains("close") && adish) score += 8;
            if (id.contains("tt_splash_skip") || id.contains("ksad_skip")) score += 10;
        }

        boolean upper = r.centerY() > 0 && r.centerY() < sh * 0.38f;
        boolean right = r.centerX() > sw * 0.55f;
        boolean corner = upper && right;
        if (corner) score += 3;
        else if (upper) score += 1;

        int w = Math.max(0, r.width());
        int h = Math.max(0, r.height());
        boolean compact = w < sw * 0.45f && h < sh * 0.20f;
        if (compact) score += 1;
        if (clickable) score += 1;

        if ((tinyClose || genericClose) && !corner) score -= 4;
        if (skip && !launchWindow && !label.contains("广告")) score -= 10;
        if (r.isEmpty()) score -= 5;
        return score;
    }


    private boolean gestureClick(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 45);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        try {
            return service.dispatchGesture(gesture, null, null);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private AccessibilityNodeInfo findClickable(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo cur = AccessibilityNodeInfo.obtain(node);
        for (int i = 0; i < 4 && cur != null; i++) {
            if (cur.isClickable() && cur.isEnabled()) return cur;
            AccessibilityNodeInfo parent = cur.getParent();
            cur.recycle();
            cur = parent;
        }
        if (cur != null) cur.recycle();
        return null;
    }

    private String normalize(String s) {
        return s.replace(" ", "")
                .replace("\n", "")
                .replace("\t", "")
                .trim();
    }

    private boolean isExcluded(String pkg) {
        return pkg.equals(service.getPackageName())
                || pkg.startsWith("com.android.systemui")
                || pkg.startsWith("com.android.settings")
                || pkg.startsWith("com.google.android.permissioncontroller")
                || pkg.startsWith("com.android.permissioncontroller")
                || pkg.startsWith("com.android.packageinstaller")
                || pkg.startsWith("com.google.android.packageinstaller")
                || pkg.contains("inputmethod");
    }

    private static final class Candidate {
        final AccessibilityNodeInfo node;
        final int score;

        Candidate(AccessibilityNodeInfo node, int score) {
            this.node = node;
            this.score = score;
        }
    }
}
