package com.pipru.v2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;
import android.view.View;

/**
 * Вызов помощника (долгое нажатие «Домой»/питания, жест из угла): поверх текущего приложения выезжает Пип и слушает.
 * Если микрофон ещё не разрешён, открывается приложение, чтобы запросить разрешение.
 */
public class PipVoiceSession extends VoiceInteractionSession implements PipOverlay.Host {
    private PipOverlay overlay;
    private boolean held;

    public PipVoiceSession(Context context) {
        super(context);
    }

    @Override
    public View onCreateContentView() {
        if (overlay == null) overlay = new PipOverlay(getContext(), this);
        return overlay.createView();
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        if (!held) {
            held = true;
            PipWidgetService.hold(true);
        }
        if (overlay != null) overlay.show();
    }

    @Override
    public void onHide() {
        super.onHide();
        if (overlay != null) overlay.onHidden();
        release();
    }

    @Override
    public void onDestroy() {
        if (overlay != null) overlay.destroy();
        overlay = null;
        release();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (overlay != null) overlay.closeAnimated();
        else hide();
    }

    private void release() {
        if (held) {
            held = false;
            PipWidgetService.hold(false);
        }
    }

    @Override
    public void dismiss() {
        hide();
    }

    @Override
    public void requestMic() {
        Intent intent = new Intent(getContext(), PipActivity.class)
                .setAction(Intent.ACTION_ASSIST)
                .putExtra(PipActivity.EXTRA_AUTO_LISTEN, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        try {
            startAssistantActivity(intent);
        } catch (RuntimeException e) {
            try {
                getContext().startActivity(intent);
            } catch (RuntimeException ignored) {
            }
        }
        hide();
    }
}
