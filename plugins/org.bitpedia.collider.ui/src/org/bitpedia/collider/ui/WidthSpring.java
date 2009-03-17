/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: WidthSpring.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.ui;

import javax.swing.JComponent;
import javax.swing.Spring;

public class WidthSpring extends Spring {
	
	private JComponent comp;
	
	public WidthSpring(JComponent comp) {
		this.comp = comp;
	}

	public int getMinimumValue() {		
		return comp.getWidth();
	}

	public int getPreferredValue() {
		return comp.getWidth();
	}

	public int getMaximumValue() {
		return comp.getWidth();
	}

	public int getValue() {
		return comp.getWidth();
	}

	public void setValue(int value) {
		if (0 <= value) {
			comp.setSize(value, comp.getHeight());
		}
	}

}
