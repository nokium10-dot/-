package com.pip.keypet.glass;

import android.content.Intent;
import android.os.Bundle;
import android.service.voice.VoiceInteractionSession;

public final class PipVoiceInteractionSession extends VoiceInteractionSession {
    public PipVoiceInteractionSession(android.content.Context context) {
        super(context);
    }

    @Override
    public void onShow(Bundle args, int showFlags) {
        super.onShow(args, showFlags);
        Intent launch = new Intent(getContext(), PipActivity.class);
        launch.setAction(Intent.ACTION_ASSIST);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startAssistantActivity(launch);
        finish();
    }
}