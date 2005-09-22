package com.limegroup.gnutella.settings;

/**
 * Settings for mail
 */
public class MailSenderSetting extends LimeProps {

    private MailSenderSetting() {}
    
    /**
	 * Sets whether or not mail should be sent on completed downloads.
	 */
    public static final BooleanSetting MAIL_ENABLED =
        FACTORY.createBooleanSetting("MAIL_ENABLED", true);
    
    /**
     * The SMTP server to use to send mail
     */
    public static final StringSetting SMTP_SERVER = 
        FACTORY.createStringSetting("SMTP_SERVER","");
    
    /**
     * The user name for the SMTP server
     */
    public static final StringSetting SMTP_USERNAME = 
        FACTORY.createStringSetting("SMTP_USERNAME","");
    
    /**
     * The password for the SMTP server
     */
    public static final StringSetting SMTP_PASSWORD = 
        FACTORY.createStringSetting("SMTP_PASSWORD","");
    
    /**
     * The email address to send email
     */
    public static final StringSetting USER_EMAIL = 
        FACTORY.createStringSetting("USER_EMAIL","");
    
    /**
	 * Sets whether or not mail filters are activated.
	 */
    public static final BooleanSetting MAIL_FILTER_ENABLED =
        FACTORY.createBooleanSetting("MAIL_FILTER_ENABLED", true);
    
    /**
     * The email notification filter
     */
    public static final NotificationFilterSetting MAIL_FILTER = 
        FACTORY.createNotificationFilterSetting("MAIL_FILTER", new String[0][0]);
}
