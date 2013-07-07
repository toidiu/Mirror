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
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.toidiu.mirror.variables;

public class MainActivity extends Activity implements OnTouchListener{

	public Camera g_cam;
	private Viewer g_viewer;
	private MediaPlayer g_mp;
	private Music g_music = new Music(this, g_mp);
	private Shutter g_shut;
	private Pic_raw g_pic_raw;

	private int g_cam_id;
	private FrameLayout g_preview_layout;
	
	static private int g_inst_mode;
	static final private int g_inst_frz = 0;
	static final private int g_inst_rsm = 1;
	static final private int g_inst_save = 2;
	static final private int g_inst_norm = 3;

	private int mode;
	private static final int NORMAL = 0;
	private static final int FREEZE = 1;
	private static byte[] g_data;
	
	static final int CHANGE = 0;
	static final int NO_CHANGE = 1;	
	private int mode_change;
	
	private int old_sys_brt;
	private float old_scrn_brt;
	
	private PointF start = new PointF();
	private static final String TAG = "Touch";
	private final int main_move_threshold = 70;
	
	private static SharedPreferences settings;
	public static final String PREFS_NAME = "PrefsFile";
	
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
		// show the instructions everytime on default
		g_inst_mode = 0;
		
		// set up touch listeners
		main_touch_listener();
		
		// set up preferences
		settings = getSharedPreferences(PREFS_NAME, 0);
		
