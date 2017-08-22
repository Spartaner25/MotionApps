package com.invensense.app.RollDiceSM;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.util.Set;
import com.invensense.android.hardware.sysapi.SysApi;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.hardware.Sensor;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.SensorEvent;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.Toast;
//import android.widget.AdapterView.AdapterContextMenuInfo;

public class RollDiceSM extends Activity implements SensorEventListener {
    private SensorManager mSensorManager; 
    private PowerManager mPowerManager;
    private WakeLock wl;
    //private boolean wl_released;
    private boolean[] use_sensors = new boolean[12];
    private long powerMask = 0x03FF;
    private int sensorRate = SensorManager.SENSOR_DELAY_GAME;
    private Map<Integer,Boolean> menuState = new HashMap<Integer,Boolean>();
    private List<Sensor> msensors_list;
    //private long sleep;
    //put sensors con
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        int i;
        super.onCreate(savedInstanceState);
        mGLView = new GLSurfaceView(this);
        mGLView.setRenderer(new MPURenderer(this));

        setContentView(mGLView);
        registerForContextMenu(mGLView);
        
        for(i = 0; i < 12; i++) {
            use_sensors[i] = false;
        }
        use_sensors[Sensor.TYPE_ROTATION_VECTOR] = true;

        Global.enterSelfTest = false;
        Global.mDisplay = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();

