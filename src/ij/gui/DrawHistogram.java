package ij.gui;

import java.awt.*;
import java.awt.image.*;
import java.awt.event.*;
import java.io.*;
import java.awt.datatransfer.*;
import java.util.ArrayList;
import ij.*;
import ij.process.*;
import ij.measure.*;
import ij.plugin.filter.Analyzer;
import ij.text.TextWindow;

/** This class is an extended ImageWindow that displays histograms. */
public class DrawHistogram {
	private static final double SCALE = HistogramPlot.SCALE;
	static final int HIST_WIDTH = HistogramPlot.HIST_WIDTH;
	static final int HIST_HEIGHT = HistogramPlot.HIST_HEIGHT;
	static final int XMARGIN = HistogramPlot.XMARGIN;
	static final int YMARGIN = HistogramPlot.YMARGIN;
	static final int WIN_WIDTH = HistogramPlot.WIN_WIDTH;
	static final int WIN_HEIGHT = HistogramPlot.WIN_HEIGHT;
	static final int BAR_HEIGHT = HistogramPlot.BAR_HEIGHT;
	static int digits;
	static final int INTENSITY1 = 0, INTENSITY2 = 1, RGB = 2, RED = 3, GREEN = 4, BLUE = 5;
	private static int srcImageID;
	private static ImageStatistics stats;
	private static long[] histogram;
	private static LookUpTable lut;
	private static Rectangle frame = null;
	private static Button live;
	private static int decimalPlaces;
	private static long newMaxCount;
	private static boolean logScale;
	private static Calibration cal;
	private static int yMax;
	public static int nBins = 256;

	private static int rgbMode = -1;
	private static Font font = new Font("SansSerif", Font.PLAIN, (int) (12 * SCALE));
	private static boolean showBins;
	private static int col1, col2, row1, row2, row3, row4, row5;

	public static void drawHistogram(ImagePlus imp, ImageProcessor ip, boolean fixedRange, double xMin, double xMax) {
		int x, y;
		long maxCount2 = 0;
		int mode2 = 0;
		long saveModalCount;
		ip.setColor(Color.black);
		ip.setLineWidth(1);
		decimalPlaces = Analyzer.getPrecision();
		digits = cal.calibrated() || stats.binSize != 1.0 ? decimalPlaces : 0;
		saveModalCount = histogram[stats.mode];
		for (int i = 0; i < histogram.length; i++) {
			if ((histogram[i] > maxCount2) && (i != stats.mode)) {
				maxCount2 = histogram[i];
				mode2 = i;
			}
		}
		newMaxCount = histogram[stats.mode];
		if ((newMaxCount > (maxCount2 * 2)) && (maxCount2 != 0))
			newMaxCount = (int) (maxCount2 * 1.5);
		if (logScale || IJ.shiftKeyDown() && !liveMode())
			drawLogPlot(yMax > 0 ? yMax : newMaxCount, ip);
		drawPlot(yMax > 0 ? yMax : newMaxCount, ip);
		histogram[stats.mode] = saveModalCount;
		x = XMARGIN + 1;
		y = YMARGIN + HIST_HEIGHT + 2;
		if (imp == null)
			lut.drawUnscaledColorBar(ip, x - 1, y, HIST_WIDTH, BAR_HEIGHT);
		else
			drawAlignedColorBar(imp, xMin, xMax, ip, x - 1, y, HIST_WIDTH, BAR_HEIGHT);
		y += BAR_HEIGHT + (int) (15 * SCALE);
		drawText(ip, x, y, fixedRange);
		srcImageID = imp.getID();
	}

	public static void drawPlot(long maxCount, ImageProcessor ip) {
		if (maxCount == 0)
			maxCount = 1;
		frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
		ip.drawRect(frame.x - 1, frame.y, frame.width + 2, frame.height + 1);
		if (histogram.length == 256) {
			double scale2 = HIST_WIDTH / 256.0;
			int barWidth = 1;
			if (SCALE > 1)
				barWidth = 2;
			if (SCALE > 2)
				barWidth = 3;
			for (int i = 0; i < 256; i++) {
				int x = (int) (i * scale2);
				int y = (int) (((double) HIST_HEIGHT * (double) histogram[i]) / maxCount);
				if (y > HIST_HEIGHT)
					y = HIST_HEIGHT;
				for (int j = 0; j < barWidth; j++)
					ip.drawLine(x + j + XMARGIN, YMARGIN + HIST_HEIGHT, x + j + XMARGIN, YMARGIN + HIST_HEIGHT - y);
			}
		} else if (histogram.length <= HIST_WIDTH) {
			int index, y;
			for (int i = 0; i < HIST_WIDTH; i++) {
				index = (int) (i * (double) histogram.length / HIST_WIDTH);
				y = (int) (((double) HIST_HEIGHT * (double) histogram[index]) / maxCount);
				if (y > HIST_HEIGHT)
					y = HIST_HEIGHT;
				ip.drawLine(i + XMARGIN, YMARGIN + HIST_HEIGHT, i + XMARGIN, YMARGIN + HIST_HEIGHT - y);
			}
		} else {
			double xscale = (double) HIST_WIDTH / histogram.length;
			for (int i = 0; i < histogram.length; i++) {
				long value = histogram[i];
				if (value > 0L) {
					int y = (int) (((double) HIST_HEIGHT * (double) value) / maxCount);
					if (y > HIST_HEIGHT)
						y = HIST_HEIGHT;
					int x = (int) (i * xscale) + XMARGIN;
					ip.drawLine(x, YMARGIN + HIST_HEIGHT, x, YMARGIN + HIST_HEIGHT - y);
				}
			}
		}
	}

