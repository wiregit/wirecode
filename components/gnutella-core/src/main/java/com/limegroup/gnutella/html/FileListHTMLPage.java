package com.limegroup.gnutella.html;

import java.io.File;
import java.net.URLEncoder;

import com.limegroup.gnutella.FileDesc;
import com.limegroup.gnutella.FileManager;
import com.limegroup.gnutella.IncompleteFileDesc;
import com.limegroup.gnutella.MessageResourceService;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.UploadManager;
import com.limegroup.gnutella.util.NetworkUtils;
import com.limegroup.gnutella.util.StringUtils;

/**
 * Creates a HTML listing of shared files and/or magnet links.
 */
public class FileListHTMLPage {

    public static final String htmlBegin =
        "<html>\r\n<head>\r\n<title>" + 
        MessageResourceService.getHTMLPageTitle() + 
        "</title>\r\n<style type=\"text/css\"><!--\r\np {\r\nfont-family: Verdana, Arial, Helvetica, sans-serif;\r\nfont-size: 10px;\r\ncolor: #666666;\r\nfont-weight: bold;\r\n}\r\n-->\r\n</style>\r\n</head>\r\n<body link=\"#000000\" vlink=\"#000000\" alink=\"#339900\" marginwidth=\"0\"\r\nmarginheight=\"0\" leftmargin=\"0\" topmargin=\"0\">\r\n<br>\r\n<br>\r\n<p align=\"center\"><img src=\""+UploadManager.RESOURCE_GET+"com/limegroup/gnutella/html/file_view_logo.gif\" alt=\"\"></p>\r\n<p align=\"center\">" + 
        MessageResourceService.getHTMLPageListingHeader();

    public static final String htmlMiddle = 
        "<br>\r\n</p>\r\n<p align=\"center\">\r\n";

    public static final String htmlMagnet =
        "</p>\r\n<br>\r\n<p align=\"center\">" +
        MessageResourceService.getHTMLPageMagnetHeader() + "<br>";

    public static final String htmlEnd = "</p>\r\n<br>\r\n</body>\r\n</html>";

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
        FileManager fm = RouterService.getFileManager();
        return getSharedFilePage(fm.getAllSharedFileDescriptors());
    }

    /** 
     * Gets a HTML listing for the shared files you pass in.
     */
    public String getSharedFilePage(final FileDesc[] sharedFiles) {
        
        // pretty simple - start the page and add a link per shared file
        StringBuffer sb = new StringBuffer();
        sb.append(htmlBegin);

        // put the correct address
        final String host = NetworkUtils.ip2string(RouterService.getAddress());
        final String port = ""+RouterService.getPort();
        sb.append(host + ":" + port + htmlMiddle);
        
        boolean shouldShowMagnets = false;
        
        {
            // get all the Shared files from the FM
            final String beginURL = "\r\n<a href=\"/get/";
            for (int i = 0; i < sharedFiles.length; i++) {
                if(sharedFiles[i] == null ||
                   sharedFiles[i] instanceof IncompleteFileDesc)
                    continue;

                File currFile = sharedFiles[i].getFile();
                sb.append(beginURL + sharedFiles[i].getIndex() + "/" + 
                          UploadManager.FV_PASS + "/" +
                          StringUtils.replace(URLEncoder.encode(currFile.getName()),
                                              "+", "%20") + "\">" + 
                          currFile.getName() + "</a><br>");
                
                if (!shouldShowMagnets && hasEnoughAltLocs(sharedFiles[i]))
                    shouldShowMagnets = true;
            }
        }
        
        if (shouldShowMagnets) {
            // put the magnet links
            sb.append(htmlMagnet);
            final String beginURL = "\r\n<a href=\"magnet:?xt=";
            final String middle1URL = "&dn=";
            final String middle2URL = 
                "&xs=http://" + host + ":" + port + "/uri-res/N2R?";
            final String middle3URL = "\">";
            final String endURL = "</a><br>";
            for (int i = 0; i < sharedFiles.length; i++) {
                if (!hasEnoughAltLocs(sharedFiles[i])) continue;
                if (!(sharedFiles[i] instanceof IncompleteFileDesc)) {
                    final String sha1 = sharedFiles[i].getSHA1Urn().toString();
                    final String fname = sharedFiles[i].getFile().getName();
                    sb.append(beginURL + sha1 + middle1URL + fname + middle2URL +
                              sha1 + middle3URL + fname + endURL);
                }
            }
        }
        
        // cap off the page
        sb.append(htmlEnd);
        return sb.toString();
    }

    // 1 is you, so you need 2 (or more)
    private boolean hasEnoughAltLocs(FileDesc fd) {
        return (fd.getAlternateLocationCollection().getAltLocsSize() > 1);
    }


}
