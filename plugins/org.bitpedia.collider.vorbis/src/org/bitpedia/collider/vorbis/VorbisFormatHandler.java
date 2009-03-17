/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: VorbisFormatHandler.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.vorbis;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;

import javazoom.spi.vorbis.sampled.file.VorbisAudioFileReader;

import org.bitpedia.collider.core.FormatHandler;

public class VorbisFormatHandler implements FormatHandler {

	private String errorString;

	public boolean supportsExtension(String ext) {
		return "ogg".equalsIgnoreCase(ext);
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
	
	private void extractParams(Map src, Map dest) {
		
		Object value = src.get("ogg.bitrate.nominal.bps");
		if (null != value) {
			dest.put("tag.vorbis.bitrate", value.toString());
		}
		
		value = src.get("duration");
		if (null != value) {
			dest.put("tag.vorbis.duration", value.toString());
		}
		
		value = src.get("ogg.frequency.hz");
		if (null != value) {
			dest.put("tag.vorbis.samplerate", value.toString());
		}		
		
		value = src.get("ogg.channels");
		if (null != value) {
			dest.put("tag.vorbis.channels", value.toString());
		}		
		
		value = src.get("title");
		if (null != value) {
			dest.put("tag.audiotrack.title", value.toString());
		}		
		
		value = src.get("author");
		if (null != value) {
			dest.put("tag.audiotrack.artist", value.toString());
		}		
		
		value = src.get("album");
		if (null != value) {
			dest.put("tag.audiotrack.album", value.toString());
		}		
		
		value = src.get("ogg.comment.track");
		if (null != value) {
			dest.put("tag.audiotrack.tracknumber", value.toString());
		}		

		value = src.get("comment");
		if (null != value) {
			dest.put("tag.objective.description", value.toString());
		}		

		value = src.get("ogg.comment.genre");
		if (null != value) {
			dest.put("tag.id3genre.genre", value.toString());
		}		
	}

	public Map analyzeFile(String fileName) {

		Map res = null;
		
		try {
			VorbisAudioFileReader vafr = new VorbisAudioFileReader();
			
			AudioFileFormat baseFileFormat = vafr.getAudioFileFormat(new File(fileName));
			AudioFormat baseFormat = baseFileFormat.getFormat();
			
			res = new LinkedHashMap();
			extractParams(baseFileFormat.properties(), res);
			extractParams(baseFormat.properties(), res);		
		} catch (Exception e) {
			errorString = e.getMessage();
		}

		return res;
	}

	public String getError() {
		return errorString;
	}

}
