/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: Submission.java,v 1.3 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.core;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bitpedia.util.ArrayUtils;
import org.bitpedia.util.Base32;

public class Submission {

	private static final String SUBMIT_URL = "http://bitzi.com/lookup/";

	private static final int FIRST_N_HEX = 20;

	private Bitcollider bc;

	private Map attrs = new LinkedHashMap();

	private int numBitprints = 0;

	private String fileName;
	
	private long fileSize;

	private boolean autoSubmit = true;

	private String checkAsExt = null;
	
	private int percentComplete = 0;

	private class Hashes {
		String bitprint;

		String crc32Hex;

		String md5Sum;

		String ed2Kmd4Sum;

		String kzHashSum;

		String firstHex;
		
		Map attrs;
	}

	public Submission(Bitcollider bc, String checkAsExt, boolean autoSubmit) {

		this.bc = bc;
		this.checkAsExt = checkAsExt;
		this.autoSubmit = autoSubmit;
	}

	public boolean isAutoSubmit() {
		return autoSubmit;
	}

	public void setAutoSubmit(boolean autoSubmit) {
		this.autoSubmit = autoSubmit;
	}

	public String getCheckAsExt() {
		return checkAsExt;
	}

	public void setCheckAsExt(String checkAsExt) {
		this.checkAsExt = checkAsExt;
	}

	public String getAttribute(String key) {

		return (String) attrs.get(key);
	}

	public void addAttribute(String key, String value) {

		if (null == value) {
			return;
		}
		
		if (numBitprints > 0) {
			key = numBitprints + "." + key;
		}

		if (attrs.containsKey(key)) {
			return;
		}

		attrs.put(key, value);
	}
	
	private void toMultiple() {
		
		Map newAttrs = new LinkedHashMap();
		
		Iterator iter = attrs.entrySet().iterator();
		while (iter.hasNext()) {
			
			Map.Entry entry = (Map.Entry)iter.next();
			String key = (String)entry.getKey();
			String value = (String)entry.getValue();
			
			if ("head.".equals(key.substring(0, 5))) {
				if ("head.version".equals(key)) {
					value = "M"+value.substring(1);
				}
			} else {
				key = "0."+key;
			}
			
			newAttrs.put(key, value);
		}
		
		attrs = newAttrs;
	}

	private boolean calculateHashes(InputStream stm, Hashes hashes,
			Mp3Handler mp3, FormatHandler fmt) {

		Bitprint bitprint = new Bitprint();
		int crc32 = 0xffffffff;
		Md5Handler md5 = new Md5Handler();
		Ed2Handler ed2 = new Ed2Handler();
		FtuuHandler ftuu = new FtuuHandler();
		KzTreeHandler kz = new KzTreeHandler();

		if (!bitprint.analyzeInit()) {
			bc.setError(Bitcollider.ERROR_HASHCHECK);
			return false;
		}

		if (null != mp3) {
			mp3.analyzeInit();
		}
		
		if ((null != fmt) && fmt.supportsMemAnalyze()) {
			fmt.analyzeInit();
		}
		if (bc.isCalcCrc32()) {
			crc32 = 0xffffffff; //init
		}
		if (bc.isCalcMd5()) {
			md5.analyzeInit();
		}
		ed2.analyzeInit();
		ftuu.analyzeInit();
		kz.analyzeInit();

		byte[] buffer = new byte[Bitprint.BUFFER_LEN];
		long bytesRead = 0;

		percentComplete = 0;
		if ((null != bc.getProgress()) && !bc.isPreview()) {
			bc.getProgress().progress(0, fileName, null);
		}

		for (;;) {
			if (bc.isExitNow()) {
				return false;
			}

			int bytes = 0;
			try {
				bytes = stm.read(buffer);				
			} catch (IOException e) {
				bytes = -1;
				e.printStackTrace();
			}
			if (bytes <= 0) {
				break;
			}
			
			bytesRead += bytes;

			bitprint.analyzeUpdate(buffer, 0, bytes);
			if (null != mp3) {
				mp3.analyzeUpdate(buffer, 0, bytes);
			}
			if ((null != fmt) && fmt.supportsMemAnalyze()) {
				fmt.analyzeUpdate(buffer, bytes);
			}

			if (bc.isCalcCrc32()) {
				crc32 = FtuuHandler.hashSmallHash(buffer, 0, bytes, crc32);
			}
			if (bc.isCalcMd5()) {
				md5.analyzeUpdate(buffer, bytes);
			}
			
			ed2.analyzeUpdate(buffer, 0, bytes);
			ftuu.analyzeUpdate(buffer, 0, bytes);
			kz.analyzeUpdate(buffer, 0, bytes);
			
			if ((null != bc.getProgress()) && !bc.isPreview()) {
				int percent = (int)((bytesRead * 100) / fileSize);
				if (percent != percentComplete) {
					bc.getProgress().progress(percent, null, null);
					percentComplete = percent;
				}
			}
		}
		percentComplete = 100;

		byte[] bitprintRaw = bitprint.analyzeFinal();
		String tmp = Base32.encode(bitprintRaw);
		hashes.bitprint = tmp.substring(0, Bitprint.SHA_BASE32SIZE)+'.'+tmp.substring(Bitprint.SHA_BASE32SIZE);
		

		if (null != mp3) {
			mp3.analyzeFinal();
		}
		
		if ((null != fmt) && fmt.supportsMemAnalyze()) {
			hashes.attrs = fmt.analyzeFinal();
		}
		
		if (bc.isCalcCrc32()) {
			crc32 = ~crc32;
			hashes.crc32Hex = Integer.toHexString(crc32);
			while (hashes.crc32Hex.length() < 8) {
				hashes.crc32Hex = '0'+hashes.crc32Hex;
			}
		}
		
		if (bc.isCalcMd5()) {
			byte[] md5Digest = md5.analyzeFinal();
			hashes.md5Sum = ArrayUtils.byteArrayToHex(md5Digest, 0, md5Digest.length);
		}

		byte[] ed2Digest = ed2.analyzeFinal();
		byte[] ftuuDigest = ftuu.analyzeFinal();
		byte[] kzDigest = kz.analyzeFinal();

		hashes.ed2Kmd4Sum = ArrayUtils.byteArrayToHex(ed2Digest, 0,
				ed2Digest.length);
		hashes.kzHashSum = ArrayUtils.byteArrayToHex(ftuuDigest, 0,
				ftuuDigest.length)
				+ ArrayUtils.byteArrayToHex(kzDigest, 0, kzDigest.length);
		
		return true;
	}

