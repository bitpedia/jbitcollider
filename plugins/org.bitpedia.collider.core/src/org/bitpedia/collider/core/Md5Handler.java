/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: Md5Handler.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.core;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Md5Handler  {
	
	private MessageDigest digest;

	public void analyzeInit() {
		try {
			digest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			// Never happens - MD5 always exists
		}
	}
	
	public void analyzeUpdate(byte[] buf, int bufLen) {
		
		digest.update(buf, 0, bufLen);
	}
	
	public void analyzeUpdate(byte[] buf, int ofs, int bufLen) {
		
		digest.update(buf, ofs, bufLen);
	}

	public byte[] analyzeFinal() {
		
		return digest.digest();
	}

	public static byte[] md5(byte[] buffer, int len) {
		
		Md5Handler md5Handler = new Md5Handler();
		md5Handler.analyzeInit();
		md5Handler.analyzeUpdate(buffer, len);
		return md5Handler.analyzeFinal();
		
	}
	
	public static byte[] md5(byte[] buffer, int ofs, int len) {
		
		Md5Handler md5Handler = new Md5Handler();
		md5Handler.analyzeInit();
		md5Handler.analyzeUpdate(buffer, ofs, len);
		return md5Handler.analyzeFinal();
		
	}
}
