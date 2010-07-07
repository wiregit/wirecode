package org.limewire.core.settings;

import org.limewire.setting.BooleanSetting;
import org.limewire.setting.StringArraySetting;
import org.limewire.setting.StringSetting;

/**
 * Settings related to interaction with Spoon.
 */
public class SpoonSettings extends LimeProps {
    
    private SpoonSettings() {}
    
    /**
     * Returns true if search queries should be sent to the spoon server. NOTE: this
     * will send ALL search queries to the spoon server if true.
     */
    public static final BooleanSetting SPOON_SEARCH_IS_ENABLED = 
        FACTORY.createRemoteBooleanSetting("SPOON_SEARCH_IS_ENABLED", true, "Spoon.spoonSearchIsEnabled");
    
    /**
     * Location of the Spoon Search Server.
     */
    public static final StringSetting SPOON_SEARCH_SERVER =
        FACTORY.createRemoteStringSetting("SPOON_SEARCH_SERVER", "http://int.api.spoon.awseast.lime:8080/ad/", "Spoon.spoonSearchServer");
    
    /**
     * URL to append spoon ads to.
     */
    public static final StringSetting SPOON_AD_URL =
        FACTORY.createRemoteStringSetting("SPOON_AD_URL", "http://www.limewire.com/spoon/", "Spoon.spoonAdUrl");
    
    /**
     * A list of country codes where its appropriate to show spoon.
     */
    public static final StringArraySetting VALID_COUNTRY_CODES = FACTORY.createRemoteStringArraySetting(
            "SPOON_VALID_COUNTRY_CODES", new String[]{"US"}, "Spoon.validCountryCodes");
}
