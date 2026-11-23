package com.pipru.assistant;

import android.Manifest;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognitionService;
import android.speech.SpeechRecognizer;

/**
 * Нужен системе как часть «цифрового помощника». Сам речь не распознаёт, а честно передаёт
 * запрос настоящему сервису распознавания (Google и т.п.), чтобы другие приложения
 * не ломались, если Пип стал помощником.
 */
public class PipRecognitionService extends RecognitionService {
    private final Handler main = new Handler(Looper.getMainLooper());
    private SpeechRecognizer delegate;

    @Override
    protected void onStartListening(Intent recognizerIntent, Callback callback) {
        main.post(() -> start(new Intent(recognizerIntent), callback));
    }

    @Override
    protected void onCancel(Callback callback) {
        main.post(() -> {
            if (delegate != null) delegate.cancel();
        });
    }

    @Override
    protected void onStopListening(Callback callback) {
        main.post(() -> {
            if (delegate != null) delegate.stopListening();
        });
    }

    @Override
    public void onDestroy() {
        main.post(this::destroyDelegate);
        super.onDestroy();
    }

    private void start(Intent intent, Callback callback) {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            fail(callback, SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS);
            return;
        }
        ComponentName target = RecognizerPicker.pick(this);
        if (target == null) {
            fail(callback, SpeechRecognizer.ERROR_CLIENT);
            return;
        }
        destroyDelegate();
        try {
            delegate = SpeechRecognizer.createSpeechRecognizer(this, target);
            delegate.setRecognitionListener(new Relay(callback));
            delegate.startListening(intent);
        } catch (RuntimeException e) {
            destroyDelegate();
            fail(callback, SpeechRecognizer.ERROR_CLIENT);
        }
    }

    private void destroyDelegate() {
        if (delegate != null) {
            try {
                delegate.destroy();
            } catch (RuntimeException ignored) {
            }
            delegate = null;
        }
    }

    private static void fail(Callback callback, int code) {
        try {
            callback.error(code);
        } catch (RemoteException ignored) {
        }
    }

    private static final class Relay implements RecognitionListener {
        private final Callback callback;

        Relay(Callback callback) {
            this.callback = callback;
        }

        @Override public void onReadyForSpeech(Bundle params) {
            try { callback.readyForSpeech(params); } catch (RemoteException ignored) { }
        }
        @Override public void onBeginningOfSpeech() {
            try { callback.beginningOfSpeech(); } catch (RemoteException ignored) { }
        }
        @Override public void onRmsChanged(float rmsdB) {
            try { callback.rmsChanged(rmsdB); } catch (RemoteException ignored) { }
        }
        @Override public void onBufferReceived(byte[] buffer) {
            try { callback.bufferReceived(buffer); } catch (RemoteException ignored) { }
        }
        @Override public void onEndOfSpeech() {
            try { callback.endOfSpeech(); } catch (RemoteException ignored) { }
        }
        @Override public void onError(int error) {
            try { callback.error(error); } catch (RemoteException ignored) { }
        }
        @Override public void onResults(Bundle results) {
            try { callback.results(results); } catch (RemoteException ignored) { }
        }
        @Override public void onPartialResults(Bundle partialResults) {
            try { callback.partialResults(partialResults); } catch (RemoteException ignored) { }
        }
        @Override public void onEvent(int eventType, Bundle params) { }
    }
}