	private String generateFirstHex(InputStream stm, int n) {

		byte[] buf = new byte[n];
		try {
			n = stm.read(buf);
			if (n < 0) {
				return "";
			}

			return ArrayUtils.byteArrayToHex(buf, 0, n);

		} catch (IOException e) {
			return null;
		}
	}

	public boolean getBitprintData(String fileName, Hashes hashes,
			Mp3Handler mp3, FormatHandler fmt) {

		boolean ret = false;

		try {
			File file = new File(fileName);
			fileSize = file.length();
			InputStream stm = new FileInputStream(file);
			try {
				ret = calculateHashes(stm, hashes, mp3, fmt);
			} finally {
				try {
					stm.close();
				} catch (IOException e) {
				}
			}
			if (ret) {
				stm = new FileInputStream(fileName);
				try {
					hashes.firstHex = generateFirstHex(stm, FIRST_N_HEX);
					ret = null != hashes.firstHex;
				} finally {
					try {
						stm.close();
					} catch (IOException e) {
					}
				}
			}
			return ret;

		} catch (FileNotFoundException e) {
			bc.setError(Bitcollider.ERROR_FILENOTFOUND);
			return false;
		}
	}
	
	public static String extractName(String fileName) {

		int sepPos = fileName.lastIndexOf(File.separatorChar);
		if (-1 != sepPos) {
			fileName = fileName.substring(sepPos + 1);
		}
		return fileName;
	}

	public static String extractExt(String fileName) {

		fileName = extractName(fileName); 

		String ext = "";
		int extPos = fileName.lastIndexOf('.');
		if (-1 != extPos) {
			ext = fileName.substring(extPos + 1);
		}

		return ext;
	}

