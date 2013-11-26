package jp.ogwork.camerafragment.camera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.media.MediaScannerConnection;
import android.os.Environment;
import android.os.Handler.Callback;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * CameraSurfaceView
 * 
 * @author s.ogawa
 */
public class CameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Callback, Runnable {

	/** -------------------------- */
	/** ----- inner classes ------ */
	/** -------------------------- */

	/**
	 * CameraPreferences
	 * */
	public class CameraData {
		/** 0:back 1:front */
		public int cameraid;
		/** keep default */
		public int format;
		/** Camera preview size */
		public int previewWidth;
		/** Camera preview size */
		public int previewHeight;
		/** Camera preview rotate state */
		public boolean isRotate;
		/** save picture dir name */
		public String saveDirName;
		/** picture file name */
		public String saveFileName;
	}

	public class CameraSettings {
		/** 0:back 1:front */
		public int cameraid;
		/** save picture dir name */
		public String saveDirName;
		/** picture file name */
		public String saveFileName;
	}

	/** -------------------------- */
	/** -------- interface ------- */
	/** -------------------------- */

	public interface OnPreviewSizeChangeListener {
		Camera.Size onPreviewSizeChange(List<Camera.Size> supportedPreviewSizeList);

		void onPreviewSizeChanged(Camera.Size previewSize);
	}

	public interface OnPictureSizeChangeListener {
		Camera.Size onPictureSizeChange(List<Camera.Size> supportedPictureSizeList);
	}

	public interface OnTakePictureListener {
		void onShutter();

		void onPictureTaken(Bitmap bitmap, Camera camera);
	}

	public interface OnPreviewListener {
		void onPreview(byte[] data, Camera camera);
	}

	public interface OnDrawListener {
		void onDraw(SurfaceHolder holder);
	}

	/** -------------------------- */
	/** --------- public --------- */
	/** -------------------------- */

	public CameraSurfaceView(Context context, boolean useInCamera) {
		super(context);
		this.context = context;
		init(useInCamera);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		this.context = context;
		init();
	}

