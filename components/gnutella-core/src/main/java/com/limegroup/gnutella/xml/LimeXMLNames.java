package com.limegroup.gnutella.xml;

import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.I18n;

public class LimeXMLNames {

    public static final String AUDIO_SCHEMA = "http://www.limewire.com/schemas/audio.xsd";
    
    public static final String AUDIO = "audios__audio";
    public static final String AUDIO_TITLE = "audios__audio__title__";
    public static final String AUDIO_TRACK = "audios__audio__track__";
    public static final String AUDIO_ARTIST = "audios__audio__artist__";
    public static final String AUDIO_ALBUM = "audios__audio__album__";
    public static final String AUDIO_GENRE = "audios__audio__genre__";
    public static final String AUDIO_COMMENTS = "audios__audio__comments__";
    public static final String AUDIO_YEAR = "audios__audio__year__";
    public static final String AUDIO_TYPE = "audios__audio__type__";
    public static final String AUDIO_LANGUAGE = "audios__audio__language__";
    public static final String AUDIO_SECONDS = "audios__audio__seconds__";
    public static final String AUDIO_SHA1 = "audios__audio__SHA1__";
    public static final String AUDIO_BITRATE = "audios__audio__bitrate__";
    public static final String AUDIO_PRICE = "audios__audio__price__";
    public static final String AUDIO_LINK = "audios__audio__link__";
    public static final String AUDIO_ACTION = "audios__audio__action__";
    public static final String AUDIO_LICENSE = "audios__audio__license__";
    public static final String AUDIO_LICENSETYPE = "audios__audio__licensetype__";
    
    public static final String APPLICATION_SCHEMA = "http://www.limewire.com/schemas/application.xsd";
    
    public static final String APPLICATION = "applications__application";
    public static final String APPLICATION_NAME = "applications__application__name__";
    public static final String APPLICATION_PUBLISHER = "applications__application__publisher__";
    public static final String APPLICATION_PLATFORM = "applications__application__platform__";
    public static final String APPLICATION_LICENSETYPE = "applications__application__licensetype__";
    public static final String APPLICATION_LICENSE = "applications__application__license";
    
    public static final String DOCUMENT_SCHEMA = "http://www.limewire.com/schemas/document.xsd";
    
    public static final String DOCUMENT = "documents__document";
    public static final String DOCUMENT_TITLE = "documents__document__title__";
    public static final String DOCUMENT_TOPIC = "documents__document__topic__";
    public static final String DOCUMENT_AUTHOR = "documents__document__author__";
    public static final String DOCUMENT_LICENSE = "documents__document__license__";
    public static final String DOCUMENT_LICENSETYPE = "documents__document__licensetype__";
    
    public static final String IMAGE_SCHEMA = "http://www.limewire.com/schemas/image.xsd";
    
    public static final String IMAGE = "images__image";
    public static final String IMAGE_TITLE = "images__image__title__";
    public static final String IMAGE_DESCRIPTION = "images__image__description__";
    public static final String IMAGE_ARTIST = "images__image__artist__";
    public static final String IMAGE_LICENSE = "images__image__license__";
    public static final String IMAGE_LICENSETYPE = "images__image__licensetype__";
    
    public static final String VIDEO_SCHEMA = "http://www.limewire.com/schemas/video.xsd";
    
    public static final String VIDEO = "videos__video";
    public static final String VIDEO_TITLE = "videos__video__title__";
    public static final String VIDEO_TYPE = "videos__video__type__";
    public static final String VIDEO_YEAR = "videos__video__year__";
    public static final String VIDEO_RATING = "videos__video__rating__";
    public static final String VIDEO_LENGTH = "videos__video__length__";
    public static final String VIDEO_COMMENTS = "videos__video__comments__";
    public static final String VIDEO_LICENSE = "videos__video__license__";
    public static final String VIDEO_LICENSETYPE = "videos__video__licensetype__";
    public static final String VIDEO_HEIGHT = "videos__video__height__";
    public static final String VIDEO_WIDTH = "videos__video__width__";
    public static final String VIDEO_BITRATE = "videos__video__bitrate__";
    public static final String VIDEO_ACTION = "videos__video__action__";
    public static final String VIDEO_DIRECTOR = "videos__video__director__";
    public static final String VIDEO_STUDIO = "videos__video__studio__";
    public static final String VIDEO_LANGUAGE = "videos__video__language__";
    public static final String VIDEO_STARS = "videos__video__stars__";
    public static final String VIDEO_PRODUCER = "videos__video__producer__";
    public static final String VIDEO_SUBTITLES = "videos__video__subtitles__";

    private static Map<String, String> displayNameByKey;

