package com.limegroup.gnutella.html;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.NetworkUtils;
import java.io.File;

/**
 * Creates a HTML listing of shared files and/or magnet links.
 */
public class FileListHTMLPage {

    public final String htmlBegin =
        "<html>\r\n<head>\r\n<title>Download Page</title>\r\n<style type=\"text/css\"><!--\r\np {\r\nfont-family: Verdana, Arial, Helvetica, sans-serif;\r\nfont-size: 10px;\r\ncolor: #666666;\r\nfont-weight: bold;\r\n}\r\n-->\r\n</style>\r\n</head>\r\n<body link=\"#000000\" vlink=\"#000000\" alink=\"#339900\" marginwidth=\"0\"\r\nmarginheight=\"0\" leftmargin=\"0\" topmargin=\"0\">\r\n<br>\r\n<br>\r\n<p align=\"center\"><img src=\"http://www.limewire.org/img/logo_wh.gif\" alt=\"\"></p>\r\n<p align=\"center\">File Listing for ";

    public final String htmlMiddle = "<br>\r\n</p>\r\n<p align=\"center\">\r\n";

    public final String htmlEnd = "</p>\r\n<br>\r\n</body>\r\n</html>";

    private static final FileListHTMLPage _instance = new FileListHTMLPage();
    public static FileListHTMLPage instance() {
        return _instance;
    }

    private FileListHTMLPage() {
    }

    /** 
     * Gets a HTML listing of all your shared files.
     */
    public String getSharedFilePage() {
        
        // pretty simple - start the page and add a link per shared file
        StringBuffer sb = new StringBuffer();
        sb.append(htmlBegin);
        
        // put the correct address
        final String host = NetworkUtils.ip2string(RouterService.getAddress());
        final String port = ""+RouterService.getPort();
        sb.append(host + ":" + port + htmlMiddle);

        // get all the Shared files from the FM
        final String beginURL = "\r\n<a href=http://" + host + ":" + port + "/";
        FileManager fm = RouterService.getFileManager();
        FileDesc[] sharedFiles = fm.getAllSharedFileDescriptors();
        for (int i = 0; i < sharedFiles.length; i++) {
            File currFile = sharedFiles[i].getFile();
            sb.append(beginURL + currFile.getName() + ">" + currFile.getName() +
                      "</a><br>");
        }

        // cap off the page
        sb.append(htmlEnd);
        return sb.toString();
    }

}
