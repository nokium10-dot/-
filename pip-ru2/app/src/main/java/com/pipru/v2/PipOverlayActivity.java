package com.pipru.v2;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

/** Помощник в виде прозрачного окна поверх любого приложения: используется кнопкой «Голос» виджета и вызовом ASSIST. */
public final class PipOverlayActivity extends Activity implements PipOverlay.Host {
    private static final int REQ_AUDIO = 801;
    private PipOverlay overlay;
    private boolean shown;
    private boolean askingMic;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= 27) setShowWhenLocked(true);
        overlay = new PipOverlay(this, this);
        setContentView(overlay.createView());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!shown) {
            shown = true;
            overlay.show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        overlay.restartListening();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Ушли с экрана (например, открыли другое приложение): помощник закрывается, микрофон выключается.
        if (overlay != null) overlay.onHidden();
        if (!isFinishing() && !askingMic) finish();
    }

    @Override
    protected void onDestroy() {
        if (overlay != null) overlay.destroy();
        overlay = null;
        super.onDestroy();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onBackPressed() {
        if (overlay != null) overlay.closeAnimated();
        else super.onBackPressed();
    }

    @Override
    public void dismiss() {
        finish();
        overridePendingTransition(0, 0);
    }

    @Override
    public void requestMic() {
        askingMic = true;
        requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, REQ_AUDIO);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode != REQ_AUDIO) return;
        askingMic = false;
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            if (overlay != null) overlay.micGranted();
        } else {
            Toast.makeText(this, "Без доступа к микрофону Пип не услышит команду", Toast.LENGTH_LONG).show();
            finish();
        }
    }
}
