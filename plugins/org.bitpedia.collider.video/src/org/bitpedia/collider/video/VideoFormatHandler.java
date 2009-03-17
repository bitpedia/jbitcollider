/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: VideoFormatHandler.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.video;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bitpedia.collider.core.FormatHandler;

public class VideoFormatHandler implements FormatHandler {

	private static final int UNKNOWN_FMT = 0;

	private static final int AVI_FMT = 1;

	private static final int QUICK_TIME_FMT = 2;

	private static final int MPEG_FMT = 3;

	/*
	 * We must be able to determine file format using this many bytes from the
	 * beginning of the file
	 */
	private static final int HEAD_BUFFER = 12;

	public static class VideoData {

		int width; /* width in pixels */

		int height; /* height in pixels */

		int fps; /* frames per second */

		int duration; /* duration in milliseconds */

		int bitrate; /* bitrate in kbps */

		String codec; /* video compression codec */
	}

	private String errorString;

	public boolean supportsExtension(String ext) {
		return "avi".equalsIgnoreCase(ext) || "mov".equalsIgnoreCase(ext)
				|| "qt".equalsIgnoreCase(ext) || "mpg".equalsIgnoreCase(ext)
				|| "mpeg".equalsIgnoreCase(ext) || "m2v".equalsIgnoreCase(ext);
	}

	public boolean supportsMemAnalyze() {
		return false;
	}

	public boolean supportsFileAnalyze() {
		return true;
	}

	public void analyzeInit() {
	}

	public void analyzeUpdate(byte[] buf, int bufLen) {
	}

	public Map analyzeFinal() {
		return null;
	}

	private int findFormat(String fileName) {

		InputStream stm = null;
		try {
			stm = new FileInputStream(fileName);
			byte[] buffer = new byte[HEAD_BUFFER];
			if (HEAD_BUFFER != stm.read(buffer)) {
				return UNKNOWN_FMT;
			}

			if (("RIFF".equals(new String(buffer, 0, 4)))
					&& ("AVI ".equals(new String(buffer, 8, 4)))) {
				/* AVI signature: "RIFF____AVI " */
				return AVI_FMT;
			} else if (("moov".equals(new String(buffer, 4, 4)))
					|| ("mdat".equals(new String(buffer, 4, 4)))) {
				/* QuickTime signature: "____moov" or "____mdat" */
				return QUICK_TIME_FMT;
			} else if (0 == buffer[0] && 0 == buffer[1] && 1 == buffer[2]
					&& ((byte)0xB3 == buffer[3] || (byte)0xBA == buffer[3])) {
				/* MPEG signature: 0x000001B3 or 0x000001BA */
				return MPEG_FMT;
			} else {
				return UNKNOWN_FMT;
			}
		} catch (FileNotFoundException e) {
			return UNKNOWN_FMT;
		} catch (IOException e) {
			return UNKNOWN_FMT;
		} finally {
			try {
				stm.close();
			} catch (IOException e) {
			}
		}
	}

	public Map analyzeFile(String fileName) {

		File file = new File(fileName);
		int fmt = findFormat(fileName);
		String fmtStr = "";
		VideoData data = new VideoData();

		RandomAccessFile stm = null;
		try {
			stm = new RandomAccessFile(fileName, "r");

			switch (fmt) {
			case AVI_FMT:
				fmtStr = "AVI";
				AviFormat.parseAvi(stm, data);
				break;
			case QUICK_TIME_FMT:
				fmtStr = "QuickTime";
				QuickTimeFormat.parseQuickTime(stm, data);
				break;
			case MPEG_FMT:
				int version = MpegFormat.parseMpeg(stm, data); 
				if (1 == version) {
					fmtStr = "MPEG-1";
				} else if (2 == version) {
					fmtStr = "MPEG-2";
				}
				break;
			}
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
		} finally {
			try {
				stm.close();
			} catch (IOException e) {
			}
		}

		/*
		 * If necessary, use filesize to estimate bitrate from duration or vice
		 * versa
		 */
		if ((0 == data.bitrate) && (0 != data.duration)) {
			data.bitrate = (int) VideoUtils.round((double)file.length() / data.duration
					* 8);
		} else if ((0 == data.duration) && (0 != data.bitrate)) {
			data.duration = (int) VideoUtils.round((double)file.length() / data.bitrate
					* 8);
		}

		Map attrs = new LinkedHashMap();

		if (!VideoUtils.isEmpty(fmtStr)) {
			attrs.put("tag.video.format", fmtStr);
		}
		if (0 != data.width) {
			attrs.put("tag.video.width", "" + data.width);
		}
		if (0 != data.height) {
			attrs.put("tag.video.height", "" + data.height);
		}
		if (0 != data.fps) {
			attrs.put("tag.video.fps", "" + data.fps);
		}
		if (0 != data.duration) {
			attrs.put("tag.video.duration", "" + data.duration);
		}
		if (0 != data.bitrate) {
			attrs.put("tag.video.bitrate", "" + data.bitrate);
		}
		if (!VideoUtils.isEmpty(data.codec)) {
			attrs.put("tag.video.codec", data.codec);
		}

		return attrs;
	}

	public String getError() {
		return errorString;
	}

}
