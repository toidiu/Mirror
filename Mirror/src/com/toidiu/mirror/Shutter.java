package com.toidiu.mirror;

import android.hardware.Camera.ShutterCallback;
import android.util.Log;

public class Shutter implements ShutterCallback {

	private static final String TAG = "ShutterCallback";
	
	@Override
	public void onShutter() {
		// TODO Auto-generated method stub
		Log.d(TAG, "onShutter'd");
	}

}
