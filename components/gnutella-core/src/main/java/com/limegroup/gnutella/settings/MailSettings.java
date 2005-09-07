package com.limegroup.gnutella.settings;

/**
 * Settings for mail
 */
public class MailSettings extends LimeProps {

    private MailSettings() {}
    
    /**
	 * Sets whether or not mail should be sent on completed downloads.
	 */
    public static final BooleanSetting MAIL_ENABLED =
        FACTORY.createBooleanSetting("MAIL_ENABLED", true);
    
    /**
     * The SMTP server to use to send mail
     */
    public static final StringSetting SMTP_SERVER = 
        FACTORY.createStringSetting("SMTP_SERVER","cyrus.limewire.com");
    
    /**
     * The user name for the SMTP server
     */
    public static final StringSetting SMTP_USERNAME = 
        FACTORY.createStringSetting("SMTP_USERNAME","mkornfilt");
    
    /**
     * The password for the SMTP server
     */
    public static final StringSetting SMTP_PASSWORD = 
        FACTORY.createStringSetting("SMTP_PASSWORD","mamako1");
    
    /**
     * The email address to send email
     */
    public static final StringSetting USER_EMAIL = 
        FACTORY.createStringSetting("USER_EMAIL","mkornfilt@limewire.com");
}
