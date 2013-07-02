package com.toidiu.mirror;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnTouchListener{

	private Camera g_cam;
	private Viewer g_viewer;
	private Shutter g_shut;
	private Pic_raw g_pic_raw;
	private Pic_jpeg g_pic_jpeg = new Pic_jpeg(this);
	private int g_cam_id;
	private FrameLayout g_preview_layout;
	private int old_sys_brightness;
	private float old_scrn_brightness;
	private MediaPlayer mp;
	private int mp_position;
	private PointF start = new PointF();
	private static final String TAG = "Touch";
	private final int main_move_threshold = 70;
	static final int NORMAL = 0;
	static final int FREEZE = 1;
	private int mode;
	static final int CHANGE = 0;
	static final int NO_CHANGE = 1;	
	private int mode_change;
	
	// gets camera id. creates a layout and clears it. 
	// calls a function to create viewer and add it to the layout
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.layout2);

		g_cam_id = 100;
		mode = NORMAL;
		mode_change = NO_CHANGE;
		
		// set up touch listeners
		main_touch_listener();
		
		// set up preferences
		main_pref();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	        
		// get original brightness
		main_get_orig_brightness();
		
		// find an instance of front camera
		g_cam_id = main_find_frnt_cam();

		// create the layout for the viewer and clear it
		g_preview_layout = (FrameLayout) findViewById(R.id.camera_preview);
		g_preview_layout.removeAllViews();

		main_start_camera_layout(g_preview_layout, g_cam_id);
		
		// start music
		//main_start_music();
	}
	
	// restore brightness, stop music and release camera
	@Override
	protected void onPause() {
		super.onPause();
		
		// restore original brightness
    	Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS, old_sys_brightness);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = old_scrn_brightness;
		getWindow().setAttributes(lp);
		
		// pause the music and get current location
		//main_stop_music();
		
		// release views/camera
		main_release_camera(g_preview_layout, g_cam);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		// stop the music
		//main_stop_music();
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
        if ( keyCode == KeyEvent.KEYCODE_BACK ) {
        	Log.d("BACK PRESS", "pressed the back button");
        	if(FREEZE == mode) {
        		main_back_button(keyCode, event);
        		return true;
        	} else {
        		return super.onKeyDown(keyCode, event);
        	}
        }
        
        // back button was not pressed
        return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onTouch(View v, MotionEvent event) {
		
		main_dump_event(event);
		
		switch (event.getAction() /*& MotionEvent.ACTION_MASK*/) {
		case MotionEvent.ACTION_DOWN:
			// handle action down
			start.set(event.getX(), event.getY());

			mode_change = NO_CHANGE;
			break;

		case MotionEvent.ACTION_UP:
			// handle action up
			break;

		case MotionEvent.ACTION_MOVE:
			double move_down = Math.abs(start.y) - Math.abs(event.getY());
			
			// move is down toggles freeze and normal
			if( (move_down < -main_move_threshold) && (NORMAL == mode) 
					&& (NO_CHANGE == mode_change) ) {
				g_cam.stopPreview();
				mode = FREEZE;
				mode_change = CHANGE;
				main_inst_on();
			} else if ( (move_down < -main_move_threshold) && (FREEZE == mode) 
					&& (NO_CHANGE == mode_change) ) {
				g_cam.startPreview();
				mode = NORMAL;
				mode_change = CHANGE;
				main_inst_off();
			}
			
			// resume preview if move is up
			if( (move_down > main_move_threshold) && (NORMAL == mode) ) {
				g_cam.takePicture(g_shut, g_pic_raw, g_pic_jpeg);
			} else if( (move_down > main_move_threshold) && (FREEZE == mode) ) {
				
			}
			
			break;
			
		case MotionEvent.ACTION_POINTER_DOWN:
			// handle action 
			break;
			
		default:
			break;
		}
		
		return true;
	}

