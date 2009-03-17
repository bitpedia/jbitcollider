/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: Mp3Handler.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.core;

import org.bitpedia.util.Sha1;

public class Mp3Handler {
	
	private static final int[] mpeg1Bitrates = { 0, 32, 40, 48, 56, 64, 80, 96, 112, 
        128, 160, 192, 224, 256, 320 };
	private static final int[] mpeg2Bitrates = { 0, 8, 16, 24, 32, 40, 48, 56, 64, 
        80, 96, 112, 128, 144, 160 };
	private static final int[] mpeg1SampleRates = { 44100, 48000, 32000 };
	private static final int[] mpeg2SampleRates = { 22050, 24000, 16000 };
	private static final int[] mpegLayer = { 0, 3, 2, 1 };
	
	public static final int ID3_TAG_LEN = 128; 
	
    public int bitRate;
    public int sampleRate;
    public boolean stereo;
    public int duration;
    public byte[] audioSha;
    public int frames;
    public int mpegVer;
    public int avgBitRate;
    
    private int skipSize;
    private byte[] spanningHeader;
    private int spanningSize;
    private Sha1 sha;
    private int goodBytes, badBytes;
    private byte[] startBuffer;
    private int startBytes;
    private byte[] audioShaBuffer;
    private byte[] audioShaExtra;
    private int audioShaBytes;
    
    private static int extractBitRate(byte[] header, int ofs) {

        int id = header[ofs+1] >= 0 ? header[ofs+1] : header[ofs+1]+256;
        int br = header[ofs+2] >= 0 ? header[ofs+2] : header[ofs+2]+256;
        
        id = (id & 0x8) >> 3;
        br = (br & 0xF0) >> 4;

        if (0 != id) {
        	if (br < mpeg1Bitrates.length) {
        		return mpeg1Bitrates[br];
        	}
        } else {
        	if (br < mpeg2Bitrates.length) {
        		return mpeg2Bitrates[br];
        	}
        }
        
        return 0;
    }

    private static int extractSampleRate(byte[] header, int ofs) {

        int id = header[ofs+1] >= 0 ? header[ofs+1] : header[ofs+1]+256;
        int sr = header[ofs+2] >= 0 ? header[ofs+2] : header[ofs+2]+256;

        id = (id & 0x8) >> 3;
        sr = (sr >> 2) & 0x3;
        
        if (0 != id) {
        	if (sr < mpeg1SampleRates.length) {
        		return mpeg1SampleRates[sr];
        	}
        } else {
        	if (sr < mpeg2SampleRates.length) {
        		return mpeg2SampleRates[sr];
        	}
        }
        
        return 0;
    }

    private static boolean extractStereo(byte[] header, int ofs) {
    	
    	int b = header[ofs+3] >= 0 ? header[ofs+3] : header[ofs+3]+256;
    	return 3 != ((b & 0xc0) >> 6);
    }

    private static int extractMpegVer(byte[] header, int ofs) {
    	
    	int b = header[ofs+1] >= 0 ? header[ofs+1] : header[ofs+1]+256;
    	if (0 == ((b & 0x8) >> 3)) {
    		return 2;
    	} else {
    		return 1;
    	}
    }

    private static int extractMpegLayer(byte[] header, int ofs) {
    	
    	int b = header[ofs+1] >= 0 ? header[ofs+1] : header[ofs+1];
    	return mpegLayer[((b & 0x7) >> 1)]; 
    }

    private static int extractPadding(byte[] header, int ofs) {
    	
    	int b = header[ofs+2] >= 0 ? header[ofs+2] : header[ofs+2]+256;
    	return (b >> 1) & 0x1;
    }
    
