/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: FormatHandler.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.core;

import java.util.Map;

public interface FormatHandler {
	
	public boolean supportsExtension(String ext);
	
	public boolean supportsMemAnalyze();
	public boolean supportsFileAnalyze();
	
	public void analyzeInit();
	public void analyzeUpdate(byte[] buf, int bufLen);
	public Map analyzeFinal();
	
	public Map analyzeFile(String fileName);
	
	public String getError(); 

}
