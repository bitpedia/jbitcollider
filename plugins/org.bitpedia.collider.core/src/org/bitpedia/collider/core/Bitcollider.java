/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: Bitcollider.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.core;

import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class Bitcollider {
	
	public static final String BC_AGENTNAME = "jBitprinter";

	/* You may want to change this to a build identifier of your own, instead
	   of using a timestamp */
	public static final String BC_AGENTBUILD = "03/16/2006 18:15";

	/* This indicates the version of the official submission spec */
	public static final String BC_SUBMITSPECVER = "0.4";

	/* Your agent-version string; should be #[.#[.#[etc]]] format */
	public static final String BC_VERSION = "0.6.0";
	
	public static final String getAgentString() {
		return Bitcollider.BC_AGENTNAME + "/" + Bitcollider.BC_VERSION + " ("
				+ Bitcollider.BC_AGENTBUILD + ")";
	}
	
	public static final String ERROR_FILENOTFOUND = "File not found or permission denied.";
	public static final String ERROR_MALLOCFAILED = "Failed to allocate memory.";
	public static final String ERROR_LAUNCHBROWSER = "Cannot launch web browser.";
	public static final String ERROR_TEMPFILEERR = "Cannot create a temorary file for the bitprint submission.";
	public static final String ERROR_HASHCHECK = "The hash functions compiled into this version of the bitcollider utility are faulty!!!";
	public static final String WARNING_NOTMP3 = "This is not an MP3 file. Skipping mp3 information."; 

	public interface Progress {
		public void progress(int percent, String fileName, String message);
	}

	private Collection fmtHandlers;

	private boolean calcMd5 = false;
	private boolean calcCrc32 = false;
	
	private Progress progress;
	
	private String error;
	private String warning;
	private boolean preview;
	private boolean exitNow;

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}

	public boolean isExitNow() {
		return exitNow;
	}

	public void setExitNow(boolean exitNow) {
		this.exitNow = exitNow;
	}

	public boolean isPreview() {
		return preview;
	}

	public void setPreview(boolean preview) {
		this.preview = preview;
	}

	public String getWarning() {
		return warning;
	}

	public void setWarning(String warning) {
		this.warning = warning;
	}

	public Bitcollider(Collection fmtHandlers) {

		this.fmtHandlers = fmtHandlers;
	}
	
	public FormatHandler getFormatHandler(String ext) {
		
		Iterator iter = fmtHandlers.iterator();
		while (iter.hasNext()) {
			FormatHandler fh = (FormatHandler)iter.next();
			if (fh.supportsExtension(ext)) {
				return fh;
			}
		}
		
		return null;		
	}

	public Submission generateSubmission(List fileList, String asExt,
			boolean autoSubmit) {
		
		Submission submission = new Submission(this, asExt, autoSubmit); 

		Iterator iter = fileList.iterator();
		while (iter.hasNext()) {
			String fileName = iter.next().toString();
			File file = new File(fileName);
			if (!file.exists()) {
				System.err.println("Cannot find file/dir "+fileName+". Skipping.");
				continue;
			} else if (file.isFile()) {
				
				boolean ret = submission.analyzeFile(fileName, false);
				if (!ret) {
					// fprintf(stderr, "%s problem: %s\n", fileName, get_error(bc));
					continue;
				}
			} else if (file.isDirectory()) {
				
				submission.recurseDir(fileName, false, false); 
			} else {
				System.err.println(fileName+" is not a regular file. Skipping.");
				continue;
			}
			
			//TODO print_warning(bc);
		}

		return submission;
	}

	public boolean isCalcCrc32() {
		return calcCrc32;
	}

	public void setCalcCrc32(boolean calcCrc32) {
		this.calcCrc32 = calcCrc32;
	}

	public boolean isCalcMd5() {
		return calcMd5;
	}

	public void setCalcMd5(boolean calcMd5) {
		this.calcMd5 = calcMd5;
	}

	public Progress getProgress() {
		return progress;
	}

	public void setProgress(Progress progress) {
		this.progress = progress;
	}

}