	public boolean analyzeFile(String fileName, boolean matchingExtsOnly) {

		bc.setError(null);
		bc.setWarning(null);

		this.fileName = fileName;

		if (bc.isExitNow()) {
			return false;
		}

		String ext = extractExt(fileName);
		boolean mp3Check = (null == checkAsExt) && ("mp3".equalsIgnoreCase(ext));
		ext = null == checkAsExt ? ext : checkAsExt;

		FormatHandler fmtHandler = bc.getFormatHandler(ext);

		/*
		 * If we're only supposed to work on files with known extensions, and we
		 * don't know this extension, bail.
		 */
		if (matchingExtsOnly && (null == fmtHandler) && !mp3Check) {
			if ((null != bc.getProgress()) && !bc.isPreview()) {
				bc.getProgress().progress(0, fileName, "skipped.");
			}

			return false;
		}

		/* If we're in preview mode, return now */
		if (bc.isPreview()) {
			numBitprints++;
			return true;
		}

		Mp3Handler mp3 = null;
		if (mp3Check) {
			mp3 = new Mp3Handler();
		}

		Hashes hashes = new Hashes();
		if (!getBitprintData(fileName, hashes, mp3, fmtHandler)) {
			return false;
		}

		/* If this is the first bit print, add a header to the attrs */ 
		if (0 == numBitprints) {
			addAttribute("head.agent", Bitcollider.getAgentString());
			addAttribute("head.version", "S"+Bitcollider.BC_SUBMITSPECVER);
		}
		
		/* If this is the second bitprint, convert the single submission
	       to a multiple submission */
		if (1 == numBitprints) {
			toMultiple();
		}

		addAttribute("bitprint", hashes.bitprint);

		addAttribute("tag.file.length", ""+(new File(fileName).length()));
		addAttribute("tag.file.first20", hashes.firstHex);
		addAttribute("tag.filename.filename", extractName(fileName));

	    if (bc.isCalcCrc32()) {
	    	addAttribute("tag.crc32.crc32", hashes.crc32Hex);
		}
	    if (bc.isCalcMd5()) {
	    	addAttribute("tag.md5.md5", hashes.md5Sum);
	    }

	    addAttribute("tag.ed2k.ed2khash", hashes.ed2Kmd4Sum);
	    addAttribute("tag.kzhash.kzhash", hashes.kzHashSum);

	    /* Check to make sure that we carried out the mp3 check, and
	       make sure that an audioSha was generated. If not, then the
	       mp3 routines deemed that this was not a valid mp3 file
	       and we should skip the mp3 tag generation */
	    if (mp3Check && (0 == mp3.sampleRate)) {
	    	bc.setWarning(Bitcollider.WARNING_NOTMP3);
	    	mp3Check = false;
	    } else {
	    	if (mp3Check) {
	    		addAttribute("tag.mp3.duration", ""+mp3.duration);
	    		if (0 == mp3.bitRate) {
	    			addAttribute("tag.mp3.bitrate", ""+mp3.avgBitRate);
	    			addAttribute("tag.mp3.vbr", "y");
	    		} else {
	    			addAttribute("tag.mp3.bitrate", ""+mp3.bitRate);
	    		}
	    		
	    		addAttribute("tag.mp3.samplerate", ""+mp3.sampleRate);
	    		addAttribute("tag.mp3.stereo", mp3.stereo ? "y" : "n");
	    		addAttribute("tag.mp3.audio_sha1", Base32.encode(mp3.audioSha));
	    		
	    		Id3Handler.Id3Info info = Id3Handler.readId3Tags(fileName);
	    		if (null != info) {
	    			addAttribute("tag.mp3.encoder", info.encoder);
	    			addAttribute("tag.audiotrack.title", info.title);
		            addAttribute("tag.audiotrack.artist", info.artist);
	 	            addAttribute("tag.audiotrack.album", info.album);
	 	            addAttribute("tag.audiotrack.tracknumber", info.trackNumber);
	 	            addAttribute("tag.id3genre.genre", info.genre);
	 	            addAttribute("tag.audiotrack.year", info.year);
	    		}
	       }
	    }
	    
	    if (null != hashes.attrs) {
    		Iterator iter = hashes.attrs.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry entry = (Map.Entry) iter.next();
				addAttribute((String) entry.getKey(), (String) entry
						.getValue());
			}
	    }

	    /* If a plugin was selected, but no memory analyze functions were
	       provided, call the plugin's file analyze methods */
	    if ((null != fmtHandler) && !fmtHandler.supportsMemAnalyze() &&
	    		fmtHandler.supportsFileAnalyze() && !bc.isExitNow()) {
	    	
	    	Map fileAttrs = fmtHandler.analyzeFile(fileName);
	    	if ((null != fileAttrs) && (0 < fileAttrs.size())) {
	    		Iterator iter = fileAttrs.entrySet().iterator();
				while (iter.hasNext()) {
					Map.Entry entry = (Map.Entry) iter.next();
					addAttribute((String) entry.getKey(), (String) entry
							.getValue());
				}
	    	} else {
	    	    /* If we selected a plugin, but the no attributes were returned,
	    	     * then check for an error from the plugin. An error from a plugin 
	    	     * should be considered a warning since its not fatal to the execution 
	    	     * of the bitcollider */
	    		bc.setWarning(fmtHandler.getError());
	    	}
	    }
	    
