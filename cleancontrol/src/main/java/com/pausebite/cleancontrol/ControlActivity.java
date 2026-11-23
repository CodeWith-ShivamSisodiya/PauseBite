package com.pausebite.cleancontrol;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class ControlActivity extends Activity {
 @Override public void onCreate(Bundle state) {
  super.onCreate(state);
  TextView label = new TextView(this);
  label.setText("PauseBite controlled installation test");
  label.setTextSize(20f);
  label.setPadding(24,48,24,24);
  setContentView(label);
 }
}
