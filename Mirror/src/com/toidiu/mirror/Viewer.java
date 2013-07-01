package com.toidiu.mirror;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.hardware.Camera;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

public class Viewer extends SurfaceView implements SurfaceHolder.Callback {

	// global variables
	private SurfaceHolder g_viewer_holder;			//< creates a surface holder
    private Camera g_viewer_cam;					//< creates a camera instance
    private static final String TAG = "Viewer";		//< used for error message

    // set camera passed into viewer. add a callback for the viewer.
    @SuppressWarnings("deprecation")
	public Viewer(Context context, Camera cam) {
        super(context);
        g_viewer_cam = cam;

        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        g_viewer_holder = getHolder();
        g_viewer_holder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        g_viewer_holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }

    // set the camera as to the surface holder. start the preview for the camera.
    public void surfaceCreated(SurfaceHolder holder) {
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
        	g_viewer_cam.setPreviewDisplay(holder);
        	g_viewer_cam.startPreview();
        	
        	
        } catch (IOException e) {
            Log.d(TAG, "error in surfaceCreated: " + e.getMessage());
        }
    }

    // empty
    public void surfaceDestroyed(SurfaceHolder holder) {
        // empty. Take care of releasing the Camera preview in your activity.
        g_viewer_cam.stopPreview();
    }

    // stop preview. set preview. start the preview. rotate.
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {

        // return if preview surface does not exist
        if (null == holder.getSurface()) {
        	return;
        }

        // stop preview before making changes
        try {
        	g_viewer_cam.stopPreview();
        } catch (Exception e){
          // ignore: tried to stop a non-existent preview
        }
        
        
//        Display display = ((WindowManager)getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        viewer_size_rotate();
        
        // start preview with new settings
        try {

        	g_viewer_cam.setPreviewDisplay(g_viewer_holder);
        	g_viewer_cam.startPreview();
        	
        } catch (Exception e){
            Log.d(TAG, "Error in surface changed: " + e.getMessage());
        }
    }
    
    
//*********************************************************************
//**********************end of system functions************************
//*********************************************************************
    
    public void viewer_size_rotate() {
    	Display display = ((WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
    	
        Camera.Parameters param = g_viewer_cam.getParameters();
        List<Camera.Size> previewSizes = param.getSupportedPreviewSizes();
        Camera.Size previewSize = previewSizes.get(0);
    	
    	switch (display.getRotation()) {
		case Surface.ROTATION_0:
	        param.setPreviewSize(previewSize.width, previewSize.height);
	        g_viewer_cam.setDisplayOrientation(90);
			break;
		case Surface.ROTATION_90:
	        param.setPreviewSize(previewSize.width, previewSize.height);
			break;
		case Surface.ROTATION_180:
	        param.setPreviewSize(previewSize.width, previewSize.height);
			break;
		case Surface.ROTATION_270:
	        param.setPreviewSize(previewSize.width, previewSize.height);
	        g_viewer_cam.setDisplayOrientation(180);
			break;

		default:
			break;
		}
    	
    	 try {
    		 g_viewer_cam.setParameters(param);
         } catch (Exception e){
             Log.d(TAG, "Error in viewer size rotate: " + e.getMessage());
         }
		
    	
    }

}