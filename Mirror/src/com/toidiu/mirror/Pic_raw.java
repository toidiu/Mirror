package com.toidiu.mirror;

import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.util.Log;

public class Pic_raw implements PictureCallback{

	private static final String TAG = "Picture_raw";
	
	@Override
	public void onPictureTaken(byte[] data, Camera camera) {
		// TODO Auto-generated method stub
		Log.d(TAG, "onPictureTaken - raw");
	}
}
