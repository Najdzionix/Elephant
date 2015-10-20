package com.pinktwins.elephant.util;

import com.google.common.io.Files;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class IOUtil {

	private static final Logger LOG = Logger.getLogger(IOUtil.class.getName());

	private static final byte[] emptyBytes = new byte[0];

	private IOUtil() {
	}

	public static byte[] readFile(String file) {
		return readFile(new File(file));
	}

	public static byte[] readFile(File file) {
		// Open file
		RandomAccessFile f;
		try {
			f = new RandomAccessFile(file, "r");
		} catch (FileNotFoundException e) {
			return emptyBytes;
		}

		try {
			// Get and check length
			long longlength = f.length();
			int length = (int) longlength;
			if (length != longlength)
				throw new IOException("File size >= 2 GB");
			// Read file and return data
			byte[] data = new byte[length];
			f.readFully(data);
			return data;
		} catch (IOException e) {
			try {
				f.close();
			} catch (IOException e2) {
				LOG.severe("Fail: " + e);
			}
			return emptyBytes;
		} finally {
			try {
				f.close();
			} catch (IOException e) {
				LOG.severe("Fail: " + e);
			}
		}
	}

	public static void writeFile(File file, String text) throws IOException {
		Files.write(text, file, Charset.defaultCharset());
	}

	public static JSONObject loadJson(File file) {
		try {
			String json = null;

			if (file.exists()) {
				json = new String(IOUtil.readFile(file), "UTF-8");
			}

			if (json == null || json.isEmpty()) {
				json = "{}";
			}

			return new JSONObject(json);
		} catch (JSONException e) {
			LOG.severe("Fail: " + e);
		} catch (IOException e) {
			LOG.severe("Fail: " + e);
		}

		return new JSONObject();
	}
}
