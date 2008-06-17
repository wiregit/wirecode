package com.limegroup.gnutella.caas.restlet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.inject.Inject;
import com.limegroup.gnutella.caas.Common;
import com.limegroup.gnutella.caas.SearchParams;

public class RestletSearchParams extends SearchParams {

    private static DocumentBuilder _builder;
    
    static {
        setup();
    }
    
    @Inject
    public RestletSearchParams() {
        
    }
    
    /**
     * 
     */
    private final static void setup() {
        try {
            _builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        }
        catch (ParserConfigurationException pce) {
            // ...
        }
    }
    
    /**
     * Create an XML string appropriate for the type of search being done.
     * 
     * TODO - get rid of the toString*() methods and just call toXml() and 
     *        then serialize it.
     *        
     */
    public String toString() {
        String searchType = getSearchType();
        
        if (searchType == null || searchType.length() == 0)
            return null;
        else if (searchType.equalsIgnoreCase("video"))
            return toStringVideo();
        else if (searchType.equalsIgnoreCase("audio"))
            return toStringAudio();
        else if (searchType.equalsIgnoreCase("document"))
            return toStringDocument();
        else if (searchType.equalsIgnoreCase("image"))
            return toStringImage();
        else if (searchType.equalsIgnoreCase("program"))
            return toStringProgram();
        else
            return null;
    }
    
    /**
     * 
     */
    public Element toXml() {
        String searchType = getSearchType();
        
        if (searchType == null || searchType.length() == 0)
            return null;
        else if (searchType.equalsIgnoreCase("video"))
            return toXmlVideo();
        else if (searchType.equalsIgnoreCase("audio"))
            return toXmlAudio();
        else if (searchType.equalsIgnoreCase("document"))
            return toXmlDocument();
        else if (searchType.equalsIgnoreCase("image"))
            return toXmlImage();
        else if (searchType.equalsIgnoreCase("program"))
            return toXmlProgram();
        else
            return null;
    }
    
    private Element toXmlVideo() {
        Document document = _builder.newDocument();
        Element videos = (Element)document.appendChild(document.createElement("videos"));
        Element video = (Element)videos.appendChild(document.createElement("video"));
        
        if (getTitle() != null)
            video.setAttribute("name", getTitle());
        
        if (getType() != null)
            video.setAttribute("type", getType());
        
        if (getYear() != null)
            video.setAttribute("year", getYear());
        
        if (getRating() != null)
            video.setAttribute("rating", getRating());
        
        if (getLength() != null)
            video.setAttribute("length", getLength());
        
        if (getComments() != null)
            video.setAttribute("comments", getComments());
        
        if (getLicenseType() != null)
            video.setAttribute("licensetype", getLicenseType());
        
        if (getCopyright() != null)
            video.setAttribute("copyright", getCopyright());
        
        if (getHeight() != null)
            video.setAttribute("height", getHeight());
        
        if (getWidth() != null)
            video.setAttribute("width", getWidth());
        
        if (getBitrate() != null)
            video.setAttribute("bitrate", getBitrate());
        
        if (getDirector() != null)
            video.setAttribute("director", getDirector());
        
        if (getStudio() != null)
            video.setAttribute("studio", getStudio());
        
        if (getLanguage() != null)
            video.setAttribute("language", getLanguage());
        
        if (getStars() != null)
            video.setAttribute("stars", getStars());
        
        if (getProducer() != null)
            video.setAttribute("producer", getProducer());
        
        if (getSubtitles() != null)
            video.setAttribute("subtitles", getSubtitles());
        
        return videos;
    }
    
    private Element toXmlAudio() {
        Document document = _builder.newDocument();
        Element audios = (Element)document.appendChild(document.createElement("audios"));
        Element audio = (Element)audios.appendChild(document.createElement("audio"));
        
        if (getTitle() != null)
            audio.setAttribute("title", getTitle());
        
        if (getArtist() != null)
            audio.setAttribute("artist", getArtist());
        
        if (getAlbum() != null)
            audio.setAttribute("album", getAlbum());
        
        if (getGenre() != null)
            audio.setAttribute("genre", getGenre());
        
        if (getLicenseType() != null)
            audio.setAttribute("licensetype", getLicenseType());
        
        if (getTrack() != null)
            audio.setAttribute("track", getTrack());
        
        if (getType() != null)
            audio.setAttribute("type", getType());
        
        if (getYear() != null)
            audio.setAttribute("year", getYear());
        
        if (getLength() != null)
            audio.setAttribute("length", getLength());
        
        if (getLanguage() != null)
            audio.setAttribute("language", getLanguage());
        
        if (getBitrate() != null)
            audio.setAttribute("bitrate", getBitrate());
        
        if (getComments() != null)
            audio.setAttribute("comments", getComments());
        
        if (getCopyright() != null)
            audio.setAttribute("copyright", getCopyright());
        
        return audios;
    }
    
