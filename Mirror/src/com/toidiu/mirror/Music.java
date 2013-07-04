package com.toidiu.mirror;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;

public class Music {

	private MediaPlayer m_mp;
	private int mp_position;
	private Context m_ctx;
	
	public Music (Context ctx, MediaPlayer mp) {
		m_mp = mp;
		m_ctx = ctx;
	}
	
    // start music
	public void main_start_music() {

		int music;
		music = R.raw.i_feel_pretty;
		
		if(m_mp != null) { m_mp.release(); }
		
		m_mp = MediaPlayer.create(m_ctx, music);
		m_mp.setLooping(true);
		m_mp.start();
	}
	
	// stop music
	public void main_stop_music() {
		m_mp.stop();
		m_mp.release();
		m_mp = null;
	}

	// pause music
	public void main_pause_music() {
		mp_position = m_mp.getCurrentPosition();
		m_mp.pause();
	}
}