    private int findStart(byte[] buffer, int ofs, int len) {
    	
    	int goodFrames = 0, goodFrameOffset = -1;

        if (null != startBuffer) {
        	
        	byte[] newBuffer = new byte[startBytes+len];
        	System.arraycopy(startBuffer, 0, newBuffer, 0, startBytes);
        	System.arraycopy(buffer, ofs, newBuffer, startBytes, len);
        	startBuffer = newBuffer;
        	startBytes += len;
        	buffer = startBuffer;
        	len = startBytes;
        	ofs = 0;
        }

        /* Loop through the buffer trying to find frames */
        int max = len;
        for(int i = 0; i < max-1; ) {
        	/* Find the frame marker */
        	int bi = buffer[i+ofs] >= 0 ? buffer[i+ofs] : buffer[i+ofs]+256;
        	int bi1 = buffer[i+ofs+1] >= 0 ? buffer[i+ofs+1] : buffer[i+ofs+1]+256;
        	if ((0xFF != bi) || ((0xF0 != (bi1 & 0xF0)) && (0xE0 != (bi1 & 0xF0)))) {
        		i++;
        		continue;
        	}
        	
        	/* Extract sample rate and layer from this first frame */
        	int firstSampleRate = extractSampleRate(buffer, i+ofs);
        	int firstLayer = extractMpegLayer(buffer, i+ofs);

        	/* Check for invalid sample rates */
        	if (0 == firstSampleRate) { 
        		i++;
        		continue;
        	}

        	/* Calculate the size of the frame from the header components */
        	int br = extractBitRate(buffer, i+ofs); 
        	int sr = extractSampleRate(buffer, i+ofs); 
        	int pd = extractPadding(buffer, i+ofs);
        	int size;
        	if (1 == extractMpegVer(buffer, i+ofs)) {
        		size = (144000 * br) / sr + pd;
        	} else {
        		size = (72000 * br) / sr + pd;
        	}
        	
        	if ((size <= 1) || (2048 < size)) {
        		i++;
        		continue;
        	}

        	if (max <= i + size) {
        		if (null == startBuffer) {
        			startBytes = len;
        			startBuffer = new byte[len];
        			System.arraycopy(buffer, ofs, startBuffer, 0, len);
        		}
        		return -1; 
        	}

        	/* now we have what seems to be a valid size. Let's see if there
        	 is a new frame with the right layer and sample rate right after
        	 this potential frame */
        	int secondSampleRate = extractSampleRate(buffer, i+ofs+size);
        	int secondLayer = extractMpegLayer(buffer, i+ofs+size);

        	if ((firstSampleRate == secondSampleRate) && (firstLayer == secondLayer)) {
        		goodFrames++;
        		if (goodFrameOffset < 0) {
        			goodFrameOffset = i;
        		}
        		i += size;
        	} else {
        		goodFrames = 0;

        		if (goodFrameOffset >= 0) {
        			i = goodFrameOffset + 1;
        		} else {
        			i++;
        		}
        		goodFrameOffset = -1;
        	}

        	if (3 == goodFrames) {
        		return goodFrameOffset;
        	}
        }

        return -1;
    }     
    
    private void resetValues() {
    	
        bitRate = 0;
        sampleRate = 0;
        stereo = false;
        duration = 0;
        audioSha = null;
        frames = 0;
        mpegVer = 0;
        avgBitRate = 0;
        
        skipSize = 0;
        spanningHeader = new byte[3];
        spanningSize = 0;
        sha = new Sha1();
        goodBytes = 0; 
        badBytes = 0;
        startBuffer = null;
        startBytes = 0;
        audioShaBuffer = null;
        audioShaExtra = new byte[3];
        audioShaBytes = 0;
    }
    
	public void analyzeInit() {
		
		resetValues();
	}
	
	public void analyzeUpdate(byte[] buffer, int len) {
		
		analyzeUpdate(buffer, 0, len);
	}
	
