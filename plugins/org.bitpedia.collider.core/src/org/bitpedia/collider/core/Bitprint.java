/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: Bitprint.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.core;

import java.util.Arrays;

import org.bitpedia.util.Base32;
import org.bitpedia.util.Sha1;
import org.bitpedia.util.TigerTree;

public class Bitprint {
	
	/* BITPRINT_RAW_LEN defines the length of the bitprint returned by the
	   bitziCreateBitprint function. The bitprint argument needs to have
	   at least BITPRINT_RAW_LEN bytes available. */
	public static final int BITPRINT_RAW_LEN = 44;
	public static final int BITPRINT_BASE32_LEN = 72;
	public static final int SHA_BASE32SIZE = 32;
	public static final int TIGER_BASE32SIZE = 39;
	
	public static final int BUFFER_LEN = 4096;
	
	private static final int ONEK_SIZE = 1025;
	private static final String EMPTY_SHA = "3I42H3S6NNFQ2MSVX7XZKYAYSCX5QBYJ";
	private static final String ONE_SHA = "GVVBSK3ZCOYEYVCXJUMMFDKG4Y4VIKFL";
	private static final String ONEK_SHA = "CAE54LXWDA55NWGAR4PNRX2II7TR66WL";
	private static final String EMPTY_TIGER = "LWPNACQDBZRYXW3VHJVCJ64QBZNGHOHHHZWCLNQ";
	private static final String ONE_TIGER = "QMLU34VTTAIWJQM5RVN4RIQKRM2JWIFZQFDYY3Y";
	private static final String ONEK_TIGER = "CDYY2OW6F6DTGCH3Q6NMSDLSRV7PNMAL3CED3DA"; 	
	
	private Sha1 sha1;
	private TigerTree tt;
	
	/* NOTE: This function returns true if it failed the check! */
	private static boolean checkTigertreeHash(String expected, byte[] data, int len) {

		TigerTree tt = new TigerTree();
		tt.update(data, 0, len);
		String ttDigest = Base32.encode(tt.digest());
		
		return !ttDigest.equals(expected);
	}

	/* NOTE: This function returns true if it failed the check! */
	private static boolean checkSha1Hash(String expected, byte[] data, int len) {
		
		Sha1 sha = new Sha1();
		sha.engineUpdate(data, 0, len);
		String shaDigest = Base32.encode(sha.digest());
		
		return !shaDigest.equals(expected);
	} 	
	
	/* NOTE: This function returns true if it failed the check! */
	private static boolean hashSanityCheck() {
		
		boolean failed = false;
		byte[] data = new byte[] {'1'};
				
		failed = failed || checkTigertreeHash(EMPTY_TIGER, data, 0);
		failed = failed || checkSha1Hash(EMPTY_SHA, data, 0);		
		
		failed = failed || checkTigertreeHash(ONE_TIGER, data, 1);
		failed = failed || checkSha1Hash(ONE_SHA, data, 1);
		
		data = new byte[ONEK_SIZE];
		Arrays.fill(data, (byte)'a');
		failed = failed || checkTigertreeHash(ONEK_TIGER, data, ONEK_SIZE);
		failed = failed || checkSha1Hash(ONEK_SHA, data, ONEK_SIZE);
		
		return failed;
	} 	

	public boolean analyzeInit() {
		
	    if (hashSanityCheck()) {
	        return false;
	    }

	    tt = new TigerTree();
	    sha1 = new Sha1();

	    return true; 		
	}
	
	public void analyzeUpdate(byte[] buf, int ofs, int bufLen) {
		
		tt.update(buf, ofs, bufLen);
		sha1.update(buf, ofs, bufLen);
	}
	
	public byte[] analyzeFinal() {
		
		byte[] ttDigest = tt.digest();
		byte[] sha1Digest = sha1.digest();
		
		byte[] res = new byte[sha1Digest.length+ttDigest.length];
		System.arraycopy(sha1Digest, 0, res, 0, sha1Digest.length);
		System.arraycopy(ttDigest, 0, res, sha1Digest.length, ttDigest.length);
		return res;
	}
	
	public static void main(String[] args) {
		
		if (hashSanityCheck()) {
			System.out.println("Hash test FAILED");
		} else {
			System.out.println("Hash test OK");
		}
		
	}

}
