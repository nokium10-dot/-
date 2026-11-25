package com.pipru.v2;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Handler;

/** Короткий «пищащий» звук Пипа перед фразой: весёлый, злой или грустный. Синтезируется на лету. */
final class PipVoiceFx {
    private static final int RATE = 22050;

    private PipVoiceFx() {
    }

    /** Параметры голоса для синтезатора: {высота, скорость}. */
    static float[] voice(String mood) {
        if ("angry".equals(mood)) return new float[]{1.15f, 1.3f};
        if ("sad".equals(mood)) return new float[]{1.45f, 0.9f};
        if ("happy".equals(mood)) return new float[]{1.9f, 1.12f};
        return new float[]{1.7f, 1.08f};
    }

    /** Проигрывает писк и возвращает его длительность в мс. */
    static long chirp(String mood, Handler main) {
        float[][] notes;
        if ("angry".equals(mood)) notes = new float[][]{{430, 190, 150}, {360, 150, 170}};
        else if ("sad".equals(mood)) notes = new float[][]{{820, 480, 240}};
        else if ("happy".equals(mood)) notes = new float[][]{{900, 1300, 80}, {1300, 1750, 110}};
        else notes = new float[][]{{1000, 1500, 120}};
        boolean rough = "angry".equals(mood);

        int total = 0;
        for (float[] n : notes) total += (int) (RATE * n[2] / 1000f);
        short[] pcm = new short[total];
        int pos = 0;
        double phase = 0;
        for (float[] n : notes) {
            int len = (int) (RATE * n[2] / 1000f);
            for (int i = 0; i < len; i++) {
                float t = i / (float) len;
                double f = n[0] + (n[1] - n[0]) * t;
                phase += 2 * Math.PI * f / RATE;
                double v = Math.sin(phase) + 0.3 * Math.sin(2 * phase) + (rough ? 0.45 * Math.sin(3 * phase) : 0);
                double env = Math.min(1.0, i / (RATE * 0.006)) * Math.pow(1.0 - t, 0.7);
                pcm[pos++] = (short) (v * env * 0.26 * Short.MAX_VALUE);
            }
        }
        long ms = total * 1000L / RATE;
        try {
            final AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.length * 2)
                    .build();
            track.write(pcm, 0, pcm.length);
            track.play();
            main.postDelayed(track::release, ms + 300);
        } catch (RuntimeException e) {
            return 0;
        }
        return ms;
    }
}
