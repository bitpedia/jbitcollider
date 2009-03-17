/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: AppPlugin.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.ui;

import java.awt.BorderLayout;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Collection;

import javax.swing.JFrame;

import org.java.plugin.boot.Application;
import org.java.plugin.boot.ApplicationPlugin;
import org.java.plugin.registry.Extension;
import org.java.plugin.registry.ExtensionPoint;
import org.java.plugin.registry.Extension.Parameter;
import org.java.plugin.util.ExtendedProperties;

public class AppPlugin extends ApplicationPlugin implements Application {

	private JFrame frame;

	private MainDialog mainDlg;
	
	private Collection fmtHandlers;

	public AppPlugin() {

		frame = new JFrame("jBitcollider");
		frame.getContentPane().setLayout(new BorderLayout());
		mainDlg = new MainDialog(frame);
		frame.getContentPane().add("Center", mainDlg);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(450, 300);
		frame.setResizable(false);
		frame.setLocationRelativeTo(null);
	}

	protected Application initApplication(ExtendedProperties props, String[] args)
			throws Exception {
		
		boolean noSubmitting = false;
		String alternateUrl = null;
		boolean md5 = false;
		boolean crc32 = false;
		
		int i = 0;
		while (i < args.length) {
			if ("-n".equals(args[i])) {
				noSubmitting = true;
			} else if ("-u".equals(args[i])) {
				if (i+1 < args.length) {
					alternateUrl = args[i+1];
					i++;
				}
			} else if ("-md5".equals(args[i])) {
				md5 = true;
			} else if ("-crc32".equals(args[i])) {
				crc32 = true;
			}
				
			i++;
		}
		
		mainDlg.configure(noSubmitting, alternateUrl, md5, crc32);
		

		return this;
	}

	protected void doStart() throws Exception {
	}

	protected void doStop() throws Exception {
	}

	public void startApplication() throws Exception {
		fmtHandlers = loadFormatHandlers();
		mainDlg.setFmtHandlers(fmtHandlers);
		frame.setVisible(true);
	}

	public Collection loadFormatHandlers() {

		Collection handlers = new LinkedList();

		try {

			String id = getManager().getPlugin("org.bitpedia.collider.core")
					.getDescriptor().getId();
			ExtensionPoint toolExtPoint = getManager().getRegistry()
					.getExtensionPoint(id, "FormatHandler");
			for (Iterator it = toolExtPoint.getConnectedExtensions().iterator(); it
					.hasNext();) {
				Extension ext = (Extension) it.next();

				Parameter classParam = ext.getParameter("class");
				ClassLoader classLoader = getManager().getPluginClassLoader(ext.getDeclaringPluginDescriptor());
				Class handlerClass = classLoader.loadClass(classParam.valueAsString());				
				handlers.add(handlerClass.newInstance());
			}

		} catch (Exception e) {
			System.out.println("Error loading plugins: "+e.getMessage());
			e.printStackTrace();
		} 

		return handlers;

	}

	public Collection getFmtHandlers() {
		return fmtHandlers;
	}
}