	public void analyzeUpdate(byte[] buffer, int ofs, int len) {
		
		int size = 0;
		byte[] temp = null;

		/* If this is the first time in the update function, then seek to
		   find the actual start of the mp3 and skip over any ID3 tags or garbage
		   that might be at the beginning of the file */
		if ((0 == badBytes) && (0 == goodBytes)) {
			
			int offset = findStart(buffer, ofs, len);
		    if (offset < 0) {
		    	return;
		    }

		    /* If it took more than one block to determine the start of the mp3
		       file, then use the buffer that was created by the find_mp3_start
		       routine, rather than the buffer that was passed in. */
		    if (null != startBuffer) {
		        buffer = startBuffer;
		        len = startBytes;
		        ofs = 0;
		    }

		    /* Skip over the crap at the beginning of the file */
		    ofs += offset; 
		    len -= offset;
		    size = 0;
		}

		/* If the a header spanned the last block and this block, then
		   allocate a larger buffer and copy the last header plus the new
		   block into the new buffer and work on it. This shouldn't happen
		   very often. */
		if (0 < spanningSize) {
		      temp = new byte[len + spanningSize];
		      System.arraycopy(spanningHeader, 0, temp, 0, spanningSize);
		      System.arraycopy(buffer, ofs, temp, spanningSize, len);
		      len += spanningSize;
		      buffer = temp;
		      ofs = 0;
		}

		/* Pass the bytes we're skipping through the sha function */
		updateAudioSha1(buffer, ofs, skipSize); 
		
		/* Save the three bytes immediately following the last audio sha
		   block for later. These bytes will be used to check for ID3
		   tags at the end of truncated audio frames. See mp3_final for
		   more details. */
		System.arraycopy(buffer, ofs+skipSize, audioShaExtra, 0, 3);

		/* Loop through the buffer trying to find frames */
		int i = ofs + skipSize, max = ofs+len; 
		while (i < max) {
			
		    if ((max - i) < 4) {
		    	
		        /* If we have a header that spans a block boundary, save
		           up to 3 bytes and then return */
		        spanningSize = max - i;
		        System.arraycopy(buffer, i, spanningHeader, 0, spanningSize);
		        skipSize = 0;
		        
		        temp = null;

		         return;
		    }
		 
		      /* Find the frame marker */
		    int bi = buffer[i] >= 0 ? buffer[i] : buffer[i]+256;
		    int bi1 = buffer[i+1] >= 0 ? buffer[i+1] : buffer[i+1]+256;
		    if ((bi != 0xFF) || (((bi1 & 0xF0) != 0xF0) && ((bi1 & 0xF0) != 0xE0))) {
		    	  
		          badBytes++;
		          i++;
		          continue;
		      }

	          int sr = extractSampleRate(buffer, i);
		      /* Check for invalid sample rates */
		      if (0 == sr) {
		    	  
		          badBytes++;
		          i++;
		          continue;
		      }
		      
	          int br = extractBitRate(buffer, i); 
	          int pd = extractPadding(buffer, i);
	          int mv = extractMpegVer(buffer, i);
		      /* Calculate the size of the frame from the header components */
		      if (1 == mv) {
		          size = (144000 * br) / sr + pd;
		      } else {
		          size = (72000 * br) / sr + pd;
		      }
		      if ((size <= 1) || (2048 < size)) {
		    	  
		          badBytes++;
		          i++;
		          continue;
		      }

		      /* If this is the first frame, then tuck away important info */
		      if (0 == frames) {
		    	  
		          sampleRate = sr;
		          bitRate = br;
		          mpegVer = mv;
		          stereo = extractStereo(buffer, i);
		      } else {
		          /* The sample rate inside of a file should never change. If the
		             header says it did, then assume that we found a bad header 
		             and skip past it. */
		          if (sampleRate != sr) {
		        	  
		             badBytes++;
		             i++;
		             continue;
		          }

		          /* If the bitrate in subsequent frames is different from the
		             first frame, then we have a VBR file */
		          if ((0 != bitRate) && (bitRate != br)) {
		             bitRate = 0;
		          }
		      }

		      /* Update the sha hash with the data from this frame */
		      int bytesLeft = max - i;
		      int frameSize = (size > bytesLeft) ? bytesLeft : size;
		      updateAudioSha1(buffer, i, frameSize);
		      
		      /* save the first three bytes after the audio sha block (see above) */
		      if (i+frameSize+3 < buffer.length) {
		    	  System.arraycopy(buffer, i+frameSize, audioShaExtra, 0, 3);
		      }

		      /* Move the memory pointer past the frame */
		      frames++;
		      goodBytes += size;
		      avgBitRate += br;
		      i += size;
		   }

		   /* skipSize defines the number of bytes to skip in the next block,
		      so that we're not searching for the frame marker inside of
		      a frame, which can lead to false hits. Grrr. 
		      Vielen Dank, Karl-Heinz Brandenburg! */
		   skipSize = i - max;
		   spanningSize = 0;
		   temp = null;
	}
	
