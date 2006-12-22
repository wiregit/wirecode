package org.limewire.setting;

import java.io.File;
import java.util.Properties;


/**
 * Forwards requests to default setting or to setting.
 */
public class ProxyFileSetting extends FileSetting {

	private FileSetting defaultSetting;

	/**
	 * Constructs a new file setting that defaults to a different setting.
	 */
	ProxyFileSetting(Properties defaultProps, Properties props, String key,
					 FileSetting defaultSetting) {
        super(defaultProps, props, key, 
                  new File("impossible-default-limewire-filename3141592"));
        setPrivate(true);
        this.defaultSetting = defaultSetting;
	}

	public File getValue() {
		return isDefault() ? defaultSetting.getValue() : super.getValue();
	}
}

