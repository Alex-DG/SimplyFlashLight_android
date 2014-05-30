package com.project.simplyflashlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.util.FloatMath;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageButton;

import com.example.simplyflashlight.R;

public class MainActivity extends Activity implements SensorEventListener {
	
	ImageButton btnSwitch;

	private boolean isFlashOn;
	private boolean hasFlash;
	
	private float MOTION_THRESHOLD = 1.9f;

	private Camera camera;		
	private SensorManager sensorMan;
	private Sensor accelerometer;

	private float[] mGravity;
	private float mAccel;
	private float mAccelCurrent;
	private float mAccelLast;
	
	Parameters params;
	MediaPlayer mp;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		// Initialize flash switch button
		btnSwitch = (ImageButton) findViewById(R.id.btnSwitch);
		
		// Checking if flashlight is supporting or not
		hasFlash = getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
		checkingFlashLightSystemManagement(hasFlash);
		
		// Get the camera
		getCamera();
		
		// Displaying button image
		toggleButtonImage();
		
		// Get sensor
		sensorMan = (SensorManager)getSystemService(SENSOR_SERVICE);
		accelerometer = sensorMan.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		mAccel = 0.00f;
		mAccelCurrent = SensorManager.GRAVITY_EARTH;
		mAccelLast = SensorManager.GRAVITY_EARTH;
			
		// Switch button click event to toggle flash on/off
        btnSwitch.setOnClickListener(new View.OnClickListener() {
 
            @Override
            public void onClick(View v) {
                if (isFlashOn) {
                    // turn off flash
                    turnOffFlash();
                } else {
                    // turn on flash
                    turnOnFlash();
                }
            }
        });
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.activity_main_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}

	
	/*
	 * Check if device is supporting flashlight or not
	 */
	private void checkingFlashLightSystemManagement(boolean flash) {
		if (!hasFlash) {
			// device doesn't support flash
			// Show alert message and close the application			
			AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
			alert.setTitle("Error");
			alert.setMessage("Sorry, your device doesn't support flash light.");
			alert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
				
				@Override
				public void onClick(DialogInterface dialog, int which) {
					finish(); // Closing the application
					
				}
			});
		}		
	}

	/*
	 * Getting camera parameters
	 */
	private void getCamera() {
		if(camera == null) {
			try{
				camera = Camera.open();
				params = camera.getParameters();				
			}catch(RuntimeException ex) {
				Log.e("Camera Error. Failed to Open. Error: ", ex.getMessage());
			}
		}		
	}
	
	/*
	 * Turning on flash
	 */
	private void turnOnFlash() {
		if(!isFlashOn){
			if(camera == null || params == null){
				return;
			}
			
			// Play sound
			playSound();
			
			params = camera.getParameters();
			params.setFlashMode(Parameters.FLASH_MODE_TORCH);
			
			camera.setParameters(params);
			camera.startPreview();
			
			isFlashOn = true;
			
			// Changing button/switch image
			toggleButtonImage();			
		}
	}
	
	/*
	 * Turning Off flash
	 */
	private void turnOffFlash() {
		if(isFlashOn) {
			if(camera == null || params == null) {
				return;
			}
			
			// Play sound
			playSound();
			
			params = camera.getParameters();
	        params.setFlashMode(Parameters.FLASH_MODE_OFF);
	        
	        camera.setParameters(params);
	        camera.stopPreview();
	        
	        isFlashOn = false;
	         
	        // changing button/switch image
	        toggleButtonImage();
		}
	}
	
	/*
	 * Playing sound
	 * will play button toggle sound on flash on / off
	 */
	private void playSound(){
		if(isFlashOn){
			mp = MediaPlayer.create(MainActivity.this, R.raw.light_switch_off);
		}else{
			mp = MediaPlayer.create(MainActivity.this, R.raw.light_switch_on);
		}
		mp.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
            }
        }); 
		mp.start();
	}
	
	/*
	 * Toggle switch button images
	 * changing image states to on / off
	 */
	private void toggleButtonImage(){
		if(isFlashOn){
			btnSwitch.setImageResource(R.drawable.btn_switch_on);
		}else{
			btnSwitch.setImageResource(R.drawable.btn_switch_off);
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onPause() {
		super.onPause();
		
		// On pause turn off the flash
		turnOffFlash();
		
		// Unregister listener
		sensorMan.unregisterListener(this);
	}

	@Override
	protected void onRestart() {
		super.onRestart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		// On resume turn on the flash
		if(hasFlash)
			turnOffFlash();
		
		// register this class as a listener for the orientation and
	    // accelerometer sensors
		sensorMan.registerListener(this, accelerometer,
		        SensorManager.SENSOR_DELAY_UI);
	}

	@Override
	protected void onStart() {
		super.onStart();
		
		// On starting the app get the camera params
		getCamera();
	}

	@Override
	protected void onStop() {
		super.onStop();
		
		// On stop release the camera
		if (camera != null) {
			camera.release();
			camera = null;
		}
		
	}

	@Override
	public void onAccuracyChanged(Sensor arg0, int arg1) {
		// TODO Auto-generated method stub		
	}

	@Override
	  public void onSensorChanged(SensorEvent event) {
	    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
	      getMotionDetection(event);
	    }		
	  }	
	
	/*
	 * Motion detection to manage light
	 */
	private void  getMotionDetection(SensorEvent event) {
		if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
	        mGravity = event.values.clone();
	        
	        // Shake detection
	        float x = mGravity[0];
	        float y = mGravity[1];
	        float z = mGravity[2];
	       
	        mAccelLast = mAccelCurrent;
	        mAccelCurrent = FloatMath.sqrt(x*x + y*y + z*z);
	        float delta = mAccelCurrent - mAccelLast;
	        mAccel = mAccel * 0.9f + delta;
	        
	        // Make this higher or lower according to how much motion you want to detect
	        if(mAccel > MOTION_THRESHOLD) { 	        	
	        	if(!isFlashOn) {
		    		turnOnFlash(); //if light is off, turn on   		
		    	} else {
		    		turnOffFlash();
		    	}
	        }
	    }
	    
	}
	
}
