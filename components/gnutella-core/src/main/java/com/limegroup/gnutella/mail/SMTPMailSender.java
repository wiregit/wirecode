package com.limegroup.gnutella.mail;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.event.ConnectionListener;
import javax.mail.event.TransportListener;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.settings.MailSenderSetting;

public class SMTPMailSender {

	private static final Log LOG = LogFactory.getLog(MailDownloadNotificator.class);
	
	/**
     * The Email Subject line
     */
    private final String EMAIL_SUBJECT;
    
    /**
     * The Email Body
     */
    private final String EMAIL_BODY;
    
    /**
     * The email sender
     */
    private final String EMAIL_FROM;
    
    /**
     * The email recipient
     */
    private final String EMAIL_TO;
    
    /**
     * The SMTP Host
     */
    private final String SMTP_HOST;
    
    /**
     * The SMTP username
     */
    private final String SMTP_USERNAME;
    
    /**
     * The SMTP password
     */
    private final String SMTP_PASSWORD;
    
    private ConnectionListener _connListener;
    
    private TransportListener _transpListener;
    
    /**
     * 
     * @param subject
     * @param body
     * @param from
     * @param to
     * @param host
     * @param username
     * @param password
     */
    
    public SMTPMailSender(String subject,String body,String from,
    		String to, String host,String username,String password){
    	EMAIL_SUBJECT=subject;
    	EMAIL_BODY=body;
    	EMAIL_FROM=from;
    	EMAIL_TO=to;
    	SMTP_HOST=host;
    	SMTP_USERNAME=username;
    	SMTP_PASSWORD=password;
    }
    
    public void sendMail() throws AddressException,MessagingException{
    	Assert.that(SMTP_HOST != null && EMAIL_BODY != null && 
    			EMAIL_SUBJECT != null && EMAIL_TO != null && EMAIL_FROM != null);
    	Properties props = new Properties();
    	props.put("mail.smtp.host", SMTP_HOST);
		props.put("mail.smtp.user", SMTP_USERNAME);
		props.put("mail.smtp.password", SMTP_PASSWORD);
		
		Session session = Session.getInstance(props, null);
		//Build message
	    MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(EMAIL_FROM));
		message.addRecipient(Message.RecipientType.TO, 
				new InternetAddress(EMAIL_TO));
		message.setSubject(EMAIL_SUBJECT);
		message.setText(EMAIL_BODY);
		InternetAddress[] adr = {new InternetAddress(EMAIL_FROM)};
		Transport _transport = session.getTransport("smtp");
        if(_connListener!=null)_transport.addConnectionListener(_connListener);
		if(_transpListener!=null)_transport.addTransportListener(_transpListener);
        _transport.connect();
		_transport.sendMessage(message,adr);
    }
    
    public void setConnectionListener(ConnectionListener arg0) {
    	_connListener = arg0;
    }
    
    public void setTransportListener(TransportListener arg0) {
    	_transpListener = arg0;
    }
}
