package com.jayjaylab.android.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.net.URL;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.util.SparseArray;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.jayjaylab.demo.androidcachedemo.R;
import com.jayjaylab.util.Log1;
import com.jayjaylab.util.cache.DiskLruCache;

public class ImageLoader {
	private final String LOG_TAG = "ImageLoader";
	private static ImageLoader instance;
	private String DISCK_CACHE_PATH;
	private int WIDTH_LIMIT;
	private int hEIGHT_LIMIT;
	
	
	// private WeakReference<Context> context;
	private LruCache<String, Bitmap> memoryCache;
	private DiskLruCache diskCache;
	private SparseArray<AsyncTask<String, Void, Bitmap>> diskToMemoryTaskArray; 
	private SparseArray<AsyncTask<String, Void, Void>> memoryToDiskTaskArray;
	private SparseArray<AsyncTask<String, Void, Bitmap>> networkToMemoryDiskTaskArray;
	private Animation imageEnterAnimation;
	
	public static ImageLoader getInstance() {
		if(instance == null) {			
			instance = new ImageLoader();			
		}
		
		return instance;
	}
	
	private ImageLoader() {		
	}
	
	public void init(Context context, String cacheDirPath) {
		// this.context = new WeakReference<Context>(context);
		DISCK_CACHE_PATH = cacheDirPath;
		diskToMemoryTaskArray = new SparseArray<AsyncTask<String, Void, Bitmap>>();
		memoryToDiskTaskArray = new SparseArray<AsyncTask<String, Void, Void>>();
		networkToMemoryDiskTaskArray = new SparseArray<AsyncTask<String, Void, Bitmap>>();
		
		setImageEnterAnimation(context);
		initMemoryCache();
		initDiskCache();
	}
	
	private void setImageEnterAnimation(Context context) {
		imageEnterAnimation = AnimationUtils.loadAnimation(context, R.anim.image_enter);
	}
	
	private void initDiskCache() {		
		diskCache = new DiskLruCache(DISCK_CACHE_PATH, 10 * 1024 * 1024L);		
	}
	
	private void initMemoryCache() {		
		final int cacheSize = (int)((Runtime.getRuntime().maxMemory() / 1024) / 8);
		memoryCache = new LruCache<String, Bitmap>(cacheSize) {
			@Override
			protected int sizeOf(String key, Bitmap bitmap) {
				int apiLevel = android.os.Build.VERSION.SDK_INT;
				if(apiLevel < 19)
					return bitmap.getByteCount() / 1024;
				else
					return bitmap.getAllocationByteCount() / 1024;
			}
		};
	}
	
	public boolean isCachedInDiskCache(String key) {
		synchronized(diskCache) {
			if(diskCache.isCached(key) == null)
				return false;
			else
				return true;
		}
	}
	
	private void addBitmapToDiskCache(final String key, final Bitmap bitmap) {
		synchronized(diskCache) {
			if(diskCache.isCached(key) == null) {
				diskCache.put(key, new DiskLruCache.FileCopyDelegate() {
					@Override
					public File copy(DiskLruCache diskCache) {
						File cacheFile = diskCache.getCachedFile(key);
						OutputStream out = null;
						try {
							cacheFile.createNewFile();
							out = new FileOutputStream(cacheFile);
							if(key.toLowerCase().endsWith("jpg"))
								bitmap.compress(CompressFormat.JPEG, 100, out);
							else
								bitmap.compress(CompressFormat.PNG, 100, out);
						} catch(Exception e) {
							try { out.close(); } catch(Exception ee) {}
							cacheFile.delete();
							return null;
						}
						
						try { out.close(); } catch(Exception e) {}
						return cacheFile;												
					}

					@Override
					public long getSourceSize() {
						int apiLevel = android.os.Build.VERSION.SDK_INT;
						if(apiLevel < 19)
							return bitmap.getByteCount();
						else
							return bitmap.getAllocationByteCount();
					}					
				});			
			}
		}		
	}
	
	private void addBitmapToMemoryCache(String key, Bitmap bitmap) {
		synchronized(memoryCache) {
			if(getBitmapFromMemoryCache(key) == null) {
				memoryCache.put(key, bitmap);			
			}
		}		
	}
	
	private Bitmap getBitmapFromMemoryCache(String key) {
		Bitmap bitmap = null;
		synchronized(memoryCache) {
			bitmap = memoryCache.get(key); 
		}
		
		return bitmap; 
	}
	
	public void setWidthHeight(int width, int height) {
		WIDTH_LIMIT = width;
		hEIGHT_LIMIT = height;
	}
	
