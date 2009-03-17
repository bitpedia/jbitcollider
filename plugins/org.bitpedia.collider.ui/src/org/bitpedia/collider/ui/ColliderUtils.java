/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: ColliderUtils.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.ui;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.XMLFormatter;

public class ColliderUtils {

	public static final Logger colliderLogger = Logger
			.getLogger("org.bitpedia.collider");
	static {
		colliderLogger.setLevel(Level.OFF);
		Handler[] handlers = colliderLogger.getHandlers();
		for (int i = 0; i < handlers.length; i++) {
			colliderLogger.removeHandler(handlers[i]);
		}
	}

	public static final Logger debugLogger = Logger
			.getLogger("org.bitpedia.collider.debugLogger");

	static {
		InputStream stm = ClassLoader.getSystemClassLoader()
				.getResourceAsStream("logging.properties");
		if (null != stm) {
			Properties props = new Properties();
			try {
				props.load(stm);
				configureLogger(debugLogger, props);
			} catch (Throwable e) {
				System.err.println("Error loading logging properties.");
				e.printStackTrace(System.err);
			}
		}
	}

	private static void configureLogger(Logger logger, Properties props)
			throws IOException {

		String propBaseName = logger.getName().substring(
				colliderLogger.getName().length() + 1);

		String level = props.getProperty(propBaseName + ".level");
		if (null != level) {
			Level lev = Level.parse(level);
			logger.setLevel(lev);
			if (Level.OFF.equals(lev)) {
				return;
			}
		}

		String handlerName = props.getProperty(propBaseName + ".handler");
		Handler handler = null;
		if ("console".equals(handlerName)) {
			handler = new ConsoleHandler();
		} else if (null != handlerName) {
			handler = new FileHandler(handlerName);
		}
		if (null != handler) {

			logger.addHandler(handler);

			String formatterName = props.getProperty(propBaseName
					+ ".formatter");
			if ("xml".equals(formatterName)) {
				handler.setFormatter(new XMLFormatter());
			} else if ("simple".equals(formatterName)) {
				handler.setFormatter(new SimpleFormatter());
			}
		}
	}

}
