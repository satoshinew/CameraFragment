package com.ogw.fragment.camera;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;

public class ImageProcessor {
	public ImageProcessor() {
	}

	public Bitmap process(Bitmap bitmap) {
		int width = bitmap.getWidth();
		int height = bitmap.getHeight();
		int[] imageByte = new int[width * height];
		bitmap.getPixels(imageByte, 0, width, 0, 0, width, height);

		int separater;
		for (separater = 30; separater > 0; separater--) {
			if ((width % separater == 0) && (height % separater == 0)) {
				break;
			}
		}
		separateBox(imageByte, width, height, separater);

		bitmap.setPixels(imageByte, 0, width, 0, 0, width, height);
		return bitmap;
	}

	public Bitmap convertToRGB565(Bitmap bitmap) {
		int[] data = new int[bitmap.getWidth() * bitmap.getHeight()];
		bitmap.getPixels(data, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
		Bitmap bmpGrayscale = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.RGB_565);
		bmpGrayscale.setPixels(data, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
		return bmpGrayscale;
	}

	/***
	 * 分割数に応じて、画像配列を分割して、それぞれ塗りつぶす。 width/heightが割り切れる値になることを推奨。
	 * 
	 * @param image
	 *            画像配列
	 * @param width
	 *            画像幅
	 * @param height
	 *            画像高
	 * @param separate
	 *            分割数
	 */
	private void separateBox(int[] image, int width, int height, int separate) {
		int[][] box = new int[width * height / separate][separate * separate];
		int boxWidth = width / separate;
		int boxHeight = height / separate;
		int xBoxIndex = 0;
		int yBoxIndex = 0;
		for (int boxIndex = 0; boxIndex < separate * separate; boxIndex++) {

			for (int y = 0; y < boxHeight; y++) {

				for (int x = 0; x < boxWidth; x++) {
					int widthShift = xBoxIndex * boxWidth;
					int a = x + (boxWidth * y);
					int b = x + widthShift + (y * width + ((yBoxIndex) * width * boxHeight));
					// if (boxIndex <= 3)
					// Log.d("ImageProcessor", "box[" + a + "][" + boxIndex +
					// "] = image[" + b + "]");
					// if (boxIndex >= separate * separate - 3)
					// Log.d("ImageProcessor", "box[" + a + "][" + boxIndex +
					// "] = image[" + b + "]");
					box[a][boxIndex] = image[b];
				}
			}
			xBoxIndex++;
			Log.d("ImageProcessor", "boxIndex = " + boxIndex);
			if (boxIndex != 0 && ((boxIndex + 1) % separate == 0)) {
				xBoxIndex = 0;
				Log.d("ImageProcessor", "yBoxIndex = " + yBoxIndex);
				yBoxIndex++;
			}
		}
		fillSameColor(box, boxWidth, boxHeight, separate);

		conquer(image, box, boxWidth, boxHeight, separate);
	}

	private void fillSameColor(int[][] box, int boxWidth, int boxHeight, int separator) {
		for (int boxIndex = 0; boxIndex < separator * separator; boxIndex++) {
			int alpha = 0;
			int red = 0;
			int green = 0;
			int blue = 0;
			for (int y = 0; y < boxHeight; y++) {
				for (int x = 0; x < boxWidth; x++) {
					// Color.RGBToHSV(red, green, blue, hsv)
					alpha += Color.alpha(box[x + y * boxWidth][boxIndex]);
					red += Color.red(box[x + y * boxWidth][boxIndex]);
					green += Color.green(box[x + y * boxWidth][boxIndex]);
					blue += Color.blue(box[x + y * boxWidth][boxIndex]);
				}
			}
			alpha = alpha / (boxWidth * boxHeight);
			red = red / (boxWidth * boxHeight);
			green = green / (boxWidth * boxHeight);
			blue = blue / (boxWidth * boxHeight);
			for (int y = 0; y < boxHeight; y++) {
				for (int x = 0; x < boxWidth; x++) {
					// int col = 0;
					// if (red + green + blue / 3 >= 126) {
					// col = Color.WHITE;
					// }else{
					// col = Color.BLACK;
					// }
					box[x + y * boxWidth][boxIndex] = Color.argb(alpha, red, green, blue);
				}
			}
		}
	}

	private void conquer(int[] image, int[][] box, int boxWidth, int boxHeight, int separator) {
		int xBoxIndex = 0;
		int yBoxIndex = 0;
		for (int boxIndex = 1; boxIndex <= (separator * separator); boxIndex++) {
			for (int y = 0; y < boxHeight; y++) {
				for (int x = 0; x < boxWidth; x++) {
					int b = x + xBoxIndex * boxWidth
							+ (y * boxWidth * separator + ((yBoxIndex) * boxWidth * separator * boxHeight));
					image[b] = box[x + (boxWidth * y)][boxIndex - 1];
				}
			}
			xBoxIndex++;
			if (boxIndex % separator == 0) {
				xBoxIndex = 0;
				yBoxIndex++;
			}
		}
	}

}