//*********************************************************************
//**********************end of override functions*********************
//*********************************************************************

	// Check if this device has a front camera. (returns a bool)
	private boolean main_check_cam_avail(Context context) {
	    if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)){
	        // this device has a camera
	        return true;
	    } else {
	        // no camera on this device
	        return false;
	    }
	}
	
	// iterate through camera ids to find front camera. returns id.
	// sets brightness to 100% if camera id is found.
	private int main_find_frnt_cam() {
		int cam_id = -1;
		int num_cam = Camera.getNumberOfCameras();
		
		// iterate through all cam ids to find front facing one
		for (cam_id = 0; cam_id < num_cam; cam_id++) {
			CameraInfo info = new CameraInfo();
			Camera.getCameraInfo(cam_id, info);
			if (CameraInfo.CAMERA_FACING_FRONT == info.facing) {
				main_set_brightness();
				break;
			}
		}
		
		return cam_id;
	}
	
	// opens an instance of the front camera. Creates an instance of the viewer.
	// sets the viewer instance to the layout.
	private void main_start_camera_layout(FrameLayout layout, int cam_id) {
		if ( true == main_check_cam_avail(this) ) {
			
			try {
				// open the camera (the id is supposed to be the front camera)
				//g_cam.release();
				g_cam = null;
				g_cam = Camera.open(cam_id);
			} catch(Exception e) {
				
			}
			if (null != g_cam) {
				// if the camera instance opens then open an instance of the viewer
				// and add the viewer to the layout.
//				if(g_viewer == null) {
					g_viewer = new Viewer(this, g_cam);
//				}
				layout.addView(g_viewer);
			}
		}
		
	}

    private void main_set_brightness() {
		Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
		Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS, 255);
		
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = 1F;
		getWindow().setAttributes(lp);
		
    }
    
    // get original system brightness and screen brightness
    private void main_get_orig_brightness() {
		try {
			old_sys_brightness = Settings.System.getInt(getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		
		old_scrn_brightness = getWindow().getAttributes().screenBrightness;
    }
    
    // start music
	private void main_start_music() {
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		int music;
		music = R.raw.i_feel_pretty;
		
		if(mp != null) { mp.release(); }
		
		mp = MediaPlayer.create(this, music);
		mp.setLooping(true);
		mp.start();

/*		if(main_is_restored == true) {// TODO
			mp.seekTo(mp_position);

	
		}
*/

	}
	
	// stop music
	private void main_stop_music() {
		mp.stop();
		mp.release();
		mp = null;
	}

	// pause music
	private void main_pause_music() {
		mp_position = mp.getCurrentPosition();
		mp.pause();
	}
	
	// release camera and layout
	private void main_release_camera(FrameLayout layout, Camera cam ) {
		// release views
		layout.removeAllViews();
		
		// release the camera
		if (null != cam) {
			cam.release();
			cam = null;
		}

	}

	// enable gesture and gesture listeners
	private void main_touch_listener() {
		View cam_view = findViewById(R.id.cam_frame);
		cam_view.setOnTouchListener(this);
	}
	
	// back button
	private void main_back_button(int keyCode, KeyEvent event) {
		g_cam.startPreview();
		mode = NORMAL;
	}
	
	private void main_inst_on() {
		RelativeLayout lay = (RelativeLayout) findViewById(R.id.instruction);
		((ImageView)lay.findViewById(R.id.inst_arr)).setAlpha(0x9F);
		((TextView)lay.findViewById(R.id.inst_txt)).setTextColor(0x9F000000);
		lay.setVisibility(View.VISIBLE);
	}
	
	private void main_inst_off() {
		RelativeLayout lay = (RelativeLayout) findViewById(R.id.instruction);
		lay.setVisibility(View.GONE);
	}
	
	private void main_pref() {
		SharedPreferences settings= getSharedPreferences(getResources().getString(R.string.MyPrefsFile), 0);
		boolean first_run= settings.getBoolean("first", true);

		if(first_run){
		///show instruction
		SharedPreferences.Editor editor = settings.edit();  
		editor.putBoolean("first", false);
		// do tutorial
		editor.commit();
		}
	}
	
	// dump event for touch
	private void main_dump_event(MotionEvent event) {
		String names[] = { "DOWN", "UP", "MOVE", "CANCEL", "OUTSIDE",
				"POINTER_DOWN", "POINTER_UP", "7?", "8?", "9?" };
		StringBuilder sb = new StringBuilder();
		int action = event.getAction();
		int actionCode = action & MotionEvent.ACTION_MASK;
		sb.append("event Action_").append(names[actionCode]);
		
		if( (actionCode == MotionEvent.ACTION_POINTER_DOWN) ||
				(actionCode == MotionEvent.ACTION_POINTER_UP) ) {
			sb.append("(pid ").append(action >> MotionEvent.ACTION_POINTER_ID_SHIFT);
			sb.append(")");
		}
		sb.append("[");
		for(int i=0; i < event.getPointerCount(); i++) {
			sb.append("#").append(i);
			sb.append("(pid ").append(event.getPointerId(i));
			sb.append(")=").append((int) event.getX(i));
			sb.append(",").append((int) event.getY(i));
			if(i+1 < event.getPointerCount())
				sb.append(";");
		}
		sb.append("]");
		Log.d(TAG, sb.toString());
	}

}
