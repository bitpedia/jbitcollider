/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: ArrayUtils.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.util;

public class ArrayUtils {
	
	public static String byteToHex(byte b) {
		
		return Integer.toString((b & 0xFF) + 0x100, 16).substring(1);
	}
	
	public static String byteArrayToHex(byte[] b, int offset, int len) {
		
		StringBuffer buf = new StringBuffer(); 
		
		for (int i = offset; i < offset+len; i++) {
			buf.append(byteToHex(b[i]));
		}
		
		return buf.toString();
	}
	

}
