package jp.ogwork.camerasample;

import java.util.List;

import jp.ogwork.camerafragment.camera.CameraFragment;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnPictureSizeChangeListener;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnPreviewSizeChangeListener;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnTakePictureListener;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.FrameLayout;

public class CameraSampleActivity extends FragmentActivity {

	protected static final String TAG = CameraSampleActivity.class.getName();

	protected static final String TAG_CAMERA_FRAGMENT = "camera";

	private FrameLayout fl_camera;

	private CameraFragment cameraFragment;

	/** buttons */
	private Button btn_autofocus;
	private Button btn_take;
	private Button btn_change_camera_direction;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_camera_sample);

		if (savedInstanceState == null) {
			/** カメラサイズを決定するため、Viewのサイズを取る */
			fl_camera = (FrameLayout) findViewById(R.id.fl_camera);
			fl_camera.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {
					fl_camera.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					addCameraFragment(fl_camera.getWidth(), fl_camera.getHeight(), R.id.fl_camera);
				}
			});
		}

		btn_autofocus = (Button) findViewById(R.id.btn_autofocus);
		btn_autofocus.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				autoFocus();
			}
		});

		btn_take = (Button) findViewById(R.id.btn_take);
		btn_take.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cameraFragment.enableShutterSound(false);
				takePicture();
			}
		});

		btn_change_camera_direction = (Button) findViewById(R.id.btn_change_camera_direction);
		btn_change_camera_direction.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				changeCameraDirection();
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.camera_sample, menu);
		return true;
	}

	/***
	 * CameraFragmentをviewに追加
	 * 
	 * @param viewWidth
	 * @param viewHeight
	 */
	public void addCameraFragment(final int viewWidth, final int viewHeight, int containerViewId) {
		cameraFragment = new CameraFragment();
		Bundle args = new Bundle();
		args.putInt(CameraFragment.BUNDLE_KEY_CAMERA_FACING, Camera.CameraInfo.CAMERA_FACING_BACK);
		cameraFragment.setArguments(args);

		/** プレビューサイズリスナの設定 */
		cameraFragment.setOnPreviewSizeChangeListener(new OnPreviewSizeChangeListener() {

			/** サイズ変更前 */
			@Override
			public Size onPreviewSizeChange(List<Size> supportedPreviewSizeList) {
				return cameraFragment.choosePreviewSize(supportedPreviewSizeList, 0, 0, viewWidth, viewHeight);
			}

			/** サイズ変更後 */
			@Override
			public void onPreviewSizeChanged(Size previewSize) {

				float viewAspectRatio = (float) viewHeight / previewSize.width;
				int height = viewHeight;
				int width = (int) (viewAspectRatio * previewSize.height);

				/** 縦横どちらでfixさせるか */
				if (width < viewWidth) {
					/** 縦Fixだと幅が足りないと判断 */
					/** 横fixさせる */
					width = viewWidth;
					height = (int) (viewAspectRatio * previewSize.width);
				}
				/** cameraSurfaceViewのサイズ変更 */
				cameraFragment.setLayoutBounds(width, height);
				return;
			}
		});

		/** カメラ保存サイズリスナの設定 */
		cameraFragment.setOnPictureSizeChangeListener(new OnPictureSizeChangeListener() {
			@Override
			public Size onPictureSizeChange(List<Size> supportedPictureSizeList) {
				/** 画面横幅以下の中で最大サイズを選ぶ */
				return cameraFragment.choosePictureSize(supportedPictureSizeList, 0, 0, viewWidth, viewHeight);
			}
		});

		getSupportFragmentManager().beginTransaction().add(containerViewId, cameraFragment, TAG_CAMERA_FRAGMENT)
				.commit();
	}

	/***
	 * 撮影・保存
	 */
	private void takePicture() {
		cameraFragment.takePicture(true, new OnTakePictureListener() {

			@Override
			public void onShutter() {

			}

			@Override
			public void onPictureTaken(Bitmap bitmap, Camera camera) {
				// String path =
				// Environment.getExternalStorageDirectory().toString()
				// + "/";
				// cameraFragment.setSavePictureDir(path);
				cameraFragment.saveBitmap(bitmap);
			}
		});
	}

	/**
	 * オートフォーカス
	 * */
	private void autoFocus() {
		cameraFragment.autoFocus();
	}

	/**
	 * インカメラ・フロントカメラ切り替え
	 * */
	private void changeCameraDirection() {
		int cameraDirection = 0;
		if (cameraFragment.getCameraDirection() == CameraInfo.CAMERA_FACING_BACK) {
			cameraDirection = CameraInfo.CAMERA_FACING_FRONT;
		} else {
			cameraDirection = CameraInfo.CAMERA_FACING_BACK;

		}
		cameraFragment.setCameraDirection(cameraDirection);
	}
}
