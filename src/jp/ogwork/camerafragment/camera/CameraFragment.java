package jp.ogwork.camerafragment.camera;

import java.util.List;

import jp.ogwork.camerafragment.camera.CameraSurfaceView.CameraData;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnDrawListener;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnPictureSizeChangeListener;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnPreviewListener;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnPreviewSizeChangeListener;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnTakePictureListener;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;


/**
 * Camera Fragment
 * 
 * @author s.ogawa
 */
public class CameraFragment extends Fragment {

	/** -------------------------- */
	/** --------- public --------- */
	/** -------------------------- */

	public static final String BUNDLE_KEY_CAMERA_FACING = "cameraFacing";

	public void setCameraDirection(int cameraDirection) {
		boolean isFrontCamera = false;
		if (cameraDirection == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			isFrontCamera = true;
		}
		/**  */
		cameraPreview.changeCameraDirection(isFrontCamera);
	}

	public void setLayoutBounds(int width, int height) {
		LayoutParams param = fl_camera.getLayoutParams();
		param.width = width;
		param.height = height;
		fl_camera.setLayoutParams(param);
	}

	public void setOnPictureSizeChangeListener(OnPictureSizeChangeListener listener) {
		this.onPictureSizeChangeListener = listener;
	}

	public void setOnPreviewSizeChangeListener(OnPreviewSizeChangeListener listener) {
		onPreviewSizeChangeListener = listener;
	}

	public void setOnPreviewListener(OnPreviewListener onPreviewListener) {
		this.onPreviewListener = onPreviewListener;
	}

	public void setOnDrawListener(OnDrawListener onDrawListener) {
		this.onDrawListener = onDrawListener;
	}

	public void takePicture(boolean autoFocus) {
		takePicture(autoFocus, null);
	}

	public void takePicture(boolean autoFocus, OnTakePictureListener onTakePictureListener) {
		cameraPreview.takePicture(autoFocus, onTakePictureListener);
	}

	public void autoFocus() {
		cameraPreview.autoFocus();
	}

	public float getPictureAspectRatio() {
		return cameraPreview.getPictureAspectRatio();
	}

	public float getPreviewAspectRatio() {
		return cameraPreview.getPreviewAspectRatio();
	}

	public void setSavePictureDir(String saveDirName) {
		cameraPreview.setSavePictureDir(saveDirName);
	}

	public void setSavePictureName(String saveFileName) {
		cameraPreview.setSavePictureName(saveFileName);
	}

	public CameraData getCameraData() {
		return cameraPreview.getCameraData();
	}

	public void saveBitmap(Bitmap bitmap) {
		cameraPreview.saveBitmap(bitmap);
	}

	public Bitmap createBitmap(byte[] data) {
		return cameraPreview.createBitmap(data);
	}

	public SurfaceView getSurfaceView() {
		return cameraPreview;
	}

	public void flashTorch() {
		cameraPreview.flash(Camera.Parameters.FLASH_MODE_TORCH);
	}

	public void flashOn() {
		cameraPreview.flash(Camera.Parameters.FLASH_MODE_ON);
	}

	public void flashOff() {
		cameraPreview.flash(Camera.Parameters.FLASH_MODE_OFF);
	}

	public void flashAuto() {
		cameraPreview.flash(Camera.Parameters.FLASH_MODE_AUTO);
	}

	public Size choosePreviewSize(List<Size> supported, int minWidth, int minHeight, int maxWidth, int maxHeight) {
		return CameraSurfaceView.choosePreviewSize(supported, minWidth, minHeight, maxWidth, maxHeight, isRotate());
	}

	public Size choosePictureSize(List<Size> supported, int minWidth, int minHeight, int maxWidth, int maxHeight) {
		return CameraSurfaceView.choosePictureSize(supported, minWidth, minHeight, maxWidth, maxHeight);
	}

	public boolean isRotate() {
		return cameraPreview.isRotate();
	}

	/** -------------------------- */
	/** -------- Override -------- */
	/** -------------------------- */

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (container == null) {
			return null;
		}
		fl_camera = new FrameLayout(getActivity());
		fl_camera.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		boolean usableInCamera = (Camera.getNumberOfCameras() > 1) ? true : false;

		Bundle bundle = getArguments();
		if (bundle != null) {
			defaultCameraDirection = bundle.getInt(BUNDLE_KEY_CAMERA_FACING, Camera.CameraInfo.CAMERA_FACING_BACK);
		}

		if (usableInCamera && defaultCameraDirection == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			/** start FrontCamera */
			cameraPreview = new CameraSurfaceView(getActivity(), true);
		} else {
			/** start BackCamera */
			cameraPreview = new CameraSurfaceView(getActivity(), false);
		}
		fl_camera.addView(cameraPreview);
		fl_camera.setBackgroundColor(Color.CYAN);

		if (onPreviewListener != null) {
			cameraPreview.setOnPreviewListener(onPreviewListener);
		}
		if (onDrawListener != null) {
			cameraPreview.setOnDrawListener(onDrawListener);
		}
		/** frameLayoutリサイズ用に、別のリスナーをセット */
		cameraPreview.setOnPreviewSizeCallback(new OnPreviewSizeChangeListener() {
			@Override
			public Size onPreviewSizeChange(List<Size> supportedPreviewSizeList) {
				Camera.Size size = null;
				if (onPreviewSizeChangeListener != null) {
					size = onPreviewSizeChangeListener.onPreviewSizeChange(supportedPreviewSizeList);
				}
				return size;
			}

			@Override
			public void onPreviewSizeChanged(Size previewSize) {
				Camera.Size size = cameraPreview.getCameraPreviewSize();
				if (size != null) {
					// setLayoutBounds(size.height, size.width);
					Log.d("CameraFragment", "onPreviewSizeChanged() resize to [" + size.height + "]*[" + size.width
							+ "]");
				}
				if (onPreviewSizeChangeListener != null) {
					onPreviewSizeChangeListener.onPreviewSizeChanged(size);
				}
			}
		});
		cameraPreview.setOnPictureSizeCallback(new OnPictureSizeChangeListener() {

			@Override
			public Size onPictureSizeChange(List<Size> supportedPictureSizeList) {
				Camera.Size size = null;
				if (onPictureSizeChangeListener != null) {
					size = onPictureSizeChangeListener.onPictureSizeChange(supportedPictureSizeList);
				}
				return size;
			}
		});
		return fl_camera;
	}

	@Override
	public void onDestroyView() {
		super.onDestroyView();
	}

	/** -------------------------- */
	/** -------- private --------- */
	/** -------------------------- */

	private FrameLayout fl_camera;
	private CameraSurfaceView cameraPreview;
	private OnPictureSizeChangeListener onPictureSizeChangeListener;
	private OnPreviewSizeChangeListener onPreviewSizeChangeListener;
	private OnPreviewListener onPreviewListener;
	private OnDrawListener onDrawListener;

	/** setting */
	private int defaultCameraDirection = Camera.CameraInfo.CAMERA_FACING_BACK;
}
