package com.limegroup.gnutella.xml;

import java.util.HashMap;
import java.util.Map;

import com.limegroup.gnutella.I18n;

public class LimeXMLNames {

    public static final String AUDIO_SCHEMA = "http://www.limewire.com/schemas/audio.xsd";
    
    public static final String AUDIO = "audios_audio";
    public static final String AUDIO_TITLE = "audios_audio_title_";
    public static final String AUDIO_TRACK = "audios_audio_track_";
    public static final String AUDIO_ARTIST = "audios_audio_artist_";
    public static final String AUDIO_ALBUM = "audios_audio_album_";
    public static final String AUDIO_GENRE = "audios_audio_genre_";
    public static final String AUDIO_COMMENTS = "audios_audio_comments_";
    public static final String AUDIO_YEAR = "audios_audio_year_";
    public static final String AUDIO_TYPE = "audios_audio_type_";
    public static final String AUDIO_LANGUAGE = "audios_audio_language_";
    public static final String AUDIO_SECONDS = "audios_audio_seconds_";
    public static final String AUDIO_SHA1 = "audios_audio_SHA1_";
    public static final String AUDIO_BITRATE = "audios_audio_bitrate_";
    public static final String AUDIO_PRICE = "audios_audio_price_";
    public static final String AUDIO_LINK = "audios_audio_link_";
    public static final String AUDIO_ACTION = "audios_audio_action_";
    public static final String AUDIO_LICENSE = "audios_audio_license_";
    public static final String AUDIO_LICENSETYPE = "audios_audio_licensetype_";
    
    public static final String APPLICATION_SCHEMA = "http://www.limewire.com/schemas/application.xsd";
    
    public static final String APPLICATION = "applications_application";
    public static final String APPLICATION_NAME = "applications_application_name_";
    public static final String APPLICATION_PUBLISHER = "applications_application_publisher_";
    public static final String APPLICATION_PLATFORM = "applications_application_platform_";
    public static final String APPLICATION_LICENSETYPE = "applications_application_licensetype_";
    public static final String APPLICATION_LICENSE = "applications_application_license";
    
    public static final String DOCUMENT_SCHEMA = "http://www.limewire.com/schemas/document.xsd";
    
    public static final String DOCUMENT = "documents_document";
    public static final String DOCUMENT_TITLE = "documents_document_title_";
    public static final String DOCUMENT_TOPIC = "documents_document_topic_";
    public static final String DOCUMENT_AUTHOR = "documents_document_author_";
    public static final String DOCUMENT_LICENSE = "documents_document_license_";
    public static final String DOCUMENT_LICENSETYPE = "documents_document_licensetype_";
    
    public static final String IMAGE_SCHEMA = "http://www.limewire.com/schemas/image.xsd";
    
    public static final String IMAGE = "images_image";
    public static final String IMAGE_TITLE = "images_image_title_";
    public static final String IMAGE_DESCRIPTION = "images_image_description_";
    public static final String IMAGE_ARTIST = "images_image_artist_";
    public static final String IMAGE_LICENSE = "images_image_license_";
    public static final String IMAGE_LICENSETYPE = "images_image_licensetype_";
    
    public static final String VIDEO_SCHEMA = "http://www.limewire.com/schemas/video.xsd";
    
    public static final String VIDEO = "videos_video";
    public static final String VIDEO_TITLE = "videos_video_title_";
    public static final String VIDEO_TYPE = "videos_video_type_";
    public static final String VIDEO_YEAR = "videos_video_year_";
    public static final String VIDEO_RATING = "videos_video_rating_";
    public static final String VIDEO_LENGTH = "videos_video_length_";
    public static final String VIDEO_COMMENTS = "videos_video_comments_";
    public static final String VIDEO_LICENSE = "videos_video_license_";
    public static final String VIDEO_LICENSETYPE = "videos_video_licensetype_";
    public static final String VIDEO_HEIGHT = "videos_video_height_";
    public static final String VIDEO_WIDTH = "videos_video_width_";
    public static final String VIDEO_BITRATE = "videos_video_bitrate_";
    public static final String VIDEO_ACTION = "videos_video_action_";
    public static final String VIDEO_DIRECTOR = "videos_video_director_";
    public static final String VIDEO_STUDIO = "videos_video_studio_";
    public static final String VIDEO_LANGUAGE = "videos_video_language_";
    public static final String VIDEO_STARS = "videos_video_stars_";
    public static final String VIDEO_PRODUCER = "videos_video_producer_";
    public static final String VIDEO_SUBTITLES = "videos_video_subtitles_";

    private static Map<String, String> displayNameByKey;
    
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
