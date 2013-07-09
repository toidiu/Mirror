package com.toidiu.mirror;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class Pic_jpeg implements PictureCallback{
	
	private static final String TAG = "Picture_jpeg";
	private Context ctx;
	
	public Pic_jpeg(Context p_context) {
		ctx = p_context;
	}
	
    @SuppressLint("SimpleDateFormat")
	public void onPictureTaken(byte[] data, Camera camera) {
        FileOutputStream outStream = null;
        try {
            // generate the folder
            File imagesFolder = new File(Environment.getExternalStorageDirectory(), "MirrorMirror");
            if( !imagesFolder.exists() ) {
            	imagesFolder.mkdirs();
            }
            String file_path = imagesFolder.getPath();
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
            Toast.makeText(ctx, "SAVED at: " + file_path, Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) { // <10>
            //Toast.makeText(ctx, "Exception #2", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {}
        
        camera.startPreview();
    }
    
}
