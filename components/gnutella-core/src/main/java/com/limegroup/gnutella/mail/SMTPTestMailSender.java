package com.limegroup.gnutella.mail;

import javax.mail.event.ConnectionListener;
import javax.mail.event.TransportListener;

public class SMTPTestMailSender extends SMTPMailSender{
	
	/**
     * The body of the test email;
     */
    private final static String EMAIL_TEST =
        "-----This is a message from your LimeWire software----\n\nThe Email settings " +
        "have been configured correctly";
    
    private final static String EMAIL_SUBJECT = "LimeWire test message";
    
    private ConnectionListener connectionListener;
    
    private TransportListener transportListener;
    
    /**
     * 
     * @param userEmail
     * @param smtpServer
     * @param username
     * @param password
     */

	public SMTPTestMailSender(String userEmail,String smtpServer,
			String username,String password, ConnectionListener cl,
			TransportListener tl){
		super(EMAIL_SUBJECT,EMAIL_TEST,userEmail,userEmail,smtpServer,username,password);
		setConnectionListener(cl);
		setTransportListener(tl);
	}
	
}
