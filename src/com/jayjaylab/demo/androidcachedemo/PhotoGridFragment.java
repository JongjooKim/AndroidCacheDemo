package com.jayjaylab.demo.androidcachedemo;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jayjaylab.android.cache.ImageLoader;
import com.jayjaylab.util.Log1;

import android.R.anim;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.GridView;
import android.widget.ImageView;

public class PhotoGridFragment extends Fragment implements GridView.OnScrollListener {
	private final String LOG_TAG = "PhotoGridFragment";
	private final String HTML_URL = 
			"http://www.gettyimagesgallery.com/collections/archive/slim-aarons.aspx";	
	
	private PhotoGridArrayAdapter adapter; 
	
	// Views
	private ViewGroup rootView;
	private GridView gridView;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		// starts loading image paths
		ImagePathLoadingTask task = new ImagePathLoadingTask(getActivity());
		task.execute(HTML_URL);
		
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		rootView = (ViewGroup)inflater.inflate(R.layout.photo_grid_fragment, container,
				false);		
		
		init(savedInstanceState);
		return rootView;
	}
	
	private void init(Bundle savedInstanceState) {		
		setViews();
	}
	
	private void setViews() {
		setGridView();
	}
	
	private void setGridView() {
		gridView = (GridView)rootView.findViewById(R.id.grid_view);
		adapter = new PhotoGridArrayAdapter(getActivity(), R.layout.grid_item_layout, null);		
		gridView.setAdapter(adapter);
		gridView.setOnScrollListener(this);
	}
	
	public ArrayList<String> getImagePaths(String url) {
		Log1.d(LOG_TAG, "getImagePaths() : url : " + url);		
		ArrayList<String> list = new ArrayList<String>();
		
		try {
			final String imgTagRegExpression = "<[Ii][Mm][Gg][^>]+[Ss][Rr][Cc]\\s*=\\s*['\"]([^'\"]+)['\"][^>]*>";
			final String srcTagRegExpression = "[Ss][Rr][Cc]\\s*=\\s*['\"](.*?)['\"]";
			URL oracle = new URL(url);
			BufferedReader in = new BufferedReader(new InputStreamReader(oracle.openStream()));
			String line;			
			Pattern imgTagPattern = Pattern.compile(imgTagRegExpression);
			Pattern srcTagPattern = Pattern.compile(srcTagRegExpression);
			Matcher imgTagmatcher = null;
			Matcher srcTagMatcher = null;
			String imageTag = null;
			
			while ((line = in.readLine()) != null) {			
				// finds <img tag/>
				imgTagmatcher = imgTagPattern.matcher(line);
				while(imgTagmatcher.find()) {
					imageTag = imgTagmatcher.group();
					srcTagMatcher = srcTagPattern.matcher(imageTag);
					while(srcTagMatcher.find()) {
						list.add(srcTagMatcher.group(1));
					}
				}								
			}
			
			in.close();
		} catch(Exception e) {
			Log1.e(LOG_TAG, "getImagePaths() : exception : " + e.getMessage());
			list.clear();
		}
		
		return list;
	}
	
	private class ImagePathLoadingTask extends AsyncTask<String, Void, ArrayList<String>> {
		private final String LOG_TAG = "ImagePathLoadingTask";
		private ProgressDialog progressDialog;
		
		public ImagePathLoadingTask(Context context) {
			progressDialog = new ProgressDialog(context);
			progressDialog.setTitle(R.string.loading_image_url);
		}
		
		protected void onPreExecute() {
			progressDialog.show();
		}
		
		@Override
		protected void onProgressUpdate(Void... values) {
			super.onProgressUpdate(values);
		}

		@Override
		protected ArrayList<String> doInBackground(String... urls) {			
			return getImagePaths(urls[0]);
		}

		@Override
		protected void onPostExecute(ArrayList<String> result) {
			if(progressDialog.isShowing()) {
				progressDialog.dismiss();
			}		
			
			adapter.setItems(result);
			
			super.onPostExecute(result);
		}		
	}

	@Override
	public void onScrollStateChanged(AbsListView view, int scrollState) {
		switch(scrollState) {
			case GridView.OnScrollListener.SCROLL_STATE_IDLE:
				Log1.d(LOG_TAG, "SCROLL_STATE_IDLE");				
				final int first = view.getFirstVisiblePosition();
				final int last = view.getLastVisiblePosition();				
				/*
				Log.d(LOG_TAG, "onScrollStateChanged() : first : " + first + 
						", last : " + last);
				*/
				ImageLoader.getInstance().cancelTaskExceptBetween(first, last);
				break;
			case GridView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
				Log1.d(LOG_TAG, "SCROLL_STATE_TOUCH_SCROLL");				
				break;
			case GridView.OnScrollListener.SCROLL_STATE_FLING:
				Log1.d(LOG_TAG, "SCROLL_STATE_FLING");				
				break;			
		}
	}

	@Override
	public void onScroll(AbsListView view, int firstVisibleItem,
			int visibleItemCount, int totalItemCount) {
		// TODO Auto-generated method stub		
	}	
}
