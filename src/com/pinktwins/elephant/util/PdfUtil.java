package com.pinktwins.elephant.util;

import com.sun.pdfview.PDFFile;
import com.sun.pdfview.PDFPage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.PixelGrabber;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.logging.Logger;

// Based on:
// http://stackoverflow.com/questions/550129/export-pdf-pages-to-a-series-of-images-in-java
//
// Using "PDF Renderer" from Swinglabs:
// https://java.net/projects/pdf-renderer/downloads
//
// This is not the best renderer. Other options:
//
// - pdfbox: slow, bad output on some pdfs I tested with
// - iText: not a renderer
// - JPedal: commercial, insanely expensive
// - jPod: will check this
// - gnujpdf: no way
// - PDFJet: commercial
// - ICEpdf: free versions doesn't have "font renderer", possibly limited
// - jmupdf: by far fastests and most compatible, JNI wrapper to a native library.
//    latest version was missing binary for Linux. Might be an option to switch to. 

public class PdfUtil {

	private static final Logger LOG = Logger.getLogger(PdfUtil.class.getName());

	RandomAccessFile raf;
	PDFFile pdffile;
	int numPages;

	private static final int screenDpi = Toolkit.getDefaultToolkit().getScreenResolution();

	public PdfUtil(File f) {
		try {
			raf = new RandomAccessFile(f, "r");
			FileChannel channel = raf.getChannel();
			ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());

			pdffile = new PDFFile(buf);
			numPages = pdffile.getNumPages();
		} catch (FileNotFoundException e) {
			LOG.severe("Fail: " + e);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		} catch (NullPointerException e) {
			LOG.severe("Fail: " + e);
			LOG.severe("Got NPE on file " + f);
		}
	}

	public int numPages() {
		return numPages;
	}

	public Dimension pageSize(int n) {
		PDFPage page = pdffile.getPage(n);
		return new Dimension((int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
	}

	public Image writePage(int n, File outPath) {
		PDFPage page = pdffile.getPage(n);

		// Improve the image quality slightly compared to assuming 72dpi.
		// XXX magic formula
		double adjust = (screenDpi / 72.0 - 1.0) / 2.0 + 1.0;

		Rectangle rect = new Rectangle(0, 0, (int) page.getBBox().getWidth(), (int) page.getBBox().getHeight());
		Image img = page.getImage((int) (rect.width * adjust), (int) (rect.height * adjust), rect, // clip rect
				null, // null for the ImageObserver
				true, // fill background with white
				true // block until drawing is done
				);

		BufferedImage bImg = toBufferedImage(img);
		try {
			ImageIO.write(bImg, "png", outPath);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}

		return bImg;
	}

	public void close() {
		if (raf != null) {
			try {
				raf.close();
			} catch (IOException e) {
				LOG.severe("Fail: " + e);
			}
		}
	}

	private static BufferedImage toBufferedImage(Image image) {
		if (image instanceof BufferedImage) {
			return (BufferedImage) image;
		}

		image = new ImageIcon(image).getImage();

		boolean hasAlpha = hasAlpha(image);
		BufferedImage bimage = null;
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();

		try {
			int transparency = Transparency.OPAQUE;
			if (hasAlpha) {
				transparency = Transparency.BITMASK;
			}
			GraphicsDevice gs = ge.getDefaultScreenDevice();
			GraphicsConfiguration gc = gs.getDefaultConfiguration();
			bimage = gc.createCompatibleImage(image.getWidth(null), image.getHeight(null), transparency);
		} catch (HeadlessException e) {
		}

		if (bimage == null) {
			int type = BufferedImage.TYPE_INT_RGB;
			if (hasAlpha) {
				type = BufferedImage.TYPE_INT_ARGB;
			}
			bimage = new BufferedImage(image.getWidth(null), image.getHeight(null), type);
		}

		Graphics g = bimage.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bimage;
	}

	private static boolean hasAlpha(Image image) {
		if (image instanceof BufferedImage) {
			BufferedImage bimage = (BufferedImage) image;
			return bimage.getColorModel().hasAlpha();
		}

		PixelGrabber pg = new PixelGrabber(image, 0, 0, 1, 1, false);
		try {
			pg.grabPixels();
		} catch (InterruptedException e) {
		}

		ColorModel cm = pg.getColorModel();
		return cm.hasAlpha();
	}
}
