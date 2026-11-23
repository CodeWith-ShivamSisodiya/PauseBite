package com.pausebite.permissionbaseline;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class BaselineActivity extends Activity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  LinearLayout layout = new LinearLayout(this);
  layout.setOrientation(LinearLayout.VERTICAL);
  int pad = (int)(24 * getResources().getDisplayMetrics().density);
  layout.setPadding(pad,pad,pad,pad);
  TextView title = new TextView(this);
  title.setText("PauseBite — Permission Baseline");
  title.setTextSize(24);
  layout.addView(title);
  TextView detail = new TextView(this);
  detail.setText("Diagnostic build only. No monitoring, overlays, background services, internet access or requested permissions.\n\nPlease note whether Google Play Protect warns before installation. This does not replace your main PauseBite app.");
  detail.setTextSize(16);
  layout.addView(detail);
  setContentView(layout);
 }
}
