package com.limegroup.gnutella.settings;

public class CCPublisherSettings extends LimeProps {  

	private CCPublisherSettings(){}
	
	public static final BooleanSetting SHOW_CCPUBLISHER_WARNING= 
		FACTORY.createBooleanSetting("SHOW_CCPUBLISHER_WARNING", true);
}
