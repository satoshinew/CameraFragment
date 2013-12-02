package jp.ogwork.camerafragment;

import android.os.Handler;

public class CameraHandler extends Handler {
	public static final int REQ_CAMERA_OPEN = 1;
	public static final int REQ_CAMERA_START_PREVIEW = 2;
	public CameraHandler(Handler.Callback callback) {
		super(callback);
	}
}
