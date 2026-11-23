package com.pip.keypet.glass;

import android.service.voice.VoiceInteractionSession;
import android.service.voice.VoiceInteractionSessionService;

public final class PipVoiceInteractionSessionService extends VoiceInteractionSessionService {
    @Override
    public VoiceInteractionSession onNewSession(android.os.Bundle args) {
        return new PipVoiceInteractionSession(this);
    }
}