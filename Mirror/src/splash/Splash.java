package splash;

import com.toidiu.mirror.MainActivity;
import com.toidiu.mirror.R;
import com.toidiu.mirror.R.layout;
//import com.toidiu.mirror.R.layout;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.WindowManager;

public class Splash extends Activity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		//display splash view
		setContentView(R.layout.splash);
		
		//wait 4 seconds
		final Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
            public void run() {
                // Do something after 2s
				startMirror();
            }
		}, 2000);
		
	}

	//switch to main activity
	private void startMirror(){
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}
	
}