        if ((getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT 
               && (Global.mDisplay.getOrientation() == Surface.ROTATION_0 
               || Global.mDisplay.getOrientation() == Surface.ROTATION_180)) 
               ||(getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE 
               && (Global.mDisplay.getOrientation() == Surface.ROTATION_90 
               || Global.mDisplay.getOrientation() == Surface.ROTATION_270))) {
           setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
           Global.mPortrait = true;
        } else {
           setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
           Global.mPortrait = false;
        }    
    }
    
    public void onCreateContextMenu(ContextMenu menu, View v,
                                    ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.context_menu, menu);
      
      // I'm glad android's menu handling is so smart: 
      //     get the status of each item from our saved state
      Set s = menuState.entrySet();
      Iterator it = s.iterator();
      while(it.hasNext()) { 
          Map.Entry m = (Map.Entry)it.next();
          int key = (Integer)m.getKey();
          boolean value = (Boolean)m.getValue();
          MenuItem mi = menu.findItem(key);
          if(mi != null) {
              mi.setChecked(value);
          }
      }
    }
 
    public boolean onContextItemSelected(MenuItem item) {
        int result;

        //AdapterContextMenuInfo linfo = (AdapterContextMenuInfo) item.getMenuInfo();
        final int itemid = item.getItemId();
        boolean ckval;
        if(item.isCheckable()) {  //toggle checks -- hopefully this works for radios, too
            if (item.isChecked()) ckval =false;
            else ckval=true;
            item.setChecked(ckval);
            menuState.put(itemid, ckval);
        }
        switch (itemid) {
        case R.id.item04: // reset cal
            SysApi.resetCal();
            break;

        case R.id.item19:
            try {
                Thread.sleep(1500);
            } catch(InterruptedException e) {
                Log.i("Sleep", e.toString());
            }
            /*
            for (int cnt = 0;cnt < 1000; cnt++){
               Global.enterSelfTest = true;
               result = SysApi.selfTest(); 
               if (result == 0)
                   Log.d("RollDiceSM", "Calibration PASSED: " + cnt );
               else
                   Log.d("RollDiceSM", "Calibration FAILED: " + cnt );
               
               try {
                   Thread.sleep(2000);
                  } catch(InterruptedException e) {
                   Log.i("Sleep", e.toString());
                  }
            }
            */
            result = SysApi.selfTest();
            if (result == 0) 
                Toast.makeText(this, 
                               "Calibration PASSED: " + result, 
                               Toast.LENGTH_LONG).show();
            else 
                Toast.makeText(this, 
                               "Calibration FAILED: " + result, 
                               Toast.LENGTH_LONG).show();

            Global.enterSelfTest = false;
            break;

        case R.id.item21:
            try {
                Thread.sleep(500);
            } catch(InterruptedException e) {
                Log.i("Sleep", e.toString());
            }
            Global.enterSelfTest = true;
            Global.enterSelfTest = false;
            break;
            
        case R.id.item20:
            float[] f = new float[9];
            int i;
            for(i=0;i<9;i++) f[i] = 0.0f;
            SysApi.setBiases(f);
            break;
        case R.id.item12:// gy //************* sensor data buttons ******
            if (item.isChecked()) {
                use_sensors[Sensor.TYPE_GYROSCOPE] = true;
            } else
                use_sensors[Sensor.TYPE_GYROSCOPE] = false;
            setupListener(Sensor.TYPE_GYROSCOPE);
            break;
        case R.id.item13:// a
            if (item.isChecked()) {
                use_sensors[Sensor.TYPE_ACCELEROMETER] = true;
            } else
                use_sensors[Sensor.TYPE_ACCELEROMETER] = false;
            setupListener(Sensor.TYPE_ACCELEROMETER);
            break;
        case R.id.item14:// m
            if (item.isChecked()) {
                use_sensors[Sensor.TYPE_MAGNETIC_FIELD] = true;
            } else
                use_sensors[Sensor.TYPE_MAGNETIC_FIELD] = false;
            setupListener(Sensor.TYPE_MAGNETIC_FIELD);
            break;
        case R.id.item15:// or
            if (item.isChecked()) {
                use_sensors[Sensor.TYPE_ORIENTATION] = true;
            }else
                use_sensors[Sensor.TYPE_ORIENTATION] = false;
            setupListener(Sensor.TYPE_ORIENTATION);
            break;
        case R.id.item16:// rv
            if (item.isChecked()) {
                use_sensors[Sensor.TYPE_ROTATION_VECTOR] = true;
            }else
                use_sensors[Sensor.TYPE_ROTATION_VECTOR] = false;
            setupListener(Sensor.TYPE_ROTATION_VECTOR);
            break;
        case R.id.item17:// la
            if (item.isChecked()) {
                use_sensors[Sensor.TYPE_LINEAR_ACCELERATION] = true;
            }else
                use_sensors[Sensor.TYPE_LINEAR_ACCELERATION] = false;
            setupListener(Sensor.TYPE_LINEAR_ACCELERATION);
            break;
        case R.id.item18:// gr
            if (item.isChecked()) {
                use_sensors[Sensor.TYPE_GRAVITY] = true;
            }else
                use_sensors[Sensor.TYPE_GRAVITY] = false;
            setupListener(Sensor.TYPE_GRAVITY);
            break;
        case R.id.item05: // gyro power // ******** power control items *******
            if(item.isChecked()){
                powerMask |= 0x000F;
            } else
                powerMask &= ~(0x000F);
            SysApi.setSensors(powerMask);
            break;
        case R.id.item06: //accel power 
            if(item.isChecked()){
                powerMask |= 0x0070;
            } else
                powerMask &= ~(0x0070);
            SysApi.setSensors(powerMask);
            break;
        case R.id.item07: //compass power 
            if(item.isChecked()){
                powerMask |= 0x0380;
            } else
                powerMask &= ~(0x0380);
            SysApi.setSensors(powerMask);
            break;
            
        //*************** update rates **************************
        case R.id.item08: // fastest
            if (item.isChecked()) {
                Log.d("RollDiceSM", "SensorManager.SENSOR_DELAY_FASTEST");
                setupListener(-1);
            }
            break;
        case R.id.item09: // game
            if (item.isChecked()) {
                sensorRate = SensorManager.SENSOR_DELAY_GAME;
                setupListener(-1);
            }
            break;
        case R.id.item10:
            if (item.isChecked()) {
                sensorRate = SensorManager.SENSOR_DELAY_UI;
                setupListener(-1);
            }
            break;
        case R.id.item11:
            if (item.isChecked()) {
                sensorRate = SensorManager.SENSOR_DELAY_NORMAL;
                setupListener(-1);
            }
            break;
        case R.id.item1:
            SysApi.setLocalMagField(22.936489f, -5.749921f, -42.702049f);  //sunnyvale mag field
            break;
        default:
            return super.onContextItemSelected(item);
        }
        return true;
    }
    
    @Override
    protected void onResume() {
        //int i;
        super.onResume();
        mPowerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wl = mPowerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "rolldice");
        wl.acquire();
        //wl_released = false;
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        msensors_list = mSensorManager.getSensorList(Sensor.TYPE_ALL);;
        
        Log.d("RollDiceSM", "accel :" + Sensor.TYPE_ACCELEROMETER + " " +(Sensor.TYPE_ACCELEROMETER ==1));
        Log.d("RollDiceSM", "GRYO :" + Sensor.TYPE_GYROSCOPE+ " " +(Sensor.TYPE_GYROSCOPE ==4));
        Log.d("RollDiceSM", "MAG :" + Sensor.TYPE_MAGNETIC_FIELD+ " " +(Sensor.TYPE_MAGNETIC_FIELD ==2));
        Log.d("RollDiceSM", "GRAV :" + Sensor.TYPE_GRAVITY+ " " +(Sensor.TYPE_GRAVITY ==9));
        Log.d("RollDiceSM", "la :" + Sensor.TYPE_LINEAR_ACCELERATION+ " " +(Sensor.TYPE_LINEAR_ACCELERATION ==10));
        Log.d("RollDiceSM", "RV :" + Sensor.TYPE_ROTATION_VECTOR+ " " +(Sensor.TYPE_ROTATION_VECTOR ==11));
        
        setupListener(Sensor.TYPE_ROTATION_VECTOR);
        
        mGLView.onResume();
        
    }

    @Override
    protected void onStop() {
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
        }
        
        super.onStop();
    }
    
       @Override
        public boolean onKeyDown(int keyCode, KeyEvent msg) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                Log.i("RollDiceSM", "Exit...");
                if(mSensorManager != null) {
                    mSensorManager.unregisterListener(this);
                    mSensorManager = null;
                }
                this.setResult(RESULT_OK);
                android.os.Process.killProcess(android.os.Process.myPid());
            }

            return true;
        }
       
        @Override
        protected void onDestroy() {
            Log.i("RollDiceSM", "Destroy main activity.");

            if(mSensorManager != null) {
                mSensorManager.unregisterListener(this);
                mSensorManager = null;
            }
            
            mGLView.mGLThread.requestExitAndWait();

            System.gc();
            System.runFinalization();
            System.exit(0);

            super.onDestroy();
        }
        
        
    @Override
    protected void onPause() {
        
        if(wl.isHeld()) {
            wl.release();
        }
        
        if(mSensorManager != null) {
            mSensorManager.unregisterListener(this);
            mSensorManager = null;
        }
        System.gc();
        mGLView.onPause();
        
        try { Thread.sleep(2); } catch(Exception e) {}
        
        super.onPause();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //Log.i("RollDiceSM", "Sensor accuracy change:" + sensor.getName() + " " + accuracy);
    }

    public void onSensorChanged(SensorEvent e) {
        String sensor_out = "";
        String format = "%+13.5f %+13.5f %+13.5f";
        if(e.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR && 
           !Global.enterSelfTest) {
            float quat[] = new float[4];
            synchronized(Global.q) {
                SensorManager.getQuaternionFromVector(quat, e.values);
                Global.q[0] = quat[0];
                Global.q[1] = quat[1];
                Global.q[2] = quat[2];
                Global.q[3] = quat[3];
            }
            String format_final = "Quaternion   : " + format + "%+13.5f (CRC %+.5f)" + " Vendor %s";
            sensor_out = String.format(format_final, 
                                       quat[0], quat[1], quat[2], quat[3],
                                       Math.sqrt(
                                           quat[0] * quat[0] + quat[1] * quat[1] + 
                                           quat[2] * quat[2] + quat[3] * quat[3]),
                                       e.sensor.getVendor());
            Log.d("RollDiceSM", sensor_out);
        }
        
        if(e.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            sensor_out = String.format("Accelerom.   : " + format, 
                                       e.values[0], e.values[1], e.values[2]);
            Log.d("RollDiceSM", sensor_out);
        }
        if(e.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            sensor_out = String.format("Gyroscope    : " + format, 
                                       e.values[0], e.values[1], e.values[2]);
            Log.d("RollDiceSM", sensor_out);
        }
        if(e.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            sensor_out = String.format("Magnetometer : " + format + " [%s]", 
                                       e.values[0], e.values[1], e.values[2],
                                       e.sensor.getName());
            Log.d("RollDiceSM", sensor_out);
        }
        if(e.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            sensor_out = String.format(format = "Linear accel : " + format + " [%s]", 
                                       e.values[0], e.values[1], e.values[2],
                                       e.sensor.getName());
            Log.d("RollDiceSM", sensor_out);
            //"L : " +  e.values[0] + " " + e.values[1] + " " + e.values[2] + " [" + e.sensor.getName()+"]");
        }
        if(e.sensor.getType() == Sensor.TYPE_GRAVITY) {
            sensor_out = String.format("Gravity      : " + format + " [%s]", 
                                       e.values[0], e.values[1], e.values[2],
                                       e.sensor.getName());
            Log.d("RollDiceSM", sensor_out);
        }
        if(e.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            sensor_out = String.format("Orientation  : " + format + " [%s]", 
                                       e.values[0], e.values[1], e.values[2],
                                       e.sensor.getName());
            Log.d("RollDiceSM", sensor_out);
        }
        //if(e.sensor.getType() == Sensor.TYPE_ROTATION_MATRIX) {
        //    float h = (float)((double)Math.atan2(e.values[4], e.values[1])*57.29577951308 - 90.0f);
        //    Log.d("RollDiceSM", "Heading: " + h);
        //}
        sensor_out = String.format("Accuracy     : %13d", e.accuracy);
        Log.d("RollDiceSM", sensor_out);
        
    }

    private void setupListener(int sentype) {
        int i;
        Sensor tsen = null;
        long start, stop;
        String out = "";

        if (sentype == -1) { // update all the rates
            if (msensors_list.size() > 0) {
                for (i = 0; i < msensors_list.size(); i++) {
                    tsen = msensors_list.get(i);
                    if (use_sensors[tsen.getType()] == true 
                          && tsen.getVendor().equals("Invensense")) {
                        Log.d("RollDiceSM", tsen.getName() + " vendor \"" + tsen.getVendor() + "\"");
                        Log.d("RollDiceSM", "RegisterListener -- update all");
                        start = System.currentTimeMillis();
                        mSensorManager.registerListener(this, tsen, sensorRate);
                        stop = System.currentTimeMillis();
                        out = String.format("updated rate %d (%d msec)", sensorRate, stop - start);
                        Log.d("RollDiceSM", out);
                    }
                    /*
                    switch (tsen.getType()) {
                    case Sensor.TYPE_ACCELEROMETER:
                    case Sensor.TYPE_MAGNETIC_FIELD:
                    case Sensor.TYPE_LINEAR_ACCELERATION:
                    case Sensor.TYPE_GRAVITY:
                    case Sensor.TYPE_GYROSCOPE:
                    case Sensor.TYPE_ROTATION_VECTOR:
                        if (use_sensors[tsen.getType()] == true 
                              && tsen.getVendor() == "Invensense") {
                            mSensorManager.unregisterListener(this);
                            Log.d("RollDiceSM", "RegisterListener -- update all");
                            start = System.currentTimeMillis();
                            mSensorManager.registerListener(this, tsen, sensorRate);
                            stop = System.currentTimeMillis();
                            out = String.format("updated rate %d (%d msec)", sensorRate, stop - start);
                            Log.d("RollDiceSM", out);
                        }
                    }
                    */
                }
            }
            return;
        }

        // activate or de-activate a single sensor
        if (msensors_list.size() > 0) {
            for (i = 0; i < msensors_list.size(); i++) {
                tsen = msensors_list.get(i);
                if (tsen.getType() == sentype)
                    break;
            }
        }

        if (use_sensors[sentype] == true) {
            Log.d("RollDiceSM", "RegisterListener -- update one");
            start = System.currentTimeMillis();
            mSensorManager.registerListener(this, tsen, sensorRate);
            stop = System.currentTimeMillis();
            out = String.format("registered sensor rate %d (%d msec)", sensorRate, stop - start);
            Log.d("RollDiceSM", "registered sensor " + sentype + " "
                    + tsen.getName());
            Log.d("RollDiceSM", out);
            
        } else {
            mSensorManager.unregisterListener(this, tsen);
        }

    }
    
    private GLSurfaceView mGLView;
}
