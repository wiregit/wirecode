package com.limegroup.gnutella.html;

/**
 * Creates a HTML listing of shared files and/or magnet links.
 */
public class FileListHTMLPage {

    public final String htmlBegin =
    "<html>\r\n<head>\r\n<metacontent=\"text/html; charset=ISO-8859-1\"\r\nhttp-equiv=\"content-type\">\r\n<title></title>\r\n</head>\r\n<body>\r\n<br>\r\n<span style=\"font-weight: bold; text-decoration: underline;\">File Listing for firewall.limewire.com:6346</span><br>\r\n<br>";

    public final String htmlEnd = "<br>\r\n</body>\r\n</html>";

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
        
        // get all the shared files from the FM

        // cap off the page
        sb.append(htmlEnd);
        return sb.toString();
    }

}
