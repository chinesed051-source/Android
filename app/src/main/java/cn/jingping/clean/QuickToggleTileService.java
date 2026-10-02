package cn.jingping.clean;

import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

public class QuickToggleTileService extends TileService {
    @Override
    public void onStartListening() {
        super.onStartListening();
        updateTile();
    }

    @Override
    public void onClick() {
        super.onClick();
        Prefs.setEnabled(this, !Prefs.isEnabled(this));
        UiUpdater.refresh(this);
        updateTile();
    }

    private void updateTile() {
        Tile tile = getQsTile();
        if (tile == null) return;
        boolean enabled = Prefs.isEnabled(this);
        tile.setState(enabled ? Tile.STATE_ACTIVE : Tile.STATE_INACTIVE);
        tile.setLabel("净屏");
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            tile.setSubtitle(enabled ? "保护中" : "已暂停");
        }
        tile.updateTile();
    }
}
