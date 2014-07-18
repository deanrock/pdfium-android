package com.kirrupt.pdfiumandroid;

import java.nio.ByteBuffer;
import java.util.Timer;
import java.util.TimerTask;

import android.support.v7.app.ActionBarActivity;

import android.support.v7.app.ActionBar;
import android.support.v4.app.Fragment;
import android.app.ActivityManager;
import android.app.ActivityManager.MemoryInfo;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Bitmap.Config;
import android.os.Bundle;
import android.os.Debug;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import com.kirrupt.pdfiumandroid.MyView1;
import com.kirrupt.pdfiumandroid.JNIWrapper;
import com.kirrupt.pdfiumandroid.PDFReader;

public class MainActivity extends ActionBarActivity {

	public static MyView1 view;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			getSupportFragmentManager().beginTransaction()
					.add(R.id.container, new PlaceholderFragment()).commit();
		}
		
		Timer myTimer = new Timer();
	    myTimer.scheduleAtFixedRate(new TimerTask() {
	        @Override
	        public void run() {
	        	MemoryInfo mi = new MemoryInfo();
	    		ActivityManager activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
	    		activityManager.getMemoryInfo(mi);
	    		long availableMegs = mi.availMem / 1048576L;
	    		long used = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
	    		used /= 1048576L;
	    		
	    		long x = Debug.getNativeHeapAllocatedSize() / 1048576L;
	    		
	    		Log.d("memory", "used: "+String.valueOf(used)+", native: "+String.valueOf(x)+", available: "+String.valueOf(availableMegs));
	    		//System.gc();
	        }

	    }, 0, 500);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}
		
		public static int unsignedToBytes(byte b) {
		    return b & 0xFF;
		  }

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			
			view = (MyView1)rootView.findViewById(R.id.view);
			
			JNIWrapper.helloLog("hey!");
			
			
			
			return rootView;
		}
	}

}