	public void analyzeFinal() {
		
		startBuffer = null;

		/* If there are more bad bytes in a file, than there are good bytes,
			assume that the file is not an MP3 file and zero out all the values
			we've collected. Unfortunately there is no good way to detecting
			whether or not a file really is an MP3 file */
		if ((goodBytes < badBytes) || (0 == goodBytes)) {
			resetValues();
		} else {
			if (null != audioShaBuffer) {
				
			    /* Copy the last three characters from after the last audioSha
			       block to the end of the sliding window. Then, look for the
			       TAG and skip it if it was found. */
			    System.arraycopy(audioShaExtra, 0, audioShaBuffer, ID3_TAG_LEN, 3);
			    int i;
			    for (i = 0; i < ID3_TAG_LEN; i++) {
			    	
			        if ("TAG".equals(new String(audioShaBuffer, i, 3))) {
			        	break;
			        }
			    }

			    if (ID3_TAG_LEN < i) {
			    	i = ID3_TAG_LEN;
			    }
			    
			    sha.engineUpdate(audioShaBuffer, 0, i);
			}
			
			audioSha = sha.engineDigest();

			if (1 == mpegVer) {
				duration = frames * 1152 / (sampleRate / 1000);
			} else {
				duration = frames * 576 / (sampleRate / 1000);
			}
			avgBitRate /= frames;
		}
	}
	
	public void updateAudioSha1(byte[] buf, int ofs, int bufLen) {
		
		/* Allocate the space for the audiosha sliding window. Allocate three
		   extra bytes to allow for the possibility that the ID3 tag spans
		   the outer boundary of the audiosha sliding window */
		if (null == audioShaBuffer) {
			audioShaBuffer = new byte[ID3_TAG_LEN + 3];
		}

		/* Save the last 128 bytes of the given buffer and audio sha all the
		   bytes passed through the sliding window */
		if (ID3_TAG_LEN < bufLen + audioShaBytes) {
			if (ID3_TAG_LEN <= bufLen) {
				sha.engineUpdate(audioShaBuffer, 0, audioShaBytes);
				sha.engineUpdate(buf, ofs, bufLen - ID3_TAG_LEN);
				System.arraycopy(buf,  ofs+bufLen - ID3_TAG_LEN, audioShaBuffer, 0, ID3_TAG_LEN);
				audioShaBytes = ID3_TAG_LEN;
			} else {
				int bytesToRemove = audioShaBytes + bufLen - ID3_TAG_LEN;
				sha.engineUpdate(audioShaBuffer, 0, bytesToRemove);
				System.arraycopy(audioShaBuffer, bytesToRemove, audioShaBuffer, 0, audioShaBytes - bytesToRemove);
				System.arraycopy(buf, ofs, audioShaBuffer, audioShaBytes - bytesToRemove, bufLen);
				audioShaBytes = audioShaBytes - bytesToRemove + bufLen;
			}
		} else {
			System.arraycopy(buf, ofs, audioShaBuffer, audioShaBytes, bufLen);
			audioShaBytes += bufLen;
		}
	} 	

}
