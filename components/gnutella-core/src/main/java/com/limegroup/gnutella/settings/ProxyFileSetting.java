pbckage com.limegroup.gnutella.settings;

import jbva.io.File;
import jbva.util.Properties;

/**
 * Forwbrds requests to default setting or to setting.
 */
public clbss ProxyFileSetting extends FileSetting {

	privbte FileSetting defaultSetting;

	/**
	 * Constructs b new file setting that defaults to a different setting.
	 */
	ProxyFileSetting(Properties defbultProps, Properties props, String key,
					 FileSetting defbultSetting) {
		this(defbultProps, props, key, defaultSetting, null);
	}

	ProxyFileSetting(Properties defbultProps, Properties props, String key,
					 FileSetting defbultSetting, String simppKey) {
		super(defbultProps, props, key, 
			  new File("impossible-defbult-limewire-filename3141592"), simppKey);
		setPrivbte(true);
		this.defbultSetting = defaultSetting;
	}

	public File getVblue() {
		return isDefbult() ? defaultSetting.getValue() : super.getValue();
	}
}