	public static void drawLogPlot(long maxCount, ImageProcessor ip) {
		frame = new Rectangle(XMARGIN, YMARGIN, HIST_WIDTH, HIST_HEIGHT);
		ip.drawRect(frame.x - 1, frame.y, frame.width + 2, frame.height + 1);
		double max = Math.log(maxCount);
		ip.setColor(Color.gray);
		if (histogram.length == 256) {
			double scale2 = HIST_WIDTH / 256.0;
			int barWidth = 1;
			if (SCALE > 1)
				barWidth = 2;
			if (SCALE > 2)
				barWidth = 3;
			for (int i = 0; i < 256; i++) {
				int x = (int) (i * scale2);
				int y = histogram[i] == 0 ? 0 : (int) (HIST_HEIGHT * Math.log(histogram[i]) / max);
				if (y > HIST_HEIGHT)
					y = HIST_HEIGHT;
				for (int j = 0; j < barWidth; j++)
					ip.drawLine(x + j + XMARGIN, YMARGIN + HIST_HEIGHT, x + j + XMARGIN, YMARGIN + HIST_HEIGHT - y);
			}
		} else if (histogram.length <= HIST_WIDTH) {
			int index, y;
			for (int i = 0; i < HIST_WIDTH; i++) {
				index = (int) (i * (double) histogram.length / HIST_WIDTH);
				y = histogram[index] == 0 ? 0 : (int) (HIST_HEIGHT * Math.log(histogram[index]) / max);
				if (y > HIST_HEIGHT)
					y = HIST_HEIGHT;
				ip.drawLine(i + XMARGIN, YMARGIN + HIST_HEIGHT, i + XMARGIN, YMARGIN + HIST_HEIGHT - y);
			}
		} else {
			double xscale = (double) HIST_WIDTH / histogram.length;
			for (int i = 0; i < histogram.length; i++) {
				long value = histogram[i];
				if (value > 0L) {
					int y = (int) (HIST_HEIGHT * Math.log(value) / max);
					if (y > HIST_HEIGHT)
						y = HIST_HEIGHT;
					int x = (int) (i * xscale) + XMARGIN;
					ip.drawLine(x, YMARGIN + HIST_HEIGHT, x, YMARGIN + HIST_HEIGHT - y);
				}
			}
		}
		ip.setColor(Color.black);
	}

	public static boolean liveMode() {
		return live != null && live.getForeground() == Color.red;
	}

	public static void drawAlignedColorBar(ImagePlus imp, double xMin, double xMax, ImageProcessor ip, int x, int y,
			int width,
			int height) {
		ImageProcessor ipSource = imp.getProcessor();
		float[] pixels = null;
		ImageProcessor ipRamp = null;
		if (rgbMode >= INTENSITY1) {
			ipRamp = new FloatProcessor(width, height);
			if (rgbMode == RED)
				ipRamp.setColorModel(LUT.createLutFromColor(Color.red));
			else if (rgbMode == GREEN)
				ipRamp.setColorModel(LUT.createLutFromColor(Color.green));
			else if (rgbMode == BLUE)
				ipRamp.setColorModel(LUT.createLutFromColor(Color.blue));
			pixels = (float[]) ipRamp.getPixels();
		} else
			pixels = new float[width * height];
		for (int j = 0; j < height; j++) {
			for (int i = 0; i < width; i++)
				pixels[i + width * j] = (float) (xMin + i * (xMax - xMin) / (width - 1));
		}
		double min = ipSource.getMin();
		double max = ipSource.getMax();
		if (ipSource.getNChannels() == 1) {
			ColorModel cm = null;
			if (imp.isComposite()) {
				if (stats != null && stats.pixelCount > ipSource.getPixelCount()) { // stack histogram
					cm = LUT.createLutFromColor(Color.white);
					min = stats.min;
					max = stats.max;
				} else
					cm = ((CompositeImage) imp).getChannelLut();
			} else if (ipSource.getMinThreshold() == ImageProcessor.NO_THRESHOLD)
				cm = ipSource.getColorModel();
			else
				cm = ipSource.getCurrentColorModel();
			ipRamp = new FloatProcessor(width, height, pixels, cm);
		}
		ipRamp.setMinAndMax(min, max);
		ImageProcessor bar = null;
		if (ip instanceof ColorProcessor)
			bar = ipRamp.convertToRGB();
		else
			bar = ipRamp.convertToByte(true);
		ip.insert(bar, x, y);
		ip.setColor(Color.black);
		ip.drawRect(x - 1, y, width + 2, height);
	}

