package com.toidiu.mirror;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

public class Pic_jpeg implements PictureCallback{
	
	private static final String TAG = "Picture_jpeg";
	private Context ctx;
	private static byte[] pj_data;
	
	public Pic_jpeg(Context p_context) {
		ctx = p_context;
	}
	
    public void onPictureTaken(byte[] data, Camera camera) {
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
            
            pj_data = data;
            
            Log.d(TAG, "onPictureTaken - wrote bytes: " + data.length);
        } catch (FileNotFoundException e) { // <10>
            //Toast.makeText(ctx, "Exception #2", Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {}
        Log.d(TAG, "onPictureTaken - jpeg");
        Toast.makeText(ctx, "SAVED", Toast.LENGTH_SHORT).show();
    }
    
    public byte[] pj_rtn_data() {
    	return pj_data;
    }
    
}
