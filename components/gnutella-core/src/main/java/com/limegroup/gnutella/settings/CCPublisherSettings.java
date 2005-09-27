package com.limegroup.gnutella.settings;

public class CCPublisherSettings extends LimeProps {  

	private CCPublisherSettings(){}
	
	public static final BooleanSetting CCPUBLISHER_WARN= 
		FACTORY.createBooleanSetting("CCPUBLISHER_WARN", false);
}