		pref_testing(); //TODO test code
		g_inst_mode = settings.getInt("first", 0);	//< first time show the instructions
		if(g_inst_norm != g_inst_mode) {
			g_inst_mode = g_inst_norm;
			main_instruction();
		}
	}

	private void pref_testing() {
		SharedPreferences.Editor editor = settings.edit();  
		editor.putInt("first", 0);
		// do tutorial
		editor.commit();
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
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		//g_music.m_start_music();
	}
	
	// restore brightness, stop music and release camera
	@Override
	protected void onPause() {
		super.onPause();
		
		// restore original brightness
    	Settings.System.putInt(getContentResolver(),
				Settings.System.SCREEN_BRIGHTNESS, old_sys_brt);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = old_scrn_brt;
		getWindow().setAttributes(lp);
		
		// pause the music and get current location
		//g_music.m_stop_music();
		
		// release views/camera
		main_release_camera(g_preview_layout, g_cam);
		
		SharedPreferences.Editor editor = settings.edit();  
		editor.putInt("first", g_inst_mode);
		// do tutorial
		editor.commit();
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		
		// stop the music
		//g_music.m_stop_music();
	}
	
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event)  {
		// back key
        if ( keyCode == KeyEvent.KEYCODE_BACK ) {
        	Log.d("BACK PRESS", "pressed the back button");
        	if(FREEZE == mode) {
        		main_back_button(keyCode, event);
        		return true;
        	} else {
        		return super.onKeyDown(keyCode, event);
        	}
        }
        
        // volume keys
        if ((keyCode == KeyEvent.KEYCODE_VOLUME_UP) 
        		|| (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)) {
            return super.onKeyUp(keyCode, event);
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

		case MotionEvent.ACTION_MOVE:
			double move_down = Math.abs(start.y) - Math.abs(event.getY());
			
			// MOVE DOWN: toggles freeze and normal
			if( (move_down < -main_move_threshold) && (NORMAL == mode) 
					&& (NO_CHANGE == mode_change)
					&& ((g_inst_frz == g_inst_mode) || (g_inst_norm == g_inst_mode)) ) {
				Pic_jpeg g_pic_jpeg = new Pic_jpeg(this);
				g_cam.takePicture(g_shut, g_pic_raw, g_pic_jpeg);
				g_data = g_pic_jpeg.pj_rtn_data();
				g_cam.stopPreview();
				mode = FREEZE;
				mode_change = CHANGE;
				if (g_inst_frz == g_inst_mode) {
					g_inst_mode++;
					main_instruction();
					Toast.makeText(this, "Freeze Mirror", Toast.LENGTH_SHORT).show();
				}
				
			} else if ( (move_down < -main_move_threshold) && (FREEZE == mode) 
					&& (NO_CHANGE == mode_change)
					&& ((g_inst_rsm == g_inst_mode) || (g_inst_norm == g_inst_mode)) ) {
				g_cam.startPreview();
				mode = NORMAL;
				mode_change = CHANGE;
				if (g_inst_rsm == g_inst_mode) {
					g_inst_mode++;
					main_instruction();
					Toast.makeText(this, "Resume Mirror", Toast.LENGTH_SHORT).show();
				}
			}
			
			// MOVE UP: resume preview
			if( (move_down > main_move_threshold) && (NORMAL == mode)
					&& ((g_inst_save == g_inst_mode) || (g_inst_norm == g_inst_mode)) ) {
				Pic_jpeg g_pic_jpeg = new Pic_jpeg(this);
				g_cam.takePicture(g_shut, g_pic_raw, g_pic_jpeg);
				g_data = g_pic_jpeg.pj_rtn_data();
				main_save_pic(g_data);
				if (g_inst_save == g_inst_mode) {
					g_inst_mode++;
					main_instruction();
				}
			} else if( (move_down > main_move_threshold) && (FREEZE == mode) ) {
				main_save_pic(g_data);
			}
			
			break;
			
		default:
			break;
		}
		
		return true;
	}
	
	
	private void main_save_pic(byte[] data) {
        FileOutputStream outStream = null;
        try {
            // generate the folder
            File imagesFolder = new File(Environment.getExternalStorageDirectory(), "MirrorMirror");
            if( !imagesFolder.exists() ) {
            	imagesFolder.mkdirs();
            }
            // generate new image name
            SimpleDateFormat formatter = new SimpleDateFormat("HH_mm_ss");
            Date now = new Date();
            String fileName = "image_" + formatter.format(now) + ".jpg";
            // create outstream and write data
            File image = new File(imagesFolder, fileName);
            outStream = new FileOutputStream(image);
            outStream.write(data);
            outStream.close();
            

            
            Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
        } catch (FileNotFoundException e) { // <10>
            //Toast.makeText(ctx, "Exception #2", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {}
        Log.d(TAG, "onPictureTaken - jpeg");
        Toast.makeText(this, "SAVED", Toast.LENGTH_SHORT).show();
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
			old_sys_brt = Settings.System.getInt(getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		
		old_scrn_brt = getWindow().getAttributes().screenBrightness;
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
	
	@SuppressWarnings("deprecation")
	private void main_instruction() {
		RelativeLayout lay;
		
		switch (g_inst_mode) {
		case 0:
			//Toast.makeText(this, "Lets swipe down to freeze the image.", Toast.LENGTH_LONG).show();
			lay = (RelativeLayout) findViewById(R.id.instruction);
			((ImageView)lay.findViewById(R.id.inst_arr)).setAlpha(0xFF);
			((TextView)lay.findViewById(R.id.inst_txt)).setText("Swipe Down");
			((TextView)lay.findViewById(R.id.inst_txt)).setTextColor(0xeeff0000);
			lay.setVisibility(View.VISIBLE);
			break;
		case 1:
			//Toast.makeText(this, "Lets swipe down again to resume the image.", Toast.LENGTH_LONG).show();
			lay = (RelativeLayout) findViewById(R.id.instruction);
			((ImageView)lay.findViewById(R.id.inst_arr)).setAlpha(0xFF);
			((TextView)lay.findViewById(R.id.inst_txt)).setText("Swipe Down");
			((TextView)lay.findViewById(R.id.inst_txt)).setTextColor(0xeeff0000);
			lay.setVisibility(View.VISIBLE);
			break;
		case 2:
			//Toast.makeText(this, "Finally lets swipe up to save that image.", Toast.LENGTH_LONG).show();
			lay = (RelativeLayout) findViewById(R.id.instruction);
			ImageView img_lay = (ImageView)findViewById(R.id.inst_arr);
			img_lay.setImageDrawable(getResources().getDrawable(R.drawable.arrow_up1));
			((TextView)lay.findViewById(R.id.inst_txt)).setText("Swipe Up");
			((TextView)lay.findViewById(R.id.inst_txt)).setTextColor(0xeeff0000);
			lay.setVisibility(View.VISIBLE);

			/*
			// rotation from 0 to 180 degrees here
			// ((ImageView)lay.findViewById(R.id.inst_arr)).setRotation(180); 
			RotateAnimation a = new RotateAnimation(0, 10, Animation.RELATIVE_TO_SELF, 340, Animation.RELATIVE_TO_SELF, 625);
			a.setFillAfter(true);
			a.setDuration(0);
			img_lay.startAnimation(a);
			*/			
			break;
		case 3:
			// the instructions were already done. set visibility to gone
			lay = (RelativeLayout) findViewById(R.id.instruction);
			lay.setVisibility(View.GONE);
			break;
		default:
			break;
		}

	}
	
	// dump event for touch
	@SuppressWarnings("deprecation")
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
