package com.eternal_search.rlottie;

import android.graphics.Bitmap;

final class RLottieMethods {
	private RLottieMethods() {
	}
	
	static native long createFromJson(String name, String json, int[] params);
	
	static native void destroy(long ptr);
	
	static native boolean renderFrame(long ptr, int frame, Bitmap bitmap, int w, int h, int stride);
	
	static native void getFrame(long ptr, Bitmap bitmap);
	
	static {
		System.loadLibrary("rlottie_jni");
	}
}
