package jp.ogwork.camerafragment;

import java.util.List;

import jp.ogwork.camerafragment.camera.CameraFragment;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnPictureSizeChangeListener;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnPreviewSizeChangeListener;
import jp.ogwork.camerafragment.camera.CameraSurfaceView.OnTakePictureListener;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.Button;
import android.widget.FrameLayout;


public class MainActivity extends FragmentActivity {

	protected static final String TAG = MainActivity.class.getName();

	protected static final String TAG_CAMERA_FRAGMENT = "camera";

	private FrameLayout fl_camera;

	private CameraFragment cameraFragment;

	/** buttons */
	private Button btn_debug;
	private Button btn_take;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (savedInstanceState == null) {
			/** カメラサイズを決定するため、rootViewのサイズを取る */
			fl_camera = (FrameLayout) findViewById(R.id.fl_camera);
			fl_camera.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {

				@SuppressWarnings("deprecation")
				@Override
				public void onGlobalLayout() {
					fl_camera.getViewTreeObserver().removeGlobalOnLayoutListener(this);
					addCameraFragment(fl_camera.getWidth(), fl_camera.getHeight());
				}
			});
		}

		btn_debug = (Button) findViewById(R.id.btn_autofocus);
		btn_debug.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cameraFragment.autoFocus();
			}
		});
		
		btn_take = (Button) findViewById(R.id.btn_take);
		btn_take.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				cameraFragment.takePicture(true, new OnTakePictureListener() {
					
					@Override
					public void onShutter() {
						
					}
					
					@Override
					public void onPictureTaken(Bitmap bitmap, Camera camera) {
						cameraFragment.saveBitmap(bitmap);
					}
				});
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	public void addCameraFragment(final int viewWidth, final int viewHeight) {
		cameraFragment = new CameraFragment();
		Bundle args = new Bundle();
		args.putInt(CameraFragment.BUNDLE_KEY_CAMERA_FACING, Camera.CameraInfo.CAMERA_FACING_BACK);
		cameraFragment.setArguments(args);
		cameraFragment.setOnPreviewSizeChangeListener(new OnPreviewSizeChangeListener() {

			@Override
			public Size onPreviewSizeChange(List<Size> supportedPreviewSizeList) {
				return cameraFragment.choosePreviewSize(supportedPreviewSizeList, 0, 0, viewWidth, viewHeight);
			}

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
				cameraFragment.setLayoutBounds(width, height);
				return;
			}
		});
		cameraFragment.setOnPictureSizeChangeListener(new OnPictureSizeChangeListener() {
			@Override
			public Size onPictureSizeChange(List<Size> supportedPictureSizeList) {
				/** 画面横幅以下の中で最大サイズを選ぶ */
				return cameraFragment.choosePictureSize(supportedPictureSizeList, 0, 0, viewWidth, viewHeight);
			}
		});

		getSupportFragmentManager().beginTransaction().add(R.id.fl_camera, cameraFragment, TAG_CAMERA_FRAGMENT)
				.commit();
	}
}
