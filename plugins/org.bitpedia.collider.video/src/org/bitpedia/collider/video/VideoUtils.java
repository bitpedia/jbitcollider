/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: VideoUtils.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.video;

import java.io.IOException;
import java.io.RandomAccessFile;

public class VideoUtils {

	public static double round(double value) {
		return Math.floor(value + 0.5);
	}

	/*
	 * Read the specified number of bytes as a little-endian (least significant
	 * byte first) integer. Note: bytes must be less than the byte width of
	 * "unsigned long int" on your platform (e.g. 8 for 32-bit systems).
	 */
	public static int readLE(RandomAccessFile stm, int bytes) throws IOException {

		int x, res = 0;

		for (x = 0; x < bytes; x++) {
			res |= stm.read() << (x * 8);
		}
		return res;
	}

	/* Same as above, but big-endian (most significant byte first) ordering */
	public static int readBE(RandomAccessFile stm, int bytes) throws IOException {

		int x, res = 0;

		for (x = bytes - 1; x >= 0; x--) {
			res |= stm.read() << (x * 8);
		}

		return res;
	}
	
	public static boolean isEmpty(String str) {
		return (null == str) || "".equals(str);
	}

}