	public boolean isImageSizeSet() {
		return WIDTH_LIMIT == 0 || hEIGHT_LIMIT == 0 ? false : true;
	}
	
	private int calculateInSampleSize(
            BitmapFactory.Options options, int reqWidth, int reqHeight) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;
		
		if (height > reqHeight || width > reqWidth) {
			final int halfHeight = height / 2;
			final int halfWidth = width / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and keeps both
			// height and width larger than the requested height and width.
			while((halfHeight / inSampleSize) > reqHeight
                && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}
		}
		
		Log1.d(LOG_TAG, "calculateInSampleSize() : sampleSize : " + inSampleSize);
		
		return inSampleSize;
	}
	
	public void displayImage(ViewGroup parent, ImageView imageView, 
			String imageUrl, int position) {		
		Object tag = imageView.getTag();
		if(tag != null && (Integer)tag == position) {						
			return;
		}
		
		imageView.setImageDrawable(null);
		imageView.setTag(position);
		
		final Bitmap bitmap = getBitmapFromMemoryCache(imageUrl);
		if(bitmap == null) {
			if(isCachedInDiskCache(imageUrl)) {
				startBitmapDiskToMemoryWorkerTask(parent, imageView, 
						imageUrl, position);						
			} else {
				startBitmapNetworkToMemoryDiskWorkerTask(parent, 
						imageView, imageUrl, position);
			}
		} else {					
			imageView.setImageBitmap(bitmap);
			imageView.startAnimation(imageEnterAnimation);
			if(!isCachedInDiskCache(imageUrl)) {
				startBitmapMemoryToDiskWorkerTask(imageUrl, position);
			}
		}
	}
	
	private Bitmap getBitmapFromHttp(String urlString) {
		Bitmap bitmap = null;
		InputStream is = null;
		BufferedInputStream bis = null;
		try {
			URL url = new URL(urlString);
			is = url.openStream();
			bis = new BufferedInputStream(is);
			
			// First decode with inJustDecodeBounds=true to check dimensions
			final BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeStream(bis, null, options);
			if(is != null) {
				try { is.close(); } catch(Exception e) {}
			}
			if(bis != null) {
				try { bis.close(); } catch(Exception e) {}
			}
			
			is = url.openStream();
			bis = new BufferedInputStream(is);
			// Calculate inSampleSize
			options.inSampleSize = calculateInSampleSize(options, WIDTH_LIMIT, hEIGHT_LIMIT);				
			// Decode bitmap with inSampleSize set
			options.inJustDecodeBounds = false;
			bitmap = BitmapFactory.decodeStream(bis, null, options);				
		} catch(Exception e) {
			Log1.e(LOG_TAG, "doInBackground() : exception : " + e.getMessage());				
		} 
		if(is != null) {
			try { is.close(); } catch(Exception e) {}
		}
		if(bis != null) {
			try { bis.close(); } catch(Exception e) {}
		}
		
		return bitmap;
	}
	
	private void startBitmapDiskToMemoryWorkerTask(
			ViewGroup parent, ImageView imageView, String imageUrl, int position) {
		Log1.d(LOG_TAG, "startBitmapDiskToMemoryWorkerTask() : imageUrl : " + imageUrl +
				", position : " + position);
		
		synchronized(diskToMemoryTaskArray) {
			if(diskToMemoryTaskArray.get(position) == null) {
				Log1.d(LOG_TAG, "1111111111111");
				AsyncTask<String, Void, Bitmap> task = 
						new BitmapDiskToMemoryWorkerTask(parent, imageView, position);
				task.execute(imageUrl);
				diskToMemoryTaskArray.put(position, task);				
			} else {
				Log1.d(LOG_TAG, "2222222222222");
				BitmapDiskToMemoryWorkerTask task = (BitmapDiskToMemoryWorkerTask)
						diskToMemoryTaskArray.get(position);
				task.setImageView(imageView);
			}
		}		
	}
	
	private void startBitmapMemoryToDiskWorkerTask(String imageUrl, int position) {
		Log1.d(LOG_TAG, "startBitmapMemoryToDiskWorkerTask() : imageUrl : " + imageUrl +
				", position : " + position);
		
		synchronized(memoryToDiskTaskArray) {
			if(memoryToDiskTaskArray.get(position) == null) {
				AsyncTask<String, Void, Void> task = 
						new BitmapMemoryToDiskWorkerTask(position);
				task.execute(imageUrl);
				memoryToDiskTaskArray.put(position, task);
			}
		}		
	}
	
	private void startBitmapNetworkToMemoryDiskWorkerTask(
			ViewGroup parent, ImageView imageView, String url, int position) {
		Log1.d(LOG_TAG, "startBitmapNetworkToMemoryDiskWorkerTask() : url : " + url + 
				", position" + position);
		
		synchronized(networkToMemoryDiskTaskArray) {
			if(networkToMemoryDiskTaskArray.get(position) == null) {
				Log1.d(LOG_TAG, "1111111111111");
				AsyncTask<String, Void, Bitmap> task = 
						new BitmapNetworkToMemoryDiskWorkerTask(parent, imageView, position);
				task.execute(url);
				networkToMemoryDiskTaskArray.put(position, task);				
			} else {
				Log1.d(LOG_TAG, "2222222222222");
				BitmapNetworkToMemoryDiskWorkerTask task =
						(BitmapNetworkToMemoryDiskWorkerTask)networkToMemoryDiskTaskArray.get(position);
				task.setImageView(imageView);
			}
		}		
	}
	
	public void cancelTaskExceptBetween(int start, int end) {
		// Log.d(LOG_TAG, "cancelTaskAt() : position : " + position);
		int key = 0;		
		synchronized(diskToMemoryTaskArray) {
			for(int i = 0; i < diskToMemoryTaskArray.size(); i++) {
				key = diskToMemoryTaskArray.keyAt(i);
				if(key < start || key > end) {
					try {
						((BitmapDiskToMemoryWorkerTask)diskToMemoryTaskArray.
								get(key)).cancel(true);
					} catch(Exception e) {}
				}
			}			
		}
		
		synchronized(memoryToDiskTaskArray) {
			key = 0;
			for(int i = 0; i < memoryToDiskTaskArray.size(); i++) {
				key = memoryToDiskTaskArray.keyAt(i);
				if(key < start || key > end) {
					try {
						((BitmapMemoryToDiskWorkerTask)memoryToDiskTaskArray.
								get(key)).cancel(true);
					} catch(Exception e) {}
				}
			}		
		}
		
		synchronized(networkToMemoryDiskTaskArray) {
			key = 0;
			for(int i = 0; i < networkToMemoryDiskTaskArray.size(); i++) {
				key = networkToMemoryDiskTaskArray.keyAt(i);
				if(key < start || key > end) {
					try {
						((BitmapNetworkToMemoryDiskWorkerTask)
								networkToMemoryDiskTaskArray.get(key)).cancel(true);
					} catch(Exception e) {}
				}
			}			
		}
	}	
	
	private class BitmapMemoryToDiskWorkerTask extends AsyncTask<String, Void, Void> {
		private final String LOG_TAG = "BitmapMemoryToDiskWorkerTask";
		private final int id;
		
		public BitmapMemoryToDiskWorkerTask(int id) {
			this.id = id;
		}
		
		@Override
		protected void onCancelled() {
			synchronized(memoryToDiskTaskArray) {
				memoryToDiskTaskArray.remove(id);
			}
			
			super.onCancelled();
		}
		
		@Override
		protected Void doInBackground(String... imageUrls) {
			final String urlString = imageUrls[0];
			final Bitmap bitmap = getBitmapFromMemoryCache(urlString);
			if(bitmap != null) {
				addBitmapToDiskCache(urlString, bitmap);				
			}
			
			synchronized(memoryToDiskTaskArray) {
				memoryToDiskTaskArray.remove(id);
			}			
			return null;
		}		
	}
	
	private class BitmapDiskToMemoryWorkerTask extends AsyncTask<String, Void, Bitmap> {
		private final String LOG_TAG = "BitmapDiskToMemoryWorkerTask";
		private WeakReference<ViewGroup> parentReference;
		private WeakReference<ImageView> imageViewReference;
		private final int id;
		
		public BitmapDiskToMemoryWorkerTask(ViewGroup parent, 
				ImageView imageView, int id) {
			this.id = id;
			parentReference = new WeakReference<ViewGroup>(parent);
			imageViewReference = new WeakReference<ImageView>(imageView);
		}
		
		public void setImageView(ImageView view) {
			imageViewReference = new WeakReference<ImageView>(view);
		}
		
		@Override
		protected void onCancelled(Bitmap result) {
			super.onCancelled(result);
		}
		
		@Override
		protected void onCancelled() {
			synchronized(diskToMemoryTaskArray) {
				diskToMemoryTaskArray.remove(id);
			}
			
			super.onCancelled();
		}
		
		@Override
		protected Bitmap doInBackground(String... imageUrls) {
			Bitmap bitmap = null;
			final String urlString = imageUrls[0];
						
			synchronized(diskCache) {
				bitmap = BitmapFactory.decodeFile(diskCache.get(urlString).getAbsolutePath());
			}						
			if(bitmap == null) {
				synchronized(diskToMemoryTaskArray) {
					diskToMemoryTaskArray.remove(id);
				}
				return null;
			}
			
			addBitmapToMemoryCache(urlString, bitmap);			
			
			return bitmap;
		}
		
		@Override
		protected void onPostExecute(Bitmap result) {
			ImageView imageView = null;
			if((imageView = imageViewReference.get()) != null) {				
				AbsListView absListView = (AbsListView)parentReference.get();
				final int first = absListView.getFirstVisiblePosition();
				final int last = absListView.getLastVisiblePosition();
				Log1.d(LOG_TAG, "result : " + result + ", id : " + id + 
						", first : " + first + ", last : " + last);
				if(id >= first && id <= last) {
					try {
						imageView.setImageBitmap(result);
						imageView.startAnimation(imageEnterAnimation);
					} catch(Exception e) {
						Log1.e(LOG_TAG, "onPostExecute() : exception : " + e.getMessage());
						if(absListView.getAdapter() instanceof BaseAdapter) {
							((BaseAdapter)absListView.getAdapter()).notifyDataSetChanged();
						} else {
							// TODO	
						}							
					}
				}
			}
			Log1.d(LOG_TAG, "BitmapDiskToMemoryWorkerTask() : post... ");
			synchronized(diskToMemoryTaskArray) {
				diskToMemoryTaskArray.remove(id);
			}
			
			super.onPostExecute(result);
		}
	}
	
	private class BitmapNetworkToMemoryDiskWorkerTask extends AsyncTask<String, Void, Bitmap> {
		private final String LOG_TAG = "BitmapNetworkToMemoryDiskWorkerTask";
		private WeakReference<ViewGroup> parentReference;
		private WeakReference<ImageView> imageViewReference;
		private final int id;
		
		public BitmapNetworkToMemoryDiskWorkerTask(ViewGroup parent, 
				ImageView imageView, int id) {
			this.id = id;
			parentReference = new WeakReference<ViewGroup>(parent);
			imageViewReference  = new WeakReference<ImageView>(imageView);
		}
		
		public void setImageView(ImageView view) {
			imageViewReference = new WeakReference<ImageView>(view);
		}
		
		@Override
		protected void onCancelled(Bitmap result) {
			Log1.d(LOG_TAG, "onCancelled() : id : " + id + ", result : " + result);
			super.onCancelled(result);
			
			// This is necessary for undetermined side effects of scrolling and fling gesture
			// setBitmap(result);	
		}

		@Override
		protected void onCancelled() {
			Log1.d(LOG_TAG, "onCancelled() : id : " + id);
			
			synchronized (networkToMemoryDiskTaskArray) {
				networkToMemoryDiskTaskArray.remove(id);
			}			
			super.onCancelled();
		}

		@Override
		protected Bitmap doInBackground(String... imageUrls) {			
			final String urlString = imageUrls[0];
			Log1.d(LOG_TAG, "doInBackground() : url : " + urlString);
			
			Bitmap bitmap = getBitmapFromHttp(urlString);			
			if(urlString != null && bitmap != null) {
				addBitmapToMemoryCache(urlString, bitmap);
				addBitmapToDiskCache(urlString, bitmap);
			}
			
			return bitmap;			
		}

		@Override
		protected void onPostExecute(Bitmap result) {			
			if(imageViewReference.get() != null) {
				Log1.d(LOG_TAG, "result : " + result);
				setBitmap(result);
			}
			Log1.d(LOG_TAG, "BitmapNetworkToMemoryDiskWorkerTask() : onPostExecute() ");
			synchronized (networkToMemoryDiskTaskArray) {
				networkToMemoryDiskTaskArray.remove(id);
			}			
			
			super.onPostExecute(result);
		}
		
		private void setBitmap(Bitmap result) {
			AbsListView absListView = (AbsListView)parentReference.get();
			final int first = absListView.getFirstVisiblePosition();
			final int last = absListView.getLastVisiblePosition();
			if(id >= first && id <= last) {
				try {
					ImageView imageView = imageViewReference.get();
					imageView.setImageBitmap(result);
					imageView.startAnimation(imageEnterAnimation);
				} catch(Exception e) {
					Log1.d(LOG_TAG, "setBitmap() : exception : " + e.getMessage());
					if(absListView.getAdapter() instanceof BaseAdapter) {
						((BaseAdapter)absListView.getAdapter()).notifyDataSetChanged();
					} else {
						// TODO
					}
				}
			}
		}
	}
}
