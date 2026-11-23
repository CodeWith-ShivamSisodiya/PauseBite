package com.pausebite.probe;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
public class ProbeActivity extends Activity {
 @Override public void onCreate(Bundle state) { super.onCreate(state); TextView v = new TextView(this); v.setText("PauseBite code-component installation probe"); v.setTextSize(20f); v.setPadding(24,48,24,24); setContentView(v); }
}
