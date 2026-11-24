package com.pipru.v2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/** Открывается шестерёнкой рядом с Пипом в системных настройках помощника. */
public class AssistantSettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, PipActivity.class)
                .putExtra(PipActivity.EXTRA_OPEN_SETTINGS, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }
}