    // TODO fberger only mark these
    public synchronized static String getDisplayName(String key) {
        if (displayNameByKey == null) {
            displayNameByKey = new HashMap<String, String>();
            
            displayNameByKey.put(AUDIO, I18n.tr("Audio"));
            displayNameByKey.put(AUDIO_TITLE, I18n.tr("Title"));
            displayNameByKey.put(AUDIO_ARTIST, I18n.tr("Artist"));
            displayNameByKey.put(AUDIO_ALBUM, I18n.tr("Album"));
            displayNameByKey.put(AUDIO_GENRE, I18n.tr("Genre"));
            displayNameByKey.put(AUDIO_TRACK, I18n.tr("Track"));
            displayNameByKey.put(AUDIO_TYPE, I18n.tr("Type"));
            displayNameByKey.put(AUDIO_SECONDS, I18n.tr("Length"));
            displayNameByKey.put(AUDIO_YEAR, I18n.tr("Year"));
            displayNameByKey.put(AUDIO_LANGUAGE, I18n.tr("Language"));
            displayNameByKey.put(AUDIO_BITRATE, I18n.tr("Bitrate"));
            displayNameByKey.put(AUDIO_COMMENTS, I18n.tr("Comments"));
            displayNameByKey.put(AUDIO_ACTION, I18n.tr("Action"));
            displayNameByKey.put(AUDIO_LICENSE, I18n.tr("Copyright"));
            displayNameByKey.put(AUDIO_LICENSETYPE, I18n.tr("License Type"));

            displayNameByKey.put(VIDEO, I18n.tr("Video"));
            displayNameByKey.put(VIDEO_TITLE, I18n.tr("Title"));
            displayNameByKey.put(VIDEO_DIRECTOR, I18n.tr("Director"));
            displayNameByKey.put(VIDEO_PRODUCER, I18n.tr("Producer"));
            displayNameByKey.put(VIDEO_STUDIO, I18n.tr("Studio"));
            displayNameByKey.put(VIDEO_STARS, I18n.tr("Stars"));
            displayNameByKey.put(VIDEO_TYPE, I18n.tr("Type"));
            displayNameByKey.put(VIDEO_LENGTH, I18n.tr("Length"));
            displayNameByKey.put(VIDEO_YEAR, I18n.tr("Year"));
            displayNameByKey.put(VIDEO_LANGUAGE, I18n.tr("Language"));
            displayNameByKey.put(VIDEO_SUBTITLES, I18n.tr("Subtitles"));
            displayNameByKey.put(VIDEO_RATING, I18n.tr("Rating"));
            displayNameByKey.put(VIDEO_COMMENTS, I18n.tr("Comments"));
            displayNameByKey.put(VIDEO_ACTION, I18n.tr("Action"));
            displayNameByKey.put(VIDEO_LICENSE, I18n.tr("Copyright"));
            displayNameByKey.put(VIDEO_LICENSETYPE, I18n.tr("License Type"));
            displayNameByKey.put(VIDEO_HEIGHT, I18n.tr("Height"));
            displayNameByKey.put(VIDEO_WIDTH, I18n.tr("Width"));
            displayNameByKey.put(VIDEO_BITRATE, I18n.tr("Bitrate"));

            displayNameByKey.put(IMAGE, I18n.tr("Image"));
            displayNameByKey.put(IMAGE_TITLE, I18n.tr("Title"));
            displayNameByKey.put(IMAGE_DESCRIPTION, I18n.tr("Description"));
            displayNameByKey.put(IMAGE_ARTIST, I18n.tr("Artist"));
            displayNameByKey.put(IMAGE_LICENSE, I18n.tr("License"));
            displayNameByKey.put(IMAGE_LICENSETYPE, I18n.tr("License Type"));

            displayNameByKey.put(DOCUMENT, I18n.tr("Document"));
            displayNameByKey.put(DOCUMENT_TITLE, I18n.tr("Title"));
            displayNameByKey.put(DOCUMENT_TOPIC, I18n.tr("Topic"));
            displayNameByKey.put(DOCUMENT_AUTHOR, I18n.tr("Author"));
            displayNameByKey.put(DOCUMENT_LICENSE, I18n.tr("License"));
            displayNameByKey.put(DOCUMENT_LICENSETYPE, I18n.tr("License Type"));

            displayNameByKey.put(APPLICATION, I18n.tr("Application"));
            displayNameByKey.put(APPLICATION_NAME, I18n.tr("Name"));
            displayNameByKey.put(APPLICATION_PUBLISHER, I18n.tr("Publisher"));
            displayNameByKey.put(APPLICATION_PLATFORM, I18n.tr("Platform"));
            displayNameByKey.put(APPLICATION_LICENSETYPE, I18n.tr("License Type"));
            displayNameByKey.put(APPLICATION_LICENSE, I18n.tr("License"));        
        }
        
        return displayNameByKey.get(key);
    }
    
}
