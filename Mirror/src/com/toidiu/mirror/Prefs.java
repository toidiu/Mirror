package com.toidiu.mirror;

import android.content.Context;
import android.content.SharedPreferences;

public class Prefs {

	public static void app_launched(Context mContext) {
		SharedPreferences p_prefs = mContext.getSharedPreferences("apprater", 0);
        //if (p_prefs.getBoolean("dontshowagain", false)) { return ; }
        SharedPreferences.Editor editor = p_prefs.edit();
        
        
        change(mContext, editor);
        
        editor.commit();
	}
	
	public static void change(Context mContext, 
			final SharedPreferences.Editor editor) {
		if (editor != null) {
            editor.putBoolean("dontshowagain", true);
            editor.commit();
        }
	}
}
