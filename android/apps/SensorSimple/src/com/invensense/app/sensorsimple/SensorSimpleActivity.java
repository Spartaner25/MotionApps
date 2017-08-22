package com.invensense.app.sensorsimple;

import java.util.Iterator;
import java.util.Vector;
import android.app.Activity;
import android.os.Bundle;
import android.os.PowerManager;
//import android.util.Log;
import android.view.View;
//import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.CheckBox;
import android.widget.Spinner;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.hardware.*;

public class SensorSimpleActivity extends Activity  {
    static Boolean use_wakelock = false;
    final String tag = "SensorSimple";
    SensorManager sm = null;
    static final int[] rates = {
        SensorManager.SENSOR_DELAY_FASTEST, 
        SensorManager.SENSOR_DELAY_GAME, 
        SensorManager.SENSOR_DELAY_UI, 
        SensorManager.SENSOR_DELAY_NORMAL
    };
    Vector<SensorFoo> mSens = new Vector<SensorFoo>();    // list/vector of data visualizers
    PowerManager.WakeLock wl;

    public class SensorFoo implements SensorEventListener, OnCheckedChangeListener, OnItemSelectedListener {
        TextView mName;        // Sensor name label
        TextView[] mData;   // Data
        CheckBox mEnable;   // Enable/Disable checkbox
        Spinner mRate;      // Rate selection menu
        Sensor mSensor; 
        TextView mStatus;    // Accuracy indication
        
        public SensorFoo(String name, Sensor s, CheckBox e, TextView n, TextView[] d, Spinner r, TextView st) {
            mName = n;
            mData = d;
            mEnable = e;
            mRate = r;
            mSensor = s;
            mStatus = st;
            
            ArrayAdapter <CharSequence>adapter = ArrayAdapter.createFromResource(
                    SensorSimpleActivity.this, R.array.rates, android.R.layout.simple_spinner_item);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            mRate.setAdapter(adapter);
            mRate.setSelection(1);
            mName.setText(name);
            mEnable.setOnCheckedChangeListener(this);
            mRate.setOnItemSelectedListener(this);
        }

        public void enable(boolean en) {
            if(en)
                sm.registerListener(this, mSensor, rates[mRate.getSelectedItemPosition()]);
            else
                sm.unregisterListener(this);
        }

        public void onSensorChanged(SensorEvent e) {
            float[] values = e.values;
            int i;
            synchronized (this) {
                for(i = 0; i < 3; i++) {
                    mData[i].setText(String.format("%+8.6f", values[i]));
                    mData[i].postInvalidate();
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //Log.d(tag,"onAccuracyChanged: " + sensor.getName() + ", accuracy: " + accuracy);
            mStatus.setText(Integer.toString(accuracy));
            mStatus.postInvalidate();
        }

        public void onCheckedChanged(CompoundButton b, boolean v) {
            if(b == mEnable) {
                enable(mEnable.isChecked());
            }
        }

        public void setRate(int i) {
            if(mEnable.isChecked()) {
                sm.unregisterListener(this);
                sm.registerListener(this, mSensor, i);
            }
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            setRate(rates[pos]);
        }

        @Override
        public void onNothingSelected(AdapterView<?> arg0) {
            // nothing
        }
    }

    private void makeFoo(String name, Sensor s, 
                         int e_id, int n_id, 
                         int d0_id, int d1_id, int d2_id, 
                         int s_id, int st_id) {
        TextView n   = (TextView) findViewById(n_id);
        TextView d[] = new TextView[3];
        d[0] = (TextView)findViewById(d0_id);
        d[1] = (TextView)findViewById(d1_id);
        d[2] = (TextView)findViewById(d2_id);
        Spinner r    = (Spinner)findViewById(s_id);
        CheckBox e   = (CheckBox)findViewById(e_id);
        TextView st =  (TextView)findViewById(st_id);
        mSens.add(new SensorFoo(name, s, e, n, d, r, st));
    }
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        // get reference to SensorManager
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);

        // get reference to each sensor
        Sensor acc = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Sensor gy  = sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        Sensor mf  = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        Sensor or  = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        Sensor rv  = sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        Sensor la  = sm.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        Sensor gr  = sm.getDefaultSensor(Sensor.TYPE_GRAVITY);
        
        // initialize the visualizers
        makeFoo("Accel", acc, 
                R.id.CheckBox10, R.id.TextView10, R.id.TextView11, R.id.TextView12, R.id.TextView13, R.id.Spinner10, R.id.TextView14);
        makeFoo("Gyro", gy, 
                R.id.CheckBox20, R.id.TextView20, R.id.TextView21, R.id.TextView22, R.id.TextView23, R.id.Spinner20, R.id.TextView24);
        makeFoo("Mag Field", mf, 
                R.id.CheckBox30, R.id.TextView30, R.id.TextView31, R.id.TextView32, R.id.TextView33, R.id.Spinner30, R.id.TextView34);
        makeFoo("Orientation", or, 
                R.id.CheckBox40, R.id.TextView40, R.id.TextView41, R.id.TextView42, R.id.TextView43, R.id.Spinner40, R.id.TextView44);
        makeFoo("Rot. Vec.", rv, 
                R.id.CheckBox50, R.id.TextView50, R.id.TextView51, R.id.TextView52, R.id.TextView53, R.id.Spinner50, R.id.TextView54);
        makeFoo("Lin. Acc.", la,
                R.id.CheckBox60, R.id.TextView60, R.id.TextView61, R.id.TextView62, R.id.TextView63, R.id.Spinner60, R.id.TextView64);
        makeFoo("Gravity", gr, 
                R.id.CheckBox70, R.id.TextView70, R.id.TextView71, R.id.TextView72, R.id.TextView73, R.id.Spinner70, R.id.TextView74);
        
        // acquire wake lock - if necessary
        if (use_wakelock) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "SensorSimple");
            wl.acquire();
        }

        TableLayout topl = (TableLayout)findViewById(R.id.TableLayout10);
        topl.postInvalidate(); // force one redraw (after we set the names, etc)
    }

    @Override
    protected void onResume() {
        //re-register for all enabled sensors
        Iterator<SensorFoo> itr = mSens.iterator();
        //@TODO: this doesn't work as expected: the layout info is reloaded at resume, so all boxes are unchecked
        while(itr.hasNext()) { 
            SensorFoo s = itr.next();
            if(s.mEnable.isChecked())
                s.enable(true);
        }
        super.onResume();
    }

    public void onPause() {
        Iterator<SensorFoo> itr = mSens.iterator();
        while(itr.hasNext()) {
            sm.unregisterListener(itr.next());
        }
        super.onPause();
    }

    @Override
    protected void onStop() {
        if (use_wakelock) {
            wl.release();
        }
        super.onStop();
    }    
}
