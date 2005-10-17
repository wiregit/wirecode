package com.limegroup.gnutella.mail;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.*;
import javax.mail.event.*;

import java.io.File;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import sun.security.krb5.internal.crypto.d;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.settings.MailSenderSetting;
import com.limegroup.gnutella.util.StringUtils;

/**
 * This class notifies the user by email when a download completes.
 *
 */
public class MailDownloadNotificator{
    
    
	private String _from,_host,_username,_password = null;

	private static final String EMAIL_SUBJECT = "Your LimeWire download";
	
	/**
     * The first part of the email's body.
     */
    private static final String EMAIL_BODY1 = 
        "-----This is a message from your LimeWire software----\n\nThe download: \"";
    
    /**
     * The second part of the email's body
     */
    private static final String EMAIL_BODY2 = "\" has ";
    
    /**
     * The keyword for a successfull download in the email's body
     */
    private static final String EMAIL_COMPLETE = "completed successfully.";
    
    /**
     * The keyword for a failed download in the email's body
     */
    private static final String EMAIL_FAILED = "failed.";
    
    public MailDownloadNotificator() {
    	_from = MailSenderSetting.USER_EMAIL.getValueAsString();		
		_host = MailSenderSetting.SMTP_SERVER.getValueAsString();
		_username =  MailSenderSetting.SMTP_USERNAME.getValueAsString();
		_password =  MailSenderSetting.SMTP_PASSWORD.getValueAsString();
    }
    
    /**
     * Sends the status of a finished download by email. The email is sent from the user's
     * email address to himself.
     * More settings are possible, such as providing imap access or activating SMTP AUTH.
     * 
     * @param dl The completed Downloader
     */
    public void sendDownloadStatusMail(Downloader dl){
        boolean fileFiltered = filterFile(MailSenderSetting.MAIL_FILTER.getValue(),dl.getFile());
        boolean filterEnabled = MailSenderSetting.MAIL_FILTER_ENABLED.getValue();
		if(!filterEnabled ||(filterEnabled && fileFiltered)){
		    String downloadState = (dl.getState()== Downloader.COMPLETE)? EMAIL_COMPLETE : EMAIL_FAILED;
		    String text = EMAIL_BODY1+dl.getFile().getName()+EMAIL_BODY2+downloadState;
		    String body = text;
		    SMTPMailSender mailSender = new SMTPMailSender(EMAIL_SUBJECT,body,_from,_from,_host,_username,_password);
			try {
				mailSender.sendMail();
			}
			catch(MessagingException doNothingException) {};
		}
	}
    
    public boolean filterFile(String[][] filterString,File dlFile) {
    	boolean isAllowed = true;
    	for (int i = 0; i < filterString.length; i++) {
			isAllowed = isAllowed && singleFilter(filterString[i],dlFile);
		}
    	return isAllowed;
    }
    
    public boolean singleFilter(String[] filterString,File dlFile){
    	boolean isAllowed = true;
    	String filename = dlFile.getName();
    	if(filterString[1].equals("name")){
			if(filterString[2].equals("contains"))
				isAllowed = (filename.toUpperCase().indexOf(filterString[3].toUpperCase()))>-1;
			else
				isAllowed = filename.equalsIgnoreCase(filterString[3]);
    	}
    	else if(filterString[1].equals("type")){
    		String[] extensions = StringUtils.split(filterString[3],",");
    		int begin = filename.lastIndexOf(".") + 1;
            if (begin == 0) // file has no extension
                isAllowed= false;
            int end = filename.length();
            String ext = filename.substring(begin, end);

            int length = extensions.length;
            boolean isValid = false;
            for (int i = 0; i < length; i++) {
                if (ext.equalsIgnoreCase(extensions[i])) {
                    isValid |= true;
                }
            }
            isAllowed &= isValid;
    	}
    	else if(filterString[1].equals("size")){
    		//size in store in KB and we need it in Bytes.
    		long sizeFilter =  (new Long(filterString[3])).longValue()*1024;
    		if(filterString[2].equals("smaller"))
				isAllowed = ((new Long(dlFile.length())).compareTo(new Long(sizeFilter)))<0;
			else
				isAllowed = ((new Long(dlFile.length())).compareTo(new Long(sizeFilter)))>0;
    	}
    	if(filterString.length > 4){
    		String[] remainingFilter = new String[filterString.length-4];
    		System.arraycopy(filterString, 4, remainingFilter, 0, filterString.length-4);
    		if(filterString[4].equals("and")) 
    			return isAllowed && singleFilter(remainingFilter,dlFile);
    		else 
    			return isAllowed || singleFilter(remainingFilter,dlFile);
    	}
    	else return isAllowed;
    }
}
