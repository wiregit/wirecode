package com.limegroup.gnutella.mail;

import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.settings.MailSenderSetting;

public abstract class AbstractMailSender {

	private static final Log LOG = LogFactory.getLog(MailDownloadNotificator.class);
	
	/**
     * The Email Subject line
     */
    protected String EMAIL_SUBJECT;
    
    /**
     * The Email Body
     */
    protected String EMAIL_BODY;
    
    /**
     * The user email
     */
    protected String USER_EMAIL;
    
    /**
     * The SMTP Host
     */
    protected String SMTP_HOST;
    
    /**
     * The SMTP username
     */
    protected String SMTP_USERNAME;
    
    /**
     * The SMTP password
     */
    protected String SMTP_PASSWORD;
    
    protected AbstractMailSender(){}
    
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
		/** ALSO POSSIBLE TO ADD LISTENERS AND NOTIFY GUI
		* transport.addConnectionListener(this);
		* transport.addTransportListener(this);*/
        transport.connect();
		transport.sendMessage(message,adr);
    }
}
