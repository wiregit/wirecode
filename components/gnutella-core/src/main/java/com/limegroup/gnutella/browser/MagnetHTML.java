package com.limegroup.gnutella.browser;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;

import com.limegroup.gnutella.ByteReader;
import com.limegroup.gnutella.Constants;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.IOUtils;
import com.limegroup.gnutella.util.URLDecoder;
import java.util.StringTokenizer;
import java.net.URLEncoder;

/**
 * Allow various Magnet Related HTML page rendering.
 */
public class MagnetHTML {

    static String buildMagnetDetailPage(String cmd) {
        StringTokenizer st = new StringTokenizer(cmd, "&");
        String keystr;
        String valstr;
        int    start;
        String address = "";
        String fname   = "";
        String sha1    = "";
        String ret= magnetDetailPageHeader();
        
        // Process each key=value pair
        while (st.hasMoreTokens()) {
            keystr = st.nextToken();
            keystr = keystr.trim();
            start  = keystr.indexOf("=")+1;
            valstr = keystr.substring(start);
            keystr = keystr.substring(0,start-1);
            valstr=URLDecoder.decode(valstr);   
            if ( keystr.equals("addr") ) {
                address = valstr;
            } else if ( keystr.startsWith("n") ) {
                fname = valstr;
            } else if ( keystr.startsWith("u") ) {
                sha1 = valstr;
                ret += magnetDetail(address, fname, sha1);
            }
        }
        ret += 
          "</table>"+
          "</body></html>";
        return ret;
    }

	static String buildMagnetEmailPage(String cmd) {
		StringTokenizer st = new StringTokenizer(cmd, "&");
		String keystr;
		String valstr;
		int    start;
		String address = "";
		String fname   = "";
		String sha1    = "";
        String ret     = "";
        int    count   = 0;
        
		// Process each key=value pair
     	while (st.hasMoreTokens()) {
		    keystr = st.nextToken();
			keystr = keystr.trim();
			start  = keystr.indexOf("=")+1;
		    valstr = keystr.substring(start);
			keystr = keystr.substring(0,start-1);
			valstr=URLDecoder.decode(valstr);	
			if ( keystr.equals("addr") ) {
				address = valstr;
                ret= magnetEmailHeader(address);
			} else if ( keystr.startsWith("n") ) {
				fname = valstr;
			} else if ( keystr.startsWith("u") ) {
				sha1 = valstr;
				ret += magnetEmailDetail(address, fname, sha1, count);
                count++;
			}
		}
        ret += magnetEmailTrailer(count);
		return ret;
	}

    private static String magnetDetail(String address, String fname, String sha1) {
        String ret =
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Name</b></td>"+
         "    <td bgcolor=\"#FFFFFF\" class=\"name\">"+fname+"</td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>SHA1</b></td>"+
         "    <td bgcolor=\"#ffffff\" class=\"text\">"+sha1+"</td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Link</b></td>"+
         "    <td bgcolor=\"#ffffff\" class=\"text\"><a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+
         fname+"</a></td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Magnet</b></td>"+
         "    <td bgcolor=\"#ffffff\"><textarea name=\"textarea\" cols=\"80\" rows=\"4\" wrap=\"VIRTUAL\" class=\"area\">magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"</textarea></td>"+
         "  </tr>"+
         "  <tr> "+
         "    <td bgcolor=\"#CCCCCC\" class=\"text\"><b>Html link</b></td>"+
         "    <td bgcolor=\"#ffffff\"><textarea name=\"textarea\" cols=\"80\" rows=\"5\" wrap=\"VIRTUAL\" class=\"area\"><a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+"&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+fname+"</a></textarea></td>"+
         "  </tr>"+
         "  <tr bgcolor=\"#333333\"> "+
         "    <td colspan=\"2\" class=\"text\" height=\"5\"></td></tr>";

        return ret;
    }