	    if ((null != bc.getProgress()) && !bc.isPreview() && !bc.isExitNow()) {
	    	bc.getProgress().progress(100, null, "ok.");
	    }
	    
	    numBitprints++; 		

		return true;
	}
	
	public int recurseDir(String path, boolean analyzeAll, boolean recurseDeep) {
		
		File dir = new File(path);
		File[] files = dir.listFiles();		
		int count = 0;
		for (int i = 0; i < files.length; i++) {
			
			if (files[i].isFile()) {
				if (analyzeFile(files[i].getPath(), !analyzeAll)) {
					count++;
				}
			} else if (files[i].isDirectory()) {
				if (recurseDeep) {
					count += recurseDir(files[i].getPath(), analyzeAll,
							recurseDeep);
				}
			} else {
				if (null != bc.getProgress()) {
					bc.getProgress().progress(0, files[i].getName(),
							"skipped. (not a regular file)");
				}
			}
			
		}
		
		return count;
	}

	private static String toEscaped(String value) {

		int valueLength = value.length();
		int extraLength = 0;

		for (int i = 0; i < valueLength; i++) {

			switch (value.charAt(i)) {
			case '"':
				extraLength += 5;
				break;
			case '&':
				extraLength += 4;
				break;
			case '<':
			case '>':
				extraLength += 3;
				break;
			}
		}

		if (0 == extraLength) {
			return value;
		}

		StringBuffer escaped = new StringBuffer();
		for (int i = 0; i < valueLength; i++) {
			switch (value.charAt(i)) {
			case '"':
				escaped.append("&quot;");
				break;
			case '&':
				escaped.append("&amp;");
				break;
			case '<':
				escaped.append("&lt;");
				break;
			case '>':
				escaped.append("&gt;");
				break;
			default:
				escaped.append(value.charAt(i));
			}
		}

		return escaped.toString();
	}

	public boolean makeHtml(PrintWriter dest, String url) {

		if (0 == numBitprints) {
			bc.setError("The submission contained no bitprints.");
			return false;
		}

		url = (null == url) ? SUBMIT_URL : url;

		dest.println("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">");
		dest.print("<HTML><HEAD><TITLE>");

		if (1 == numBitprints) {
			dest.println("Bitprint Submission " + fileName);
		} else {
			dest.println("Multiple [" + numBitprints + "] Bitprint Submission");
		}
		dest.println("</TITLE>\n</HEAD>");
		if (autoSubmit) {
			dest.println("<BODY onLoad=\"document.forms[0].submit()\">");
		} else {
			dest.println("<BODY>");
		}

		if (1 == numBitprints) {
			dest.println("<h3>Bitprint Submission " + fileName + "</h3><p>");
		} else {
			dest.println("<h3>Multiple [" + numBitprints
					+ "] Bitprint Submission</h3><p>");
		}

		dest
				.println("You are submitting the following bitprint and tag data to the web "
						+ "location <i>"
						+ url
						+ "</i>. For more information see <a "
						+ "href=\"http://bitzi.com/bitcollider/websubmit\">the Bitzi website.</a>"
						+ "<p>\nIf you are submitting more than a handful of files at once, it "
						+ "may take a while for this page to load and submit.<p>\n"
						+ "This submission should occur automatically. If it does not, you "
						+ "may press the \"submit\" button which will appear at the bottom of "
						+ "the page.<p><HR>");

		dest.println("<FORM method=post action=\"" + url + "\">");
		dest.println("<PRE>");

		int i = 0, attrInd = -1, lastAttrInd = -1;
		Iterator iter = attrs.keySet().iterator();
		while (iter.hasNext()) {

			String key = (String) iter.next();
			String value = (String) attrs.get(key);

			try {
				attrInd = Integer.parseInt(key.substring(0, key.indexOf(".")));
			} catch (Throwable t) {
				attrInd = -1;
			}
			
			if ((attrInd != lastAttrInd) || (2 == i)) {
				lastAttrInd = attrInd;
				dest.println();
			}
			i++;

			dest.print(key + "=<INPUT TYPE=\"hidden\" ");			
			String escaped = toEscaped(value);
			dest.println("NAME=\"" + key + "\" VALUE=\"" + escaped + "\">"
					+ value);
		}
		dest
				.println("\n<INPUT TYPE=\"submit\" NAME=\"Submit\" VALUE=\"Submit\">");
		dest.println("</PRE>\n</FORM>\n</BODY>\n</HTML>");

		return true;
	}

	public int getNumBitprints() {
		return numBitprints;
	}

}
