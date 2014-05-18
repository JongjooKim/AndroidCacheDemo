package com.jayjaylab.demo.androidcachedemo;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayjaylab.android.cache.ImageLoader;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

public class MainActivity extends FragmentActivity {
	private final String LOG_TAG = "MainActivity";	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		this.overridePendingTransition(R.anim.start_enter, R.anim.start_exit);
		setContentView(R.layout.activity_main);

		init(savedInstanceState);
	}
	
	private void init(Bundle savedInstanceState) {
		setPhotoGridFragment(savedInstanceState);
		setImageLoader();
	}
	
	private void setPhotoGridFragment(Bundle savedInstanceState) {
		if (savedInstanceState == null) {
			FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();			
			transaction.add(R.id.container, new PhotoGridFragment(), "photo_grid");
			transaction.commit();			
		} 
	}
	
	private void setImageLoader() {
		ImageLoader.getInstance().init(getApplicationContext(), getExternalCacheDir().
				getAbsolutePath() + File.separator + "images" + File.separator);
	}
	
	@Override
	public void finish() {
		super.finish();
		this.overridePendingTransition(R.anim.end_enter, R.anim.end_exit);
	}
}
