package com.limegroup.gnutella.mail;

import java.util.Properties;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.ConnectionListener;
import javax.mail.event.TransportEvent;
import javax.mail.event.TransportListener;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.limegroup.gnutella.settings.MailSenderSetting;

public class TestMailSender extends AbstractMailSender{
	
	/**
     * The body of the test email;
     */
    private final String EMAIL_TEST =
        "-----This is a message from your LimeWire software----\n\nThe Email settings " +
        "have been configured correctly";
    
    private ConnectionListener connectionListener;
    private TransportListener transportListener;
    
    /**
     * 
     * @param userEmail
     * @param smtpServer
     * @param username
     * @param password
     */

	public TestMailSender(String userEmail,String smtpServer,
			String username,String password, ConnectionListener cl,
			TransportListener tl){
		USER_EMAIL = userEmail;
		SMTP_HOST = smtpServer;
		SMTP_USERNAME =  username;
		SMTP_PASSWORD =  password;
		connectionListener = cl;
		transportListener = tl;
	}
	
	public void sendTestMail() throws AddressException,MessagingException{
		EMAIL_SUBJECT = "LimeWire test message";
	    EMAIL_BODY = EMAIL_TEST;
	    sendMail();
	}
	/**
	 * Overrides the abstract method to add listeners
	 */
	protected void sendMail() throws AddressException,MessagingException{
    	Properties props = new Properties();
    	props.put("mail.smtp.host", SMTP_HOST);
		props.put("mail.smtp.user", SMTP_USERNAME);
		props.put("mail.smtp.password", SMTP_PASSWORD);
//		props.put("mail.smtp.auth", "true" );
		
		Session session = Session.getInstance(props, null);
		//Build message
	    MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(USER_EMAIL));
		message.addRecipient(Message.RecipientType.TO, 
				new InternetAddress(USER_EMAIL));
		message.setSubject(EMAIL_SUBJECT);
		message.setText(EMAIL_BODY);
		InternetAddress[] adr = {new InternetAddress(USER_EMAIL)};
		Transport transport = session.getTransport("smtp");
		transport.addConnectionListener(connectionListener);
		transport.addTransportListener(transportListener);
        transport.connect();
		transport.sendMessage(message,adr);
		transport.close();
    }
}
