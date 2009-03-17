/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: MpegFormat.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.video;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.bitpedia.collider.video.VideoFormatHandler.VideoData;

public class MpegFormat {

	/*
	 * Returns 1 or 2 to indicate MPEG-1 or MPEG-2
	 * 
	 * Most MPEG data is stored in bits not necessarily aligned on byte
	 * boundaries; bits are ordered most-significant first, so big-endian of a
	 * sort. Block sizes only count bytes after the block size integer.
	 */
	public static int parseMpeg(RandomAccessFile stm, VideoData data)
			throws IOException {

		int version = 0; /* MPEG-1/2; our return value */

		/*
		 * First check if this is a Program stream (multiplexed audio/video),
		 * and handle Pack header if so
		 */
		int temp = VideoUtils.readBE(stm, 4);
		if (0x000001BA == temp) {

			/* Figure out if this is an MPEG-1 or MPEG-2 program */
			temp = stm.read();
			if (0x20 == (temp & 0xF0)) { /* binary 0010 xxxx */
				version = 1;
			} else if (0x40 == (temp & 0xC0)) { /* binary 01xx xxxx */
				version = 2;
			} else {
				return 0;
			}

			if (1 == version) {
				stm.skipBytes(4);
				data.bitrate = (int) VideoUtils.round((double) ((VideoUtils
						.readBE(stm, 3) & 0x7FFFFE) >> 1) * 0.4);
			} else {
				stm.skipBytes(5);
				data.bitrate = (int) VideoUtils.round((double) ((VideoUtils
						.readBE(stm, 3) & 0xFFFFFC) >> 2) * 0.4);
				temp = stm.read() & 0x07; /* stuffing bytes */
				if (0 != temp) {
					stm.skipBytes(temp);
				}
			}

			/*
			 * Skip any other blocks we find until we get to a video stream,
			 * which might be within a 2nd PACK
			 */
			temp = VideoUtils.readBE(stm, 4);
			while (0x000001BA != temp && 0x000001E0 != temp) {

				if (0 == temp) { /* Skip past zero padding */
					int buf = 0;
					while (0x00000100 != (temp & 0xFFFFFF00)) {
						if (-1 == buf) {
							return version; /* shouldn't happen here either */
						}

						temp <<= 8;
						buf = stm.read();
						temp |= buf;
					}
				} else {
					temp = VideoUtils.readBE(stm, 2);
					stm.skipBytes(temp);
					temp = VideoUtils.readBE(stm, 4);
				}
			}

			/*
			 * Now read byte by byte until we find the 0x000001B3 instead of
			 * actually parsing (due to too many variations). Theoretically this
			 * could mean we find 0x000001B3 as data inside another packet, but
			 * that's extremely unlikely, especially since the sequence header
			 * should not be far
			 */
			temp = VideoUtils.readBE(stm, 4);
			int buf = 0;
			while (0x000001B3 != temp) {
				if (-1 == buf) {
					return version; /* No seq. header; shouldn't happen */
				}

				temp <<= 8;
				buf = stm.read();
				temp |= buf;
			}
		} else { /* video stream only */
			stm.seek(4);
		}

		/* Now we're just past the video sequence header start code */

		temp = VideoUtils.readBE(stm, 3);
		data.width = (temp & 0xFFF000) >> 12;
		data.height = temp & 0x000FFF;

		switch (stm.read() & 0x0F) {
		case 1: /* 23.976 fps */
		case 2: /* 24 fps */
			data.fps = 24;
			break;
		case 3: /* 25 fps */
			data.fps = 25;
			break;
		case 4: /* 29.97 fps */
		case 5: /* 30 fps */
			data.fps = 30;
			break;
		case 6: /* 50 fps */
			data.fps = 50;
			break;
		case 7: /* 59.94 fps */
		case 8: /* 60 fps */
			data.fps = 60;
			break;
		}

		if (0 == data.bitrate) { /* if this is a video-only stream, */
			/* get bitrate from here */
			temp = (VideoUtils.readBE(stm, 3) & 0xFFFFC0) >> 6;
			if (0x3FFFF != temp) { /* variable bitrate */
				data.bitrate = (int) VideoUtils.round((double) temp * 0.4);
			}
		} else {
			stm.skipBytes(3);
		}

		/* If MPEG-2 or don't know yet, look for the sequence header extension */
		if (1 != version) {
			/* Skip past rest of sequence header and 64-byte matrices (if any) */
			temp = stm.read();
			if (0 != (temp & 0x02)) {
				stm.skipBytes(63);
				temp = stm.read();
			}
			if (0 != (temp & 0x01)) {
				stm.skipBytes(64);
			}

			temp = VideoUtils.readBE(stm, 4);
			if (0x000001B5 == temp) {
				if (0 == version) {
					version = 2;
				}

				stm.skipBytes(1);

				/* extensions specify MSBs of width/height */
				temp = VideoUtils.readBE(stm, 2);
				data.width |= (temp & 0x0180) << 5;
				data.height |= (temp & 0x0060) << 7;

				stm.skipBytes(2);
				/* and a numerator/denominator multiplier for fps */
				temp = stm.read();
				if ((0 != (temp & 0x60)) && (0 != (temp & 0x1F))) {
					data.fps = (int) VideoUtils.round((double) data.fps
							* (temp & 0x60) / (temp & 0x1F));
				}
			} else if (version == 0) {
				version = 1;
			}
		}

		return version;
	}

}
