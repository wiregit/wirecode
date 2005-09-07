package com.limegroup.gnutella;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.*;
import javax.mail.event.*;

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.settings.MailSettings;

/**
 * 
 *
 */
final class MailSender {
    
    private static final Log LOG = LogFactory.getLog(MailSender.class);
    
    /**
     * The Email Subject line
     */
    private final String EMAIL_SUBJECT = "Your LimeWire download";
    
    /**
     * The first part of the email's body.
     */
    private final String EMAIL_BODY1 = 
        "-----This is a message from your LimeWire software----\n\nThe download: \"";
    
    /**
     * The second part of the email's body
     */
    private final String EMAIL_BODY2 = "\" has ";
    
    /**
     * The keyword for a successfull download in the email's body
     */
    private final String EMAIL_COMPLETE = "completed successfully.";
    
    /**
     * The keyword for a failed download in the email's body
     */
    private final String EMAIL_FAILED = "failed.";
    
    /**
     * The body of the test email;
     */
    private final String EMAIL_TEST =
        "-----This is a message from your LimeWire software----\n\nThe Email settings " +
        "have been configured correctly";
    
    /**
	 * Package-access constructor
	 */
    MailSender() {}
    
    /**
     * Sends the status of a finished download by email. The email is sent from the user's
     * email address to himself.
     * More settings are possible, such as providing imap access or activating SMTP AUTH.
     * 
     * @param dl The completed Downloader
     */
    public void sendDownloadStatusMail(Downloader dl){
        
		String from = MailSettings.USER_EMAIL.getValueAsString(); //TODO use limewire address?
		String to = MailSettings.USER_EMAIL.getValueAsString();
		
		Properties props = new Properties();

		//Setup mail server
		props.put("mail.smtp.host", MailSettings.SMTP_SERVER.getValueAsString());
		props.put("mail.smtp.user", MailSettings.SMTP_USERNAME.getValueAsString());
		props.put("mail.smtp.password", MailSettings.SMTP_PASSWORD.getValueAsString());
		//sprops.put("mail.smtp.auth", "true" );

		//Get new session instance with properties
		Session session = Session.getInstance(props, null);

		try {
			//Define message
		    String downloadState = (dl.getState()== Downloader.COMPLETE)? EMAIL_COMPLETE : EMAIL_FAILED;
		    String text = EMAIL_BODY1+dl.getFile().getName()+EMAIL_BODY2+downloadState;
			//Build message
		    MimeMessage message = new MimeMessage(session);
	        message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(
					to));
			message.setSubject(EMAIL_SUBJECT);
			message.setText(text);
			InternetAddress[] adr = {new InternetAddress(to)};
			Transport transport = session.getTransport("smtp");
			/** ALSO POSSIBLE TO ADD LISTENERS AND NOTIFY GUI
			* transport.addConnectionListener(this);
			* transport.addTransportListener(this);*/
	        transport.connect();
			transport.sendMessage(message,adr);

		} catch (AddressException ae) {
		    LOG.error("Email address does not exist", ae);
		} catch (MessagingException me) {
		    LOG.error("Problem sending Email", me);
		}
	}
    public String testConnection(){
        String from = MailSettings.USER_EMAIL.getValueAsString(); //TODO use limewire address?
		String to = MailSettings.USER_EMAIL.getValueAsString();
		
		Properties props = new Properties();

		//Setup mail server
		props.put("mail.smtp.host", MailSettings.SMTP_SERVER.getValueAsString());
		props.put("mail.smtp.user", MailSettings.SMTP_USERNAME.getValueAsString());
		props.put("mail.smtp.password", MailSettings.SMTP_PASSWORD.getValueAsString());
		props.put("mail.smtp.auth", "true" );

		//Get new session instance with properties
		Session session = Session.getInstance(props, null);

		try {
			//Define message
		    String text = EMAIL_TEST;
			//Build message
		    MimeMessage message = new MimeMessage(session);
	        message.setFrom(new InternetAddress(from));
			message.addRecipient(Message.RecipientType.TO, new InternetAddress(
					to));
			message.setSubject(EMAIL_SUBJECT);
			message.setText(text);
			InternetAddress[] adr = {new InternetAddress(to)};
			Transport transport = session.getTransport("smtp");
			/** ALSO POSSIBLE TO ADD LISTENERS AND NOTIFY GUI
			* transport.addConnectionListener(this);
			* transport.addTransportListener(this);*/
	        transport.connect();
			transport.sendMessage(message,adr);
			return "Success";

		} catch (AddressException ae) {
		    LOG.error("Email address does not exist", ae);
		    return "Invalid email address";
		} catch (MessagingException me) {
		    LOG.error("Problem sending Email", me);
		    return "Connection failed";
		}
    }
}
