package com.limegroup.gnutella.html;

import com.limegroup.gnutella.*;
import com.limegroup.gnutella.util.NetworkUtils;
import java.io.File;

/**
 * Creates a HTML listing of shared files and/or magnet links.
 */
public class FileListHTMLPage {

    public final String htmlBegin =
    "<html>\r\n<head>\r\n<metacontent=\"text/html; charset=ISO-8859-1\"\r\nhttp-equiv=\"content-type\">\r\n<title></title>\r\n</head>\r\n<body>\r\n<br>\r\n<span style=\"font-weight: bold; text-decoration: underline;\">File Listing for ";

    public final String htmlMiddle = "</span><br>\r\n<br>";

    public final String htmlEnd = "\r\n<br>\r\n</body>\r\n</html>";

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