	/** Scales a threshold level to the range 0-255. */
	public static int scaleDown(ImageProcessor ip, double threshold) {
		double min = ip.getMin();
		double max = ip.getMax();
		if (max > min)
			return (int) (((threshold - min) / (max - min)) * 255.0);
		else
			return 0;
	}

	public static void drawText(ImageProcessor ip, int x, int y, boolean fixedRange) {
		ip.setFont(font);
		ip.setAntialiasedText(true);
		double hmin = cal.getCValue(stats.histMin);
		double hmax = cal.getCValue(stats.histMax);
		double range = hmax - hmin;
		if (fixedRange && !cal.calibrated() && hmin == 0 && hmax == 255)
			range = 256;
		ip.drawString(d2s(hmin), x - 4, y);
		ip.drawString(d2s(hmax), x + HIST_WIDTH - getWidth(hmax, ip) + 10, y);
		if (rgbMode >= INTENSITY1) {
			x += HIST_WIDTH / 2;
			y += 1;
			ip.setJustification(ImageProcessor.CENTER_JUSTIFY);
			boolean weighted = ((ColorProcessor) ip).weightedHistogram();
			switch (rgbMode) {
				case INTENSITY1:
					ip.drawString((weighted ? "Intensity (weighted)" : "Intensity (unweighted)"), x, y);
					break;
				case INTENSITY2:
					ip.drawString((weighted ? "Intensity (unweighted)" : "Intensity (weighted)"), x, y);
					break;
				case RGB:
					ip.drawString("R+G+B", x, y);
					break;
				case RED:
					ip.drawString("Red", x, y);
					break;
				case GREEN:
					ip.drawString("Green", x, y);
					break;
				case BLUE:
					ip.drawString("Blue", x, y);
					break;
			}
			ip.setJustification(ImageProcessor.LEFT_JUSTIFY);
		}
		double binWidth = range / stats.nBins;
		binWidth = Math.abs(binWidth);
		showBins = binWidth != 1.0 || !fixedRange;
		col1 = XMARGIN + 5;
		col2 = XMARGIN + HIST_WIDTH / 2;
		row1 = y + (int) (25 * SCALE);
		if (showBins)
			row1 -= (int) (8 * SCALE);
		row2 = row1 + (int) (15 * SCALE);
		row3 = row2 + (int) (15 * SCALE);
		row4 = row3 + (int) (15 * SCALE);
		row5 = row4 + (int) (15 * SCALE);
		long count = stats.longPixelCount > 0 ? stats.longPixelCount : stats.pixelCount;
		String modeCount = " (" + stats.maxCount + ")";
		if (modeCount.length() > 12)
			modeCount = "";

		ip.drawString("N: " + count, col1, row1);
		ip.drawString("Min: " + d2s(stats.min), col2, row1);
		ip.drawString("Mean: " + d2s(stats.mean), col1, row2);
		ip.drawString("Max: " + d2s(stats.max), col2, row2);
		ip.drawString("StdDev: " + d2s(stats.stdDev), col1, row3);
		ip.drawString("Mode: " + d2s(stats.dmode) + modeCount, col2, row3);
		if (showBins) {
			ip.drawString("Bins: " + d2s(stats.nBins), col1, row4);
			ip.drawString("Bin Width: " + d2s(binWidth), col2, row4);
		}
		drawValueAndCount(ip, Double.NaN, -1);
	}

	public static String d2s(double d) {
		if ((int) d == d)
			return IJ.d2s(d, 0);
		else
			return IJ.d2s(d, 3, 8);
	}

	public static void drawValueAndCount(ImageProcessor ip, double value, long count) {
		int y = showBins ? row4 : row3;
		ip.setRoi(0, y, WIN_WIDTH, WIN_HEIGHT - y);
		ip.setColor(Color.white);
		ip.fill();
		ip.setColor(Color.black);
		String sValue = Double.isNaN(value) ? "---" : d2s(value);
		String sCount = count == -1 ? "---" : "" + count;
		int row = showBins ? row5 : row4;
		ip.drawString("Value: " + sValue, col1, row);
		ip.drawString("Count: " + sCount, col2, row);
	}

	private static int getWidth(double d, ImageProcessor ip) {
		return ip.getStringWidth(d2s(d));
	}

}
