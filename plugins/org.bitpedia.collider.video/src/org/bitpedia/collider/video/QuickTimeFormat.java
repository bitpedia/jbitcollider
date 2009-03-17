/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: QuickTimeFormat.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.video;

import java.io.IOException;
import java.io.RandomAccessFile;

import org.bitpedia.collider.video.VideoFormatHandler.VideoData;

public class QuickTimeFormat {

	/* QuickTime uses big-endian ordering, and block ("atom") lengths include the
	 * entire atom, including the fourcc specifying atom type and the length
	 * integer itself.
	 */ 	
	public static void parseQuickTime(RandomAccessFile stm, VideoData data) throws IOException {

		   int blockLen;
		   byte[] fourcc = new byte[4];

		   stm.skipBytes(4);
		   stm.read(fourcc);
		   /* If data is first, header's at end of file, so skip to it */
		   if("mdat".equals(new String(fourcc))) {
			   stm.seek(0);
			   blockLen = VideoUtils.readBE(stm, 4);
			   stm.seek(blockLen + 4);
			   stm.read(fourcc);
		   }

		   if(!"moov".equals(new String(fourcc))) {
			   return;
		   }
		   long blockStart = stm.getFilePointer();
		   blockLen = VideoUtils.readBE(stm, 4);	/* mvhd length */
		   stm.read(fourcc);
		   if(!"mvhd".equals(new String(fourcc))) {
			   return;
		   }

		   /* Now we're at the start of the movie header */

		   /* 20: time scale (time units per second) (4 bytes) */
		   stm.seek(blockStart + 20);
		   int timescale = VideoUtils.readBE(stm, 4);

		   /* 24: duration in time units (4 bytes) */
		   data.duration = (int)VideoUtils.round((double)VideoUtils.readBE(stm, 4)
								/ timescale * 1000);

		   /* Skip the rest of the mvhd */
		   stm.seek(blockStart + blockLen);

		   /* Find and parse trak atoms */
		   while(stm.getFilePointer() < stm.length()) {
			   
		      /* Find the next trak atom */
		      blockStart = stm.getFilePointer();
		      blockLen = VideoUtils.readBE(stm, 4);	/* trak (or other atom) length */
		      stm.read(fourcc);
		      if (!"trak".equals(new String(fourcc))) {	/* If it's not a trak atom, skip it */
		    	  if (stm.getFilePointer() < stm.length()) {
		    		  stm.seek(blockStart + blockLen);
		    	  }
		    	  continue;
		      }
		      
		      long subBlockStart = stm.getFilePointer();
		      int subBlockLen = VideoUtils.readBE(stm, 4);	/* tkhd length */
		      stm.read(fourcc);
		      if (!"tkhd".equals(new String(fourcc))) {
		    	  return;
		      }
		      
		      /* Now in the track header */

		      /* 84: width (2 bytes) */
		      stm.seek(subBlockStart + 84);
		      int width = VideoUtils.readBE(stm, 2);
		      
		      /* 88: height (2 bytes) */
		      stm.seek(subBlockStart + 88);
		      int height = VideoUtils.readBE(stm, 2);
		      
		      /* Note on above: Apple's docs say that width/height are 4-byte integers,
		       * but all files I've seen have the data stored in the high-order two
		       * bytes, with the low-order two being 0x0000.  Interpreting it the
		       * "official" way would make width/height be thousands of pixels each.
		       */
		      
		      /* Skip rest of tkhd */
		      stm.seek(subBlockStart + subBlockLen);

		      /* Find mdia atom for this trak */
		      subBlockStart = stm.getFilePointer();
		      subBlockLen = VideoUtils.readBE(stm, 4);
		      stm.read(fourcc);
		      while(!"mdia".equals(new String(fourcc))) {
		    	  
		    	  stm.seek(subBlockStart + subBlockLen);
		    	  subBlockStart = stm.getFilePointer();
		    	  subBlockLen = VideoUtils.readBE(stm, 4);
		    	  stm.read(fourcc);
		      }

		      /* Now we're in the mdia atom; first sub-atom should be mdhd */
		      long subSubBlockStart = stm.getFilePointer();
		      int subSubBlockLen = VideoUtils.readBE(stm, 4);
		      stm.read(fourcc);
		      if (!"mdia".equals(new String(fourcc))) {
		    	  return;
		      }
		      
		      stm.seek(subSubBlockStart + subSubBlockLen);
		      subSubBlockStart = stm.getFilePointer();
		      subSubBlockLen = VideoUtils.readBE(stm, 4);
		      stm.read(fourcc);
		      if (!"hdlr".equals(new String(fourcc))) {
		    	  return;
		      }
		      
		      /* 12: Component type: "mhlr" or "dhlr"; we only care about mhlr,
		       * which should (?) appear first */
		      stm.seek(subSubBlockStart + 12);
		      stm.read(fourcc);
		      if (!"mhlr".equals(new String(fourcc))) {
		    	  return;
		      }
		      stm.read(fourcc);
		      if ("vide".equals(new String(fourcc))) {
		    	  /* This is a video trak */
			 data.height = height;
			 data.width = width;
		      }

		      /* Skip rest of the trak */
		      stm.seek(blockStart + blockLen);
		   } 		
		
	}

}
