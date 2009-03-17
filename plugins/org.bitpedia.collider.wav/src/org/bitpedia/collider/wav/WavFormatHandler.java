/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: WavFormatHandler.java,v 1.3 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.wav;

import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

import org.bitpedia.collider.core.FormatHandler;
import org.bitpedia.util.ArrayUtils;
import org.bitpedia.util.Sha1;


public class WavFormatHandler implements FormatHandler {

	private String errorString;

	private Sha1 audioSha1;

	private boolean stereo;

	private int sampleRate, channels, sampleSize;

	private int dataLen;

	private int samples;

	private int bytesProcessed;

	public boolean supportsExtension(String ext) {
		return "wav".equalsIgnoreCase(ext);
	}
	
	public boolean supportsMemAnalyze() {
		return true;
	}

	public boolean supportsFileAnalyze() {
		return false;
	}

	public void analyzeInit() {
		
		audioSha1 = new Sha1();
		stereo = false;
		sampleRate = 0; 
		channels = 0;
		sampleSize = 0;
		dataLen = 0;
		samples = 0;
		bytesProcessed = 0;
	}

	public void analyzeUpdate(byte[] buf, int bufLen) {

		if (-1 == bytesProcessed) {
			return;
		}

		if (0 == bytesProcessed) {
			if (buf[0] != 'R' || buf[1] != 'I' || buf[2] != 'F'
					|| buf[3] != 'F' || buf[8] != 'W' || buf[9] != 'A'
					|| buf[10] != 'V' || buf[11] != 'E' || buf[12] != 'f'
					|| buf[13] != 'm' || buf[14] != 't' || buf[15] != ' ') {
				errorString = "File is not in WAV format.";
				bytesProcessed = -1;
				return;
			}

			/*
			 * We're going to assume that we have the entire header in the first
			 * block
			 */
			assert bufLen >= 44;
			
			ByteBuffer bbuf = ByteBuffer.wrap(buf);
			bbuf.order(ByteOrder.LITTLE_ENDIAN);
			channels = bbuf.getShort(22);
			sampleRate = bbuf.getInt(24);
			sampleSize = bbuf.getShort(34);
			dataLen = bbuf.getInt(40);
			
			if ((8 != sampleSize) && (16 != sampleSize)) {
	           bytesProcessed = -1;
	           errorString = "Invalid sample size found in wav file.";
	           return;
	        }

	        samples = dataLen / (channels * (sampleSize >> 3));
	        
	        audioSha1.engineUpdate(buf, 44, bufLen-44);
	        bytesProcessed += bufLen - 44; 			
		} else {
			audioSha1.engineUpdate(buf, 0, bufLen);
	        bytesProcessed += bufLen; 			
		}

	}
	
	public Map analyzeFinal() {
		
		if (-1 == bytesProcessed) {
			return null;
		}
		
		byte[] hash = audioSha1.engineDigest();
		String value;
		
		Map attrs = new HashMap();
		
	    /* Do a quick check to see which integer math calc is appropriate */
		if (0 < sampleRate) {
		    if (samples < sampleRate) {
		    	value = Integer.toString(samples * 1000 / sampleRate);
		    } else {
		    	value = Integer.toString((samples / sampleRate) * 1000);
		    }
		    attrs.put("tag.wav.duration", value);
		}	    		
	    attrs.put("tag.wav.samplerate", Integer.toString(sampleRate));
	    attrs.put("tag.wav.channels", Integer.toString(channels));
	    attrs.put("tag.wav.samplesize", Integer.toString(sampleSize));
	    value = ArrayUtils.byteArrayToHex(hash, 0, hash.length);
	    attrs.put("tag.wav.audio_sha1", value);
		
		return attrs;
	}

	public static void main(String[] args) {

		try {
			FileInputStream fis = new FileInputStream(
					"c:\\WINDOWS\\Media\\chord.wav");
			byte[] buf = new byte[44];
			fis.read(buf);

			WavFormatHandler wfh = new WavFormatHandler();
			wfh.analyzeUpdate(buf, 44);

			System.out.println("sampleRate: " + wfh.sampleRate + ", channels:"
					+ wfh.channels + ", sampleSize:" + wfh.sampleSize + ", dataLen:"
					+ wfh.dataLen);

			fis.close();
		} catch (Exception e) {

		}

	}

	public Map analyzeFile(String fileName) {
		errorString = "analyzeFile method is not supported.";
		return null;
	}

	public String getError() {
		return errorString;
	}

}