    private Element toXmlDocument() {
        Document doc = _builder.newDocument();
        Element documents = (Element)doc.appendChild(doc.createElement("documents"));
        Element document = (Element)documents.appendChild(doc.createElement("document"));
        
        if (getTitle() != null)
            document.setAttribute("title", getTitle());
        
        if (getTopic() != null)
            document.setAttribute("topic", getTopic());
        
        if (getAuthor() != null)
            document.setAttribute("author", getAuthor());
        
        if (getLicenseType() != null)
            document.setAttribute("licensetype", getLicenseType());
        
        if (getLicense() != null)
            document.setAttribute("license", getLicense());
        
        return documents;
    }
    
    private Element toXmlImage() {
        Document document = _builder.newDocument();
        Element images = (Element)document.appendChild(document.createElement("images"));
        Element image = (Element)images.appendChild(document.createElement("image"));
        
        if (getTitle() != null)
            image.setAttribute("title", getTitle());
        
        if (getDescription() != null)
            image.setAttribute("description", getDescription());
        
        if (getArtist() != null)
            image.setAttribute("artist", getArtist());
        
        if (getLicenseType() != null)
            image.setAttribute("licensetype", getLicenseType());
        
        if (getLicense() != null)
            image.setAttribute("license", getLicense());
        
        return images;
    }
    
    private Element toXmlProgram() {
        Document document = _builder.newDocument();
        Element programs = (Element)document.appendChild(document.createElement("applications"));
        Element program = (Element)programs.appendChild(document.createElement("application"));
        
        if (getTitle() != null)
            program.setAttribute("name", getTitle());
        
        if (getPublisher() != null)
            program.setAttribute("publisher", getPublisher());
        
        if (getPlatform() != null)
            program.setAttribute("platform", getPlatform());
        
        if (getLicenseType() != null)
            program.setAttribute("licensetype", getLicenseType());
        
        if (getLicense() != null)
            program.setAttribute("license", getLicense());
        
        return programs;
    }
    
    /**
     * <videos>
     *   <video
     *     name=""
     *     publisher=""
     *     platform="Windows | OSX | Linux/Unix | Multi-platform"
     *     licensetype="GPL | LGPL | Apache/BSD | X/MIT | Shareware | Creative Commons | Public Domain"
     *     license=""
     *   />
     * </videos>
     */
    private String toStringVideo() {
        return Common.xmlToString(toXmlVideo());
    }
    
    /**
     * <audios>
     *   <audio
     *     title=""
     *     artist=""
     *     album=""
     *     genre="Blues | Classic Rock | Country | Dance | ..."
     *     licensetype="Creative Commons | Weedshare"
     *     track=""
     *     type="Song | Speech | Audiobook | Other"
     *     year=""
     *     seconds=""
     *     language=""
     *     bitrate=""
     *     comments=""
     *     license=""
     *     action=""
     *   />
     * </audios>
     */
    private String toStringAudio() {
        return Common.xmlToString(toXmlAudio());
    }
    
    /**
     * <documents>
     *   <document
     *     title=""
     *     topic=""
     *     author=""
     *     licensetype="Creative Commons | FDL | GPL | Public Domain"
     *     license=""
     *   />
     * </documents>
     */
    private String toStringDocument() {
        return Common.xmlToString(toXmlDocument());
    }
    
    /**
     * <images>
     *   <image
     *     title=""
     *     description=""
     *     artist=""
     *     licensetype="Creative Commons | FDL | GPL | Artistic | Public Domain"
     *     license=""
     *   />
     * </images>
     */
    private String toStringImage() {
        return Common.xmlToString(toXmlImage());
    }
    
    /**
     * <applications>
     *   <application
     *     name=""
     *     publisher=""
     *     platform="Windows | OSX | Linux/Unix | Multi-platform"
     *     licensetype="GPL | LGPL | Apache/BSD | X/MIT | Shareware | Creative Commons | Public Domain"
     *     license=""
     *   />
     * </applications>
     */
    private String toStringProgram() {
        return Common.xmlToString(toXmlProgram());
    }
    
}
