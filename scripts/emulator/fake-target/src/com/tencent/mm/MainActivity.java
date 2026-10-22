package com.tencent.mm;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

/**
 * Minimal target used by the emulator test environment. It is NOT WeChat: it only
 * registers the step sensors and logs whether the platform provides them.
 */
public class MainActivity extends Activity implements SensorEventListener {
    private static final String TAG = "FakeWeChat";
    private SensorManager sensorManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView output = new TextView(this);
        setContentView(output);
        sensorManager = getSystemService(SensorManager.class);

        StringBuilder sb = new StringBuilder("FakeWeChat\nsensors:\n");
        for (Sensor s : sensorManager.getSensorList(Sensor.TYPE_ALL)) {
            if (s.getType() == Sensor.TYPE_STEP_COUNTER || s.getType() == Sensor.TYPE_STEP_DETECTOR) {
                sb.append("  step type=").append(s.getType()).append(" name=").append(s.getName()).append("\n");
            }
        }
        Log.i(TAG, sb.toString());

        Sensor counter = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
        Sensor detector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR);
        if (counter != null) sensorManager.registerListener(this, counter, SensorManager.SENSOR_DELAY_NORMAL);
        if (detector != null) sensorManager.registerListener(this, detector, SensorManager.SENSOR_DELAY_NORMAL);
        output.setText(sb.append("counter=").append(counter).append("\ndetector=").append(detector));
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.i(TAG, "sensor type=" + event.sensor.getType() + " value0=" + event.values[0]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}
}
