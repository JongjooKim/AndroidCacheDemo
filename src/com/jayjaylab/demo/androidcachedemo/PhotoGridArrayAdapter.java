package com.jayjaylab.demo.androidcachedemo;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.jayjaylab.android.cache.ImageLoader;
import com.jayjaylab.util.Log1;
import com.jayjaylab.util.cache.DiskLruCache;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class PhotoGridArrayAdapter extends ArrayAdapter<String> {
	private final String LOG_TAG = "PhotoGridArrayAdapter";
	private final String SITE_URL = "http://www.gettyimagesgallery.com";
	/*
	private final String DISCK_CACHE_PATH;
	private int WIDTH_LIMIT;
	private int hEIGHT_LIMIT;
	*/
	
	private ImageLoader imageLoader;
	private final int resourceId;
	
	private LayoutInflater inflater; 	
	private ArrayList<String> items;
	/*
	private LruCache<String, Bitmap> memoryCache;
	private DiskLruCache diskCache;
	private SparseArray<AsyncTask<String, Void, Bitmap>> diskToMemoryTaskArray; 
	private SparseArray<AsyncTask<String, Void, Void>> memoryToDiskTaskArray;
	private SparseArray<AsyncTask<String, Void, Bitmap>> networkToMemoryDiskTaskArray;
	*/

	public PhotoGridArrayAdapter(Context context, int resource,
			List<String> objects) {
		super(context, resource, objects);
		
		imageLoader = ImageLoader.getInstance();		
		resourceId = resource;		
		items = (ArrayList<String>)objects;
		inflater = (LayoutInflater)getContext().getSystemService(
				Context.LAYOUT_INFLATER_SERVICE);
	}
		
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {		
		if(!imageLoader.isImageSizeSet()) {
			imageLoader.setWidthHeight(
					parent.getWidth() / 2, parent.getHeight() / 4);
		}
		
		final ViewHolder holder;
		Log1.d(LOG_TAG, "getView() : position : " + position + ", convertView : " + convertView);
		
		if(convertView == null) {			
			convertView = inflater.inflate(resourceId, parent, false);
			holder = new ViewHolder();
			holder.parent = (ViewGroup)convertView.findViewById(R.id.parent);
			holder.thumbnailImageView = (ImageView)convertView.findViewById(R.id.imageview);
			holder.textview = (TextView)convertView.findViewById(R.id.textview);
			if(holder.parent != null) {
				holder.parent.setLayoutParams(new GridView.LayoutParams(
						parent.getWidth() / 2, parent.getHeight() / 4));
			}
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder)convertView.getTag();
		}
		
		String imagePath = items.get(position);
		if(imagePath != null) {			
			if(holder.thumbnailImageView != null) {				
				final String imageUrl = SITE_URL + imagePath.substring(imagePath.indexOf("/Images"));
				
				// first of all, cleans the imageview				
				holder.textview.setText(String.valueOf(position));
				imageLoader.displayImage(parent, 
						holder.thumbnailImageView, imageUrl, position);
			}
		}
		
		return convertView;
	}
	
	public void setItems(ArrayList<String> items) {
		this.items = items;		
		notifyDataSetChanged();
	}
	
	@Override
	public int getCount() {
		if(items == null)
			return 0;
		
		return items.size();
	}	
	
	private static class ViewHolder {
		public ViewGroup parent;
		public ImageView thumbnailImageView;
		public TextView textview;
		
		public ViewHolder() {}
	}
}