	public CameraSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
		this.context = context;
		init();
	}

	public CameraSurfaceView(Context context) {
		super(context);
		this.context = context;
		init();
	}

	public float getPictureAspectRatio() {
		return this.pictureAspectRatio;
	}

	public float getPreviewAspectRatio() {
		return this.previewAspect;
	}

	public List<Camera.Size> getSupportedPreviewSize() {
		if (camera == null) {
			return null;
		}
		return camera.getParameters().getSupportedPreviewSizes();
	}

	public List<Camera.Size> getSupportedPictureSize() {
		if (camera == null) {
			return null;
		}
		return camera.getParameters().getSupportedPictureSizes();
	}

	public Camera.Size getCameraPreviewSize() {
		if (camera == null) {
			return null;
		}
		return camera.getParameters().getPreviewSize();
	}

	/**
	 * param cameraDirection : Camera.CameraInfo.CAMERA_FACING_FRONT or
	 * Camera.CameraInfo.CAMERA_FACING_BACK
	 * */
	public void changeCameraDirection(boolean useInCamera) {
		CameraData cameraData = getCameraData();
		int cameraId = useInCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;

		if (cameraData.cameraid != cameraId) {

			cameraData.cameraid = cameraId;

			/** release */
			if (camera != null) {
				releaseCamera();
			}
			CameraData data = getCameraData();
			CameraOpenTask cameraOpenTask = new CameraOpenTask(dataHandler, CameraHandler.REQ_CAMERA_OPEN, camera,
					data.cameraid);
			cameraOpenTask.execute();
			log("changeCameraDirection() request change cameraid");
			Message msgToMe = dataHandler.obtainMessage(CameraHandler.REQ_CAMERA_CONFIGURE);
			dataHandler.sendMessage(msgToMe);
		}
	}

	public void setOnPreviewSizeCallback(OnPreviewSizeChangeListener onPreviewSizeChangeListener) {
		this.onPreviewSizeChangeListener = onPreviewSizeChangeListener;
	}

	public void setOnPictureSizeCallback(OnPictureSizeChangeListener onPictureSizeChangeListener) {
		this.onPictureSizeChangeListener = onPictureSizeChangeListener;
	}

	public void setOnTakePictureListener(OnTakePictureListener onTakePictureListener) {
		this.onTakePictureListener = onTakePictureListener;
	}

	public void setOnPreviewListener(OnPreviewListener onPreviewListener) {
		this.onPreviewListener = onPreviewListener;
	}

	public void setOnDrawListener(OnDrawListener onDrawListener) {
		this.onDrawListener = onDrawListener;
	}

	public void takePicture(boolean autoFocus, OnTakePictureListener onTakePictureListener) {
		this.onTakePictureListener = onTakePictureListener;
		if (!isCameraEnable || camera == null) {
			return;
		} else {
			isCameraEnable = false;
		}
		if (autoFocus) {
			camera.cancelAutoFocus();
			camera.autoFocus(new OnAutoFocusListener());
		} else {
			camera.takePicture(new OnShutterListener(), new OnRawPictureListener(), new OnPostViewPictureListener(),
					new OnJpegPictureListener());
		}
	}

	public void autoFocus() {
		if (isCameraEnable && camera != null) {
			camera.cancelAutoFocus();
			camera.autoFocus(new AutoFocusCallbackOnlyAF());
		}
	}

	public void setSavePictureDir(String saveDirName) {
		getCameraData().saveDirName = saveDirName;
	}

	public void setSavePictureName(String saveFileName) {
		getCameraData().saveFileName = saveFileName;
	}

	public CameraData getCameraData() {
		return this.cameraSettings;
	}

	public void setPictureFormat(int format) {
		if (camera == null)
			return;
		try {
			Camera.Parameters params = camera.getParameters();
			List<Integer> supported = params.getSupportedPictureFormats();
			if (supported != null) {
				for (int f : supported) {
					if (f == format) {
						params.setPictureFormat(format);
						setParameters(params);
						break;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void saveBitmap(Bitmap bitmap, String name) {

		if (bitmap == null) {
			return;
		}

		CameraData cameraData = getCameraData();
		File mediaStorageDir = new File(cameraData.saveDirName);

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return;
			}
		}

		/** 保存先 */
		File mediaFile = new File(cameraData.saveDirName + "/" + cameraData.saveFileName);

		try {
			FileOutputStream stream = new FileOutputStream(mediaFile);
			bitmap.compress(CompressFormat.JPEG, 90, stream);
		} catch (IOException exception) {
			Log.w(TAG, "IOException during saving bitmap", exception);
			return;
		}

		MediaScannerConnection.scanFile(context, new String[] { mediaFile.toString() }, new String[] { "image/jpeg" },
				null);
	}

	public void saveBitmap(Bitmap bitmap) {

		if (bitmap == null) {
			return;
		}

		CameraData cameraData = getCameraData();
		File mediaStorageDir = new File(cameraData.saveDirName);

		if (!mediaStorageDir.exists()) {
			if (!mediaStorageDir.mkdirs()) {
				return;
			}
		}

		File mediaFile = new File(cameraData.saveDirName + "/" + cameraData.saveFileName);

		try {
			FileOutputStream stream = new FileOutputStream(mediaFile);
			bitmap.compress(CompressFormat.JPEG, 90, stream);
		} catch (IOException exception) {
			Log.w(TAG, "IOException during saving bitmap", exception);
			return;
		}

		MediaScannerConnection.scanFile(context, new String[] { mediaFile.toString() }, new String[] { "image/jpeg" },
				null);
	}

	public Bitmap createBitmap(byte[] data) {
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(getCameraData().cameraid, info);
		Matrix rotateMatrix = new Matrix();
		rotateMatrix.setRotate(info.orientation);
		BitmapFactory.Options options = new BitmapFactory.Options();
		Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
		bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, true);
		return bitmap;
	}

	public synchronized void flash(String flashMode) {

		if (camera == null) {
			Log.d(TAG, "flash() camera is null");
			return;
		}

		// カメラのパラメータを取得
		cameraParam = camera.getParameters();

		if (flashMode.equals(Camera.Parameters.FLASH_MODE_TORCH) || flashMode.equals(Camera.Parameters.FLASH_MODE_AUTO)) {

			List<String> pList = camera.getParameters().getSupportedFlashModes();
			if (pList != null && pList.contains(flashMode)) {
				cameraParam.setFlashMode(flashMode);// TOSHIBA製はFLASH_MODE_ONじゃないと死ぬらしい
			} else {
				Log.d(TAG, "flash() invalid flash mode");
				return;
			}

			// フラッシュモードを"常に点灯"に設定（Android OS Verに依存？）
			cameraParam.setFocusMode(Camera.Parameters.FOCUS_MODE_INFINITY);
			// パラメータを設定
			setParameters(cameraParam);
		} else {
			// フラッシュモードを"常に点灯"に設定（Android OS Verに依存？）
			cameraParam.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
			// パラメータを設定
			setParameters(cameraParam);
			// }
		}
	}

	public boolean getCameraHealth() {
		return this.cameraHealth;
	}

	public boolean getCameraAvailable() {
		return this.isCameraEnable;
	}

	public Camera.Size choosePreviewSize(List<Size> supported, int minWidth, int minHeight, int maxWidth, int maxHeight) {

		ArrayList<Size> list = new ArrayList<Size>();
		for (Size size : supported) {
			if (isRotate()) {
				if ((minWidth <= size.height) && (minHeight <= size.width) && (size.height <= maxWidth)
						&& (size.width <= maxHeight)) {
					list.add(size);
				}
			} else {
				if ((minWidth <= size.width) && (minHeight <= size.height) && (size.width <= maxWidth)
						&& (size.height <= maxHeight)) {
					list.add(size);
				}
			}
		}

		Size resultSize = null;

		if (list.size() == 0) {
			resultSize = supported.get(0);
			for (Size tmp : supported) {
				if (resultSize.height == tmp.height) {
					if (resultSize.width < tmp.width) {
						resultSize = tmp;
					}
				} else if (resultSize.height < tmp.height) {
					resultSize = tmp;
				}
			}
		} else {
			/** pick up most biggest size */

			resultSize = list.get(0);
			for (Size tmp : list) {
				if (resultSize.height == tmp.height) {
					if (resultSize.width < tmp.width) {
						resultSize = tmp;
					}
				} else if (resultSize.height < tmp.height) {
					resultSize = tmp;
				}
			}
		}

		return resultSize;
	}

	public Camera.Size choosePictureSize(List<Size> supported, int minWidth, int minHeight, int maxWidth, int maxHeight) {

		ArrayList<Size> list = new ArrayList<Size>();
		for (Size size : supported) {
			if ((minWidth <= size.height) && (minHeight <= size.width) && (size.height <= maxWidth)
					&& (size.width <= maxHeight)) {
				list.add(size);
			}
		}

		/** pick up most biggest size */

		Size resultSize = null;

		if (list.size() == 0) {
			resultSize = supported.get(0);
			for (Size tmp : supported) {
				if (resultSize.height == tmp.height) {
					if (resultSize.width < tmp.width) {
						resultSize = tmp;
					}
				} else if (resultSize.height < tmp.height) {
					resultSize = tmp;
				}
			}
		} else {
			/** pick up most biggest size */

			resultSize = list.get(0);
			for (Size tmp : list) {
				if (resultSize.height == tmp.height) {
					if (resultSize.width < tmp.width) {
						resultSize = tmp;
					}
				} else if (resultSize.height < tmp.height) {
					resultSize = tmp;
				}
			}
		}
		return resultSize;
	}

	public boolean isRotate() {
		return getCameraData().isRotate;
	}

	/** -------------------------- */
	/** -------- Override -------- */
	/** -------------------------- */

	@Override
	public boolean handleMessage(Message msg) {
		CameraData cameraData = getCameraData();
		switch (msg.what) {
		case CameraHandler.REQ_CAMERA_OPEN:
			synchronized (this) {
				cameraOpenMutex = null;
			}

			log("REQ_CAMERA_OPEN " + msg.toString() + " isCameraEnable = " + isCameraEnable);
			isCameraEnable = true;
			if (msg.obj != null) {
				/** Camera.open() success */
				camera = (Camera) msg.obj;
				try {
					camera.setPreviewDisplay(getHolder());
				} catch (IOException e1) {
					e1.printStackTrace();
				}

				int rorateDegree = setCameraDisplayOrientation((Activity) this.getContext(), cameraData.cameraid,
						camera);
				if (rorateDegree == 90 || rorateDegree == 270) {
					cameraData.isRotate = true;
				} else {
					cameraData.isRotate = false;
				}

				// log("REQ_CAMERA_OPEN startPreview");
				// getCameraData().camera.startPreview();

				/** send to me */
				Message msgToMe = dataHandler.obtainMessage(CameraHandler.REQ_CAMERA_CONFIGURE);
				dataHandler.sendMessage(msgToMe);

			} else {
				/** Camera.open() failed */
				log("Camera.open() failed");
				// isCameraEnable = false;
			}

			break;
		case CameraHandler.REQ_CAMERA_CONFIGURE:
			log("REQ_CAMERA_CONFIGURE " + msg.toString() + " isCameraEnable = " + isCameraEnable);
			if (isCameraEnable && camera != null) {
				isCameraEnable = false;
				camera.stopPreview();
				configure(cameraData.format, cameraData.previewWidth, cameraData.previewHeight);

				/** periodic autoFocus */
				Camera.Parameters parameters = camera.getParameters();
				List<String> focusModes = parameters.getSupportedFocusModes();
				if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
				}
				setParameters(parameters);

				log("REQ_CAMERA_CONFIGURE startPreview");
				try {
					camera.startPreview();
				} catch (Exception e) {
					e.printStackTrace();
					cameraHealth = false;
					return false;
				}
				isCameraEnable = true;
			} else {
				/** send to me */
				Message msgToMe = dataHandler.obtainMessage(CameraHandler.REQ_CAMERA_CONFIGURE);
				dataHandler.sendMessage(msgToMe);
			}

			break;
		default:
			break;
		}

		return false;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int viewWidth, int viewHeight) {

		log("surfaceChanged() previewWidth = " + viewWidth + " previewHeight = " + viewHeight);

		CameraData data = getCameraData();
		CameraOpenTask cameraOpenTask = new CameraOpenTask(dataHandler, CameraHandler.REQ_CAMERA_OPEN, camera,
				data.cameraid);

		synchronized (this) {
			if (cameraOpenMutex == null) {
				cameraOpenMutex = cameraOpenTask.execute();
			} else {
				return;
			}
		}

		CameraData cameraData = getCameraData();
		cameraData.format = format;
		cameraData.previewHeight = viewHeight;
		cameraData.previewWidth = viewWidth;

		if (onDrawListener != null) {
			mSurfaceThread = new Thread(this);
			mSurfaceThread.start();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		log("surfaceDestroyed()");
		running = false;

		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			releaseCamera();
			camera = null;
		}
		isCameraEnable = false;
	}

	@Override
	public void run() {
		while (running) {
			try {
				doDraw(getHolder());
			} catch (Exception e) {
				e.printStackTrace();
			}

			if (running) {
				try {
					Thread.sleep(DRAW_INTERVAL);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

	}

	/** -------------------------- */
	/** -------- private --------- */
	/** -------------------------- */

	private void init() {
		init(false);
	}

	@SuppressWarnings("deprecation")
	private void init(boolean useInCamera) {
		mHolder = getHolder();
		mHolder.addCallback(this);
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.HONEYCOMB) {
			mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		dataHandler = new CameraHandler(this);

		/** create CameraData */
		this.cameraSettings = new CameraData();
		CameraData cameraData = getCameraData();
		cameraData.cameraid = useInCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT
				: Camera.CameraInfo.CAMERA_FACING_BACK;
		String sd = Environment.getExternalStorageDirectory().getPath();

		/** default save info */
		cameraData.saveDirName = sd + "/tmp/";
		cameraData.saveFileName = DEFAULT_FILE_NAME;// +
		// UUID.randomUUID().toString()
		// + ".jpeg";

		isCameraEnable = false;
	}

	private void configure(int format, int width, int height) {
		// setPictureFormat(format); これ呼んだら、jpegCallback来なくなる場合有り
		if (getCameraData().isRotate) {
			int temp = width;
			width = height;
			height = temp;
		}
		Camera.Size previewSize = setPreviewSize(width, height);
		if (previewSize != null) {
			width = previewSize.width;
			height = previewSize.height;
		} else {
			/** setPreviewSize failed */
			return;
		}
		if (onPreviewListener != null) {
			setPreviewCallbackListener(width, height);
		}
		setPictureSize();
	}

	private void setPreviewCallbackListener(int previewWidth, int previewHeight) {
		Camera.Parameters param = camera.getParameters();
		int imgformat = param.getPreviewFormat();
		setPreviewCallbackListener(previewWidth, previewHeight, imgformat);
	}

	public void setPreviewCallbackListener(int previewWidth, int previewHeight, int format) {
		camera.setPreviewCallback(new PreviewCallback() {
			@Override
			public void onPreviewFrame(byte[] data, Camera camera) {
				log("data [10] = " + data[10]);
				onPreviewListener.onPreview(data, camera);
			}
		});
	}

	/**
	 * PreviewSizeの設定
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	private Camera.Size setPreviewSize(int width, int height) {
		Camera.Parameters params = camera.getParameters();
		List<Size> supported = params.getSupportedPreviewSizes();
		Camera.Size previewSize;
		if (this.onPreviewSizeChangeListener != null) {
			previewSize = onPreviewSizeChangeListener.onPreviewSizeChange(supported);
			if (previewSize == null) {
				previewSize = chooseDefaultPreviewSize(supported, width, height);
			}
			params.setPreviewSize(previewSize.width, previewSize.height);
			this.previewAspect = (float) previewSize.height / previewSize.width;
			setParameters(params);

			onPreviewSizeChangeListener.onPreviewSizeChanged(previewSize);
		} else {
			previewSize = chooseDefaultPreviewSize(supported, width, height);
			this.previewAspect = (float) previewSize.height / previewSize.width;
			params.setPreviewSize(previewSize.width, previewSize.height);
			setParameters(params);
		}

		log("setPreviewSize() selected size [" + previewSize.width + "]*[" + previewSize.height + "]");
		return previewSize;
	}

	private static Camera.Size chooseDefaultPreviewSize(List<Size> supported, int thresholdWidth, int thresholdHeight) {

		ArrayList<Size> list = new ArrayList<Size>();
		for (Size size : supported) {
			if ((size.width <= thresholdWidth) && (size.height <= thresholdHeight)) {
				list.add(size);
			}
		}

		/** pick up most biggest size */
		Size tempSize = list.get(0);
		for (Size finalist : list) {
			if (tempSize.height == finalist.height) {
				if (tempSize.width < finalist.width) {
					tempSize = finalist;
				}
			} else if (tempSize.height < finalist.height) {
				tempSize = finalist;
			}
		}
		return tempSize;
	}

	private Camera.Size setPictureSize() {
		List<Size> supported = camera.getParameters().getSupportedPictureSizes();
		Camera.Parameters params = camera.getParameters();
		Camera.Size pictureSize;
		if (this.onPictureSizeChangeListener != null) {
			pictureSize = onPictureSizeChangeListener.onPictureSizeChange(supported);
			if (pictureSize == null) {
				pictureSize = choosePictureSize(supported, this.previewAspect);
			} else {
				/** 一応、ここでサポートサイズと合致してるかチェックした方がいい。 */
			}
			params.setPictureSize(pictureSize.width, pictureSize.height);
			setParameters(params);
		} else {
			pictureSize = choosePictureSize(supported, this.previewAspect);
			params.setPictureSize(pictureSize.width, pictureSize.height);
			setParameters(params);
		}
		log("setPictureSize() selected size [" + pictureSize.width + "]*[" + pictureSize.height + "]");
		return pictureSize;
	}

	private static Camera.Size choosePictureSize(List<Size> supported, float aspectRatio) {
		ArrayList<Size> list = new ArrayList<Size>();
		for (Size size : supported) {
			float pictureAcpectRatio = (float) size.height / size.width;
			if ((aspectRatio == pictureAcpectRatio || aspectRatio == 1f / pictureAcpectRatio) && (size.width < 1000)) {
				list.add(size);
			}
		}

		/** pick up most biggest size. */
		if (list.size() == 0) {
			list = (ArrayList<Size>) supported;
		}
		Size tempSize = list.get(0);
		for (Size finalist : list) {
			if (tempSize.width == finalist.width) {
				if (tempSize.height < finalist.height) {
					tempSize = finalist;
				}
			} else if (tempSize.width < finalist.width) {
				tempSize = finalist;
			} else {
				/** do nothing */
			}
		}
		return tempSize;
	}

	private static int setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera) {
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation) {
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		default:

		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			result = (info.orientation + degrees) % 360;
			if (180 < result) {
				result = (360 - result) % 360; // compensate the mirror
			}
		} else { // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		camera.setDisplayOrientation(result);
		return result;
	}

	private synchronized void releaseCamera() {
		if (camera != null) {
			camera.stopPreview();
			camera.release();
			camera = null;
			Log.d(TAG, "releaseCamera() camera released");
		}
	}

	public static boolean isSameRatio(int width, int height, int width_2, int height_2) {
		/**
		 * previewWidth < previewHeight or previewWidth > previewHeight
		 * の保証が無い場合を吸収する為
		 */
		float ratio, ratio2;
		if (width >= height) {
			ratio = (float) width / height;
			if (width_2 >= height_2) {
				ratio2 = (float) width_2 / height_2;
			} else {
				ratio2 = (float) height_2 / width_2;
			}
		} else {
			ratio = (float) height / width;
			if (width_2 >= height_2) {
				ratio2 = (float) width_2 / height_2;
			} else {
				ratio2 = (float) height_2 / width_2;
			}
		}

		if (ratio != ratio2) {
			return false;
		}
		return true;
	}

	@SuppressLint("WrongCall")
	private void doDraw(SurfaceHolder holder) {
		if (onDrawListener != null) {
			onDrawListener.onDraw(holder);
		}
	}

	private void log(String msg) {
		if (DEBUG) {
			Log.d(TAG, msg);
		}
	}

	protected PreviewCallback mPreviewCallback = new PreviewCallback() {
		@Override
		public void onPreviewFrame(byte[] data, Camera camera) {
			log("data[0] = " + data[0] + " length = " + data.length);
			camera.addCallbackBuffer(data);
		}
	};

	private void setParameters(Parameters params) {
		if (cameraHealth && camera != null) {
			camera.setParameters(params);
		}
	}

	/** --------------------------- */
	/** ---------- class ---------- */
	/** --------------------------- */

	private class OnShutterListener implements ShutterCallback {
		@Override
		public void onShutter() {
			log("onShutter()");
			if (onTakePictureListener != null) {
				onTakePictureListener.onShutter();
			}
		}
	}

	private class OnRawPictureListener implements PictureCallback {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			log("onPictureTaken()");
			if (data != null) {
				log("onPictureTaken: raw: data length = " + data.length);
			} else {
				log("onPictureTaken: raw: data = null");
			}
			isCameraEnable = true;
		}
	}

	private class OnPostViewPictureListener implements PictureCallback {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			log("onPictureTaken()");
			if (data != null) {
				log("onPictureTaken: postView: data length = " + data.length);
			} else {
				log("onPictureTaken: postView: data = null");
			}
			isCameraEnable = true;
		}

	}

	private class OnJpegPictureListener implements PictureCallback {
		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			log("onPictureTaken()");
			if (data != null) {
				log("onPictureTaken: jpeg: data length = " + data.length);
				Camera.CameraInfo info = new Camera.CameraInfo();
				Camera.getCameraInfo(getCameraData().cameraid, info);
				Matrix rotateMatrix = new Matrix();
				rotateMatrix.setRotate(info.orientation);
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = true;
				BitmapFactory.decodeByteArray(data, 0, data.length, options);

				int w = options.outWidth;
				int h = options.outHeight;

				options.inSampleSize = calculateInSampleSize(options, w, h);

				options.inJustDecodeBounds = false;

				Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, options);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), rotateMatrix, true);
				if (onTakePictureListener != null) {
					onTakePictureListener.onPictureTaken(bitmap, camera);
				}
				camera.startPreview();
			} else {
				log("onPictureTaken: jpeg: data = null");
			}
			isCameraEnable = true;
		}

		public int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

			// 画像の元サイズ
			final int height = options.outHeight;
			final int width = options.outWidth;
			int inSampleSize = 1;

			if (height > reqHeight || width > reqWidth) {
				if (width > height) {
					inSampleSize = Math.round((float) height / (float) reqHeight);
				} else {
					inSampleSize = Math.round((float) width / (float) reqWidth);
				}
			}
			return inSampleSize;
		}
	}

	private class OnAutoFocusListener implements AutoFocusCallback {

		@Override
		public void onAutoFocus(boolean success, final Camera afCamera) {
			log("onAutoFocus()");
			// autoFocusが掛かり切らないうちにシャッターが降りるのをwait
			try {
				Thread.sleep(AUTOFOCUS_FOCUS_TIME);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			isCameraEnable = true;

			try {
				takePicture(false, onTakePictureListener);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	private class AutoFocusCallbackOnlyAF implements AutoFocusCallback {
		@Override
		public void onAutoFocus(boolean success, final Camera afCamera) {
			log("onAutoFocus()");
			try {
				Thread.sleep(AUTOFOCUS_FOCUS_TIME);
			} catch (InterruptedException e) {
			}
			camera.cancelAutoFocus();
		}
	}

	private static final String TAG = CameraSurfaceView.class.getSimpleName();
	private static final boolean DEBUG = false;
	private static final int AUTOFOCUS_FOCUS_TIME = 600;
	private static final int DRAW_INTERVAL = 100;
	private static final String DEFAULT_FILE_NAME = "pict.jpg";

	/** Camera Instance */
	private Camera camera;

	private Context context;
	private SurfaceHolder mHolder;
	private CameraHandler dataHandler;
	private boolean isCameraEnable = false;
	private boolean running = true;
	private boolean cameraHealth = true;
	private Thread mSurfaceThread = null;
	private Object cameraOpenMutex;

	/** preferences */
	private CameraData cameraSettings;
	private Camera.Parameters cameraParam;

	private float pictureAspectRatio;
	private float previewAspect;

	/** Listeners */
	private OnPreviewSizeChangeListener onPreviewSizeChangeListener;
	private OnPictureSizeChangeListener onPictureSizeChangeListener;
	private OnTakePictureListener onTakePictureListener;
	private OnPreviewListener onPreviewListener;
	private OnDrawListener onDrawListener;
}
