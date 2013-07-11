package com.toidiu.mirror;

import android.content.Context;
import android.hardware.Camera.ShutterCallback;
import android.media.AudioManager;
import android.util.Log;

public class Shutter implements ShutterCallback {

	private static final String TAG = "ShutterCallback";
	
	private Context ctx;
	
	public Shutter(Context p_context) {
		ctx = p_context;
	}
	
	@Override
	public void onShutter() {
		Log.d(TAG, "onShutter'd");

	}

}
