package com.pipru.v2;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

/** Вызов помощника (долгое нажатие «Домой»/питания, жест из угла): открываем Пипа, он сразу слушает. */
public class PipVoiceSession extends VoiceInteractionSession {
    public PipVoiceSession(Context context) {
        super(context);
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
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
