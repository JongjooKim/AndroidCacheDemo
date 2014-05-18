package com.jayjaylab.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;

public class Log1 {
	private static final String APPLICATION_NAME = "[KakaoTalkHomework]";
	
	public enum ENVIRONMENT { ANDROID, JAVA_CONSOLE }
	
	private static ENVIRONMENT environment = ENVIRONMENT.ANDROID;
	
	private static final int LOG_LEVEL_NOTHING = 0;
	private static final int LOG_LEVEL_ERROR = 1;
	private static final int LOG_LEVEL_DEBUG = 2;	
	private static final int LOG_LEVEL_INFO = 3; 
	private static final int LOG_LEVEL_VERBOSE = 4;
	
	private static int logLevel = LOG_LEVEL_DEBUG;	
	
	static public void setEnvironment(ENVIRONMENT env) {
		environment = env;
	}
	
	static public int d(String t, String m) {
		if(logLevel >= LOG_LEVEL_DEBUG) {
			if(environment == ENVIRONMENT.ANDROID) { 
				return android.util.Log.d(APPLICATION_NAME, t + " " + m);
			} else if(environment == ENVIRONMENT.JAVA_CONSOLE) {
				System.out.println(APPLICATION_NAME + " " + t + " " + m);
			}
		}
		
		return 0; 
	}

	 static public int d(String t, String m, Throwable tr) {
		 if(logLevel >= LOG_LEVEL_ERROR) {
			 if(environment == ENVIRONMENT.ANDROID) {
				 return android.util.Log.d(APPLICATION_NAME, t + " " + m, tr);
			 } else if(environment == ENVIRONMENT.JAVA_CONSOLE) {
				 System.out.println(APPLICATION_NAME + " " + t + " " + m);
			 } 
		 }
		
		 return 0; 
	 }	
	
	 static public int e(String t, String m, Throwable tr) {
		 if(logLevel >= LOG_LEVEL_ERROR) {
			 if(environment == ENVIRONMENT.ANDROID) {
				 return android.util.Log.e(APPLICATION_NAME, t + " " + m, tr);
			 } else if(environment == ENVIRONMENT.JAVA_CONSOLE) {
				 System.err.println(APPLICATION_NAME + " " + t + " " + m);
			 }
		 }
		 
		 return 0; 
	 }

	 static public int e(String t, String m) {
		 if(logLevel >= LOG_LEVEL_ERROR) {
			 if(environment == ENVIRONMENT.ANDROID) {
				 return android.util.Log.e(APPLICATION_NAME, t + " " + m);
			 } else if(environment == ENVIRONMENT.JAVA_CONSOLE) {
				 System.err.println(APPLICATION_NAME + " " + t + " " + m);
			 }
		 }
		 return 0; 
	 }
	 
	 static public int i(String t, String m) {
		 if(logLevel >= LOG_LEVEL_INFO) {
			 if(environment == ENVIRONMENT.ANDROID) {
				 return android.util.Log.i(APPLICATION_NAME, t + " " + m);
			 } else if(environment == ENVIRONMENT.JAVA_CONSOLE) {
				 System.out.println(APPLICATION_NAME + " " + t + " " + m);
			 }
		 }
		 
		 return 0; 
	 }
	 
	 static public int w(String t, String m) {
		if(logLevel >= LOG_LEVEL_VERBOSE) {
			if(environment == ENVIRONMENT.ANDROID) {
				return android.util.Log.w(APPLICATION_NAME + " " + t, m);
			} else if(environment == ENVIRONMENT.JAVA_CONSOLE) {
				System.out.println(APPLICATION_NAME + " " + t + " " + m);				
			}
		}
		
		return 0; 
	 }
	 
	 static public int v(String t, String m) {
		if(logLevel >= LOG_LEVEL_VERBOSE) {
			if(environment == ENVIRONMENT.ANDROID) {
				return android.util.Log.v(APPLICATION_NAME + " " + t, m);
			} else if(environment == ENVIRONMENT.JAVA_CONSOLE) {
				System.out.println(APPLICATION_NAME + " " + t + " " + m);
			}
		}
		
		return 0; 
	 }
}