    private static String magnetDetailPageHeader() {
       String ret= 
         "<html>"+
         "<head>"+
         "<title>LimeWire Magnet Descriptions</title>"+
         "<style type=\"text/css\">"+
         "<!--"+
         ".text {"+
         "    font-family: Verdana, Arial, Helvetica, sans-serif;"+
         "    font-size: 11px;"+
         "    color: #333333;"+
         "}"+
         ".header {"+
         "    font-family: Arial, Helvetica, sans-serif;"+
         "    font-size: 14pt;"+
         "    color: #ffffff;"+
         "}"+
         ".name {"+
         "    font-family: Verdana, Arial, Helvetica, sans-serif;"+
         "    font-size: 11px;"+
         "    font-weight: bold;"+
         "    color: #000000;"+
         "}"+
         ".area  { "+
         "border: 1px solid;"+
          "margin: 0;"+
          "padding: 4px;"+
          "background: #FFFEF4;"+
          "color: #333333;"+
          "font: 11px Verdana, Arial;"+
          "text-align: left;"+
         "}"+
         "-->"+
         "</style>"+
         "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">"+
         "</head>"+
         "<body bgcolor=\"#666666\">"+
         "<span class=\"header\"><center>"+
         "  LimeWire Magnet Details "+
         "</center></span><br>"+
         "<table border=\"0\" cellpadding=\"5\" cellspacing=\"1\" bgcolor=\"#999999\" align=\"center\">";

        return ret;
    }


    private static String magnetEmailHeader(String address){
        return
          "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\n"+
          "<html>\n"+
          "<head>\n"+
          "<title>LimeWire Send Magnet Form</title>\n"+
          "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\">\n"+
          "<script LANGUAGE=\"JavaScript1.2\" SRC=\"/magnet10/scripts.js\" TYPE=\"text/javascript\"></script>\n"+
          "<link href=\"/magnet10/style.css\" rel=\"stylesheet\" type=\"text/css\">\n"+
          "</head>\n"+
          "<body>\n"+
          "<form name=\"form1\" method=\"post\" action=\""+
          "http://email.limewire.com/cgi-bin/send_email.cgi"+
          "\" onsubmit=\"return Validator(this)\">\n"+
          "    <input name=\"address\" type=\"hidden\" value=\"\n"+address+"\">\n"+
          "    <table width=\"500\" border=\"0\" align=\"center\" cellpadding=\"5\" cellspacing=\"1\" bgcolor=\"#FFFFFF\" align=\"center\">" +
          "    <tr> \n"+
          "      <td colspan=\"2\">To EMAIL direct links to the selected files in your Shared Library, fill out and submit the form below.</td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td width=\"42\" align=\"right\"><b>To:</b></td>\n"+
          "      <td width=\"415\"> \n"+
          "        <input name=\"to_email\" type=\"text\" maxlength=\"100\">\n"+
          "         (required)</td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td align=\"right\"><b>CC:</b></td>\n"+
          "      <td> \n"+
          "        <input name=\"cc_email\" type=\"text\" maxlength=\"100\"></td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td align=\"right\"><b>From: </b></td>\n"+
          "      <td> \n"+
          "        <input name=\"from_email\" type=\"text\" maxlength=\"100\">\n"+
          "        (required)</td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td align=\"right\"><b>Subject: </b></td>\n"+
          "      <td> \n"+
          "        <input name=\"subject\" type=\"text\" maxlength=\"100\">\n"+
          "      </td>\n"+
          "    </tr>\n"+
          "    <tr class=\"yellow\"> \n"+
          "      <td>&nbsp;</td>\n"+
          "      <td>Message to send with links: <br> <textarea name=\"comment\" cols=\"40\" rows=\"5\" wrap=\"VIRTUAL\"></textarea> </td>\n"+
          "    </tr>\n"+
          "    <tr> \n"+
          "      <td colspan=\"2\" class=\"yellow2\"> LINKS TO BE MAILED:<br>\n"+
          "        <ol>";
    }


    private static String magnetEmailDetail(String address, String fname, String sha1,
         int count) {
        return
          "          <li><span class=\"links\">"+
          "<a href=\"magnet:?xt=urn:sha1:"+sha1+"&dn="+fname+
          "&xs=http://"+address+"/uri-res/N2R?urn:sha1:"+sha1+"\">"+
          fname+"</a>"+
          "</span><br>\n"+
          "          <input name=\"f"+count+"\" type=\"hidden\" value=\""+fname+"\">\n"+
          "          <input name=\"s"+count+"\" type=\"hidden\" value=\""+sha1+"\">\n"+
          "            <br>\n"+
          "            Link Description:<br>\n"+
          "            <textarea name=\"desc"+count+"\" cols=\"40\" rows=\"2\" wrap=\"VIRTUAL\"></textarea>\n"+
          "            <hr size=\"1\">\n"+
          "          </li>\n";
    }


    private static String magnetEmailTrailer(int count) {
        return
          "        </ol>\n"+
          "        <input name=\"count\" type=\"hidden\" value=\""+count+"\">"+
          "        <center><input name=\"Submit\" type=\"submit\" value=\"Submit and Send\"></center></td>\n"+
          "    </tr>";
    }

}

