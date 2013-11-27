package jp.ogwork.camerafragment.camera;

import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

/***
 * 
 * カメラをopen()して、インスタンスを返すだけ。
 * 
 * @author Satoshi Ogawa
 * 
 */
public class CameraOpenTask extends AsyncTask<Void, Void, Camera> {
	private Handler dataHandler;
	private int handlerKey;
	private Camera data;
	private int cameraId;

	public CameraOpenTask(Handler dataHandler, int handlerKey, Camera data, int cameraId) {
		this.dataHandler = dataHandler;
		this.handlerKey = handlerKey;
		this.data = data;
		this.cameraId = cameraId;
	}

	@Override
	protected Camera doInBackground(Void... params) {
		try {
			Log.d("CameraOpenTask", "◆open() start");
			data = Camera.open(cameraId);
			Log.d("CameraOpenTask", "◆open() end");
		} catch (RuntimeException e) {
			Log.d("CameraOpenTask", "◆open() " + e.toString());
			return null;
		}

		return data;
	}

	@Override
	protected void onPostExecute(Camera result) {
		super.onPostExecute(result);
		Message msg = dataHandler.obtainMessage(handlerKey, result);
		dataHandler.sendMessage(msg);
	}
}
