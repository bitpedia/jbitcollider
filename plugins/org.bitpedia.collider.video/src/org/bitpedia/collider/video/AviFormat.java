/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: AviFormat.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.video;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.bitpedia.collider.video.VideoFormatHandler.VideoData;

public class AviFormat {
	
	public static void parseAvi(RandomAccessFile stm, VideoData data) throws IOException {

		byte[] fourcc = new byte[4]; 
		
		stm.skipBytes(12);
	
		/* Verify existence of and read length of AVI header:
		 * "LIST____hdrlavih____"
		 * where the first ____ is the length of the LIST block
		 */
		stm.read(fourcc);
		if (!"LIST".equals(new String(fourcc))) {
			return;
		}
		stm.skipBytes(4);
		stm.read(fourcc);
		if (!"hdrl".equals(new String(fourcc))) {
			return;
		}
		stm.read(fourcc);
		if (!"avih".equals(new String(fourcc))) {
			return;
		}
		   
		int blockLen = VideoUtils.readLE(stm, 4); 

		/* Now we're at the start of the AVI header */

		/* 0: microseconds per frame (4 bytes) */
		data.fps = (int)VideoUtils.round(1e6 / VideoUtils.readLE(stm, 4));
		
		stm.skipBytes(12);
		
		/* 16: total frames (4 bytes) */
		data.duration = (int)VideoUtils.round((double)VideoUtils.readLE(stm, 4)
								* 1000 / data.fps);
		
		stm.skipBytes(12);

		/* 32: width (4 bytes) */
		data.width = VideoUtils.readLE(stm, 4);

		/* 36: height (4 bytes) */
		data.height = VideoUtils.readLE(stm, 4);

		/* Skip rest of avi header */
		stm.skipBytes(blockLen - 40);

		/* Verify existence of and read length of video stream header:
		 * "LIST____strlstrh____vids"
		 */
		stm.read(fourcc);
		if (!"LIST".equals(new String(fourcc))) {
			return;
		}
		blockLen = VideoUtils.readLE(stm, 4);
		stm.read(fourcc);
		if (!"strl".equals(new String(fourcc))) {
			return;
		}
		stm.read(fourcc);
		if (!"strh".equals(new String(fourcc))) {
			return;
		}
		stm.skipBytes(4);
		stm.read(fourcc);
		if (!"vids".equals(new String(fourcc))) {
			return;
		}
		
		/* Now we're in the video stream header */

		/* 16: FOURCC of video codec (4 bytes)*/
		stm.read(fourcc);
		data.codec = new String(fourcc); 
	}

}
