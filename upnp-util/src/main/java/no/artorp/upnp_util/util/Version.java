package no.artorp.upnp_util.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Version {
	private static String version = null;
	
	public static String getVersion() {
		if (version == null) {
			Properties p = new Properties();
			try (InputStream is = Version.class.getResourceAsStream("/version.properties")) {
				p.load(is);
			} catch (IOException e) {
				System.err.println("Error when loading version.properties file");
				e.printStackTrace();
			} catch (NullPointerException e) {
				System.err.println("Couldn't find version.properties file");
				e.printStackTrace();
			}
			version = p.getProperty("version", "<version_load_error>");
		}
		return version;
	}
}
