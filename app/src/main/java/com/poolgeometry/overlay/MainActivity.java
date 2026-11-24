package com.poolgeometry.overlay;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;
import android.view.ViewGroup;
import android.graphics.Color;

public class MainActivity extends Activity {
    private static final int REQ_OVERLAY = 410;
    private static final int REQ_NOTIFICATIONS = 411;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(28, 36, 28, 28);
        page.setBackgroundColor(Color.rgb(18, 13, 25));

        TextView title = new TextView(this);
        title.setText("POOL GEOMETRY");
        title.setTextColor(Color.WHITE);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 12, 0, 8);
        page.addView(title, new LinearLayout.LayoutParams(-1, -2));

        TextView info = new TextView(this);
        info.setText("Overlay Android — versão inicial\\n\\nAutorize a sobreposição para exibir as linhas por cima do 8 Ball Pool. Depois, inicie o menu flutuante e abra o jogo.");
        info.setTextColor(0xFFD6CADD);
        info.setTextSize(15);
        info.setPadding(0, 12, 0, 22);
        page.addView(info, new LinearLayout.LayoutParams(-1, -2));

        Button permission = button("1. Autorizar sobreposição");
        permission.setOnClickListener(v -> requestOverlayPermission());
        page.addView(permission, buttonParams());

        Button start = button("2. Iniciar menu flutuante");
        start.setOnClickListener(v -> startOverlay());
        page.addView(start, buttonParams());

        Button stop = button("Parar overlay");
        stop.setOnClickListener(v -> stopService(new Intent(this, OverlayService.class)));
        page.addView(stop, buttonParams());

        setContentView(page);
    }

    private Button button(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        return b;
    }
    private LinearLayout.LayoutParams buttonParams() {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2);
        p.bottomMargin = 12;
        return p;
    }
    private void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivityForResult(i, REQ_OVERLAY);
        } else {
            new AlertDialog.Builder(this).setMessage("A permissão de sobreposição já está ativa.")
                    .setPositiveButton("OK", null).show();
        }
    }
    private void startOverlay() {
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            requestOverlayPermission();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFICATIONS);
        }
        Intent i = new Intent(this, OverlayService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
        finish();
    }
}
