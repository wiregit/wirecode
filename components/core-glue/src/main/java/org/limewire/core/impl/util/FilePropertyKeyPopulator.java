package org.limewire.core.impl.util;

import java.util.Map;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.I18NConvert;

import com.limegroup.gnutella.xml.LimeXMLDocument;
import com.limegroup.gnutella.xml.LimeXMLNames;

/**
 * Given a lime xml document this class will populate the given map by
 * converting limexml values to the appropriate FilePropertyKey.
 */
public class FilePropertyKeyPopulator {
    public static void populateProperties(String fileName, long fileSize,
            long creationTime, Map<FilePropertyKey, Object> properties, LimeXMLDocument doc) {
        
        set(properties, FilePropertyKey.NAME, FileUtils.getFilenameNoExtension(fileName));
        set(properties, FilePropertyKey.DATE_CREATED, creationTime);
        set(properties, FilePropertyKey.FILE_SIZE, fileSize);
        
        String extension = FileUtils.getFileExtension(fileName);
        if (doc != null) {
            if (LimeXMLNames.AUDIO_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, FilePropertyKey.ALBUM, doc.getValue(LimeXMLNames.AUDIO_ALBUM));
                set(properties, FilePropertyKey.AUTHOR, doc.getValue(LimeXMLNames.AUDIO_ARTIST));
                set(properties, FilePropertyKey.BITRATE, doc.getValue(LimeXMLNames.AUDIO_BITRATE));
                set(properties, FilePropertyKey.COMMENTS, doc.getValue(LimeXMLNames.AUDIO_COMMENTS));
                set(properties, FilePropertyKey.GENRE, doc.getValue(LimeXMLNames.AUDIO_GENRE));
                set(properties, FilePropertyKey.LENGTH, doc.getValue(LimeXMLNames.AUDIO_SECONDS));
                set(properties, FilePropertyKey.TRACK_NUMBER, doc
                        .getValue(LimeXMLNames.AUDIO_TRACK));
                set(properties, FilePropertyKey.YEAR, doc.getValue(LimeXMLNames.AUDIO_YEAR));
                set(properties, FilePropertyKey.TITLE, doc.getValue(LimeXMLNames.AUDIO_TITLE));

                Long bitrate = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.AUDIO_BITRATE));

                Long length = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.AUDIO_SECONDS));

                int quality = toAudioQualityScore(extension, fileSize, bitrate, length);
                if (quality > 0) {
                    set(properties, FilePropertyKey.QUALITY, quality);
                }

            } else if (LimeXMLNames.VIDEO_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, FilePropertyKey.AUTHOR, doc.getValue(LimeXMLNames.VIDEO_PRODUCER));
                set(properties, FilePropertyKey.BITRATE, doc.getValue(LimeXMLNames.VIDEO_BITRATE));
                set(properties, FilePropertyKey.COMMENTS, doc.getValue(LimeXMLNames.VIDEO_COMMENTS));
                set(properties, FilePropertyKey.LENGTH, doc.getValue(LimeXMLNames.VIDEO_LENGTH));
                set(properties, FilePropertyKey.HEIGHT, doc.getValue(LimeXMLNames.VIDEO_HEIGHT));
                set(properties, FilePropertyKey.WIDTH, doc.getValue(LimeXMLNames.VIDEO_WIDTH));
                set(properties, FilePropertyKey.YEAR, doc.getValue(LimeXMLNames.VIDEO_YEAR));
                set(properties, FilePropertyKey.TITLE, doc.getValue(LimeXMLNames.VIDEO_TITLE));
                set(properties, FilePropertyKey.RATING, doc.getValue(LimeXMLNames.VIDEO_RATING));
                set(properties, FilePropertyKey.COMPANY, doc.getValue(LimeXMLNames.VIDEO_STUDIO));
                
                Long bitrate = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.VIDEO_BITRATE));
                Long length = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.VIDEO_LENGTH));
                Long height = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.VIDEO_HEIGHT));
                Long width = CommonUtils.parseLongNoException(doc
                        .getValue(LimeXMLNames.VIDEO_WIDTH));

                int quality = toVideoQualityScore(extension, fileSize, bitrate, length, height,
                        width);
                if (quality > 0) {
                    set(properties, FilePropertyKey.QUALITY, quality);
                }
            } else if (LimeXMLNames.APPLICATION_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, FilePropertyKey.NAME, doc.getValue(LimeXMLNames.APPLICATION_NAME));
                set(properties, FilePropertyKey.AUTHOR, doc
                        .getValue(LimeXMLNames.APPLICATION_PUBLISHER));
                set(properties, FilePropertyKey.PLATFORM, doc.getValue(LimeXMLNames.APPLICATION_PLATFORM));
                set(properties, FilePropertyKey.COMPANY, doc.getValue(LimeXMLNames.APPLICATION_PUBLISHER));
            } else if (LimeXMLNames.DOCUMENT_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, FilePropertyKey.NAME, doc.getValue(LimeXMLNames.DOCUMENT_TITLE));
                set(properties, FilePropertyKey.AUTHOR, doc.getValue(LimeXMLNames.DOCUMENT_AUTHOR));
            } else if (LimeXMLNames.IMAGE_SCHEMA.equals(doc.getSchemaURI())) {
                set(properties, FilePropertyKey.NAME, doc.getValue(LimeXMLNames.IMAGE_TITLE));
                set(properties, FilePropertyKey.AUTHOR, doc.getValue(LimeXMLNames.IMAGE_ARTIST));
            }
        }
    }

    /**
     * Sets the given value for the supplied property key. Nothing is set if the
     * value is empty or null. If the supplied value is a String, it is passed
     * through the I18NConvert.compose method before being set.
     */
    public static void set(Map<FilePropertyKey, Object> map, FilePropertyKey property, Object value) {
        // Insert nothing if value is null|empty.
        if (value != null && !value.toString().isEmpty()) {
            if (value instanceof String) {
                value = I18NConvert.instance().compose((String) value);
            }
            map.put(property, value);
        }
    }

    /**
     * TODO use a better analysis to map bit rates and file types to quality,
     * for now using the following articles as a guide for now.
     * 
     * http://www.extremetech.com/article2/0,2845,1560793,00.asp
     * 
     * http://www.cdburner.ca/digital-audio-formats-article/digital-audio-
     * comparison.htm
     * 
     * http://ipod.about.com/od/introductiontoitunes/a/sound_qual_test.htm
     * 
     * Returns 1 of 4 quality scores.
     * 
     * 0 - unscored 1 - poor 2 - good 3 - excellent
     */
    private static int toAudioQualityScore(String fileExtension, Long fileSize, Long bitrate,
            Long length) {
        int quality = 0;
        if ("wav".equalsIgnoreCase(fileExtension) || "flac".equalsIgnoreCase(fileExtension)) {
            quality = 3;
        } else if (bitrate != null) {
            if ("mp3".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 96) {
                    quality = 1;
                } else if (bitrate < 192) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("wma".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 64) {
                    quality = 1;
                } else if (bitrate < 128) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("aac".equalsIgnoreCase(fileExtension)
                    || "m4a".equalsIgnoreCase(fileExtension)
                    || "m4b".equalsIgnoreCase(fileExtension)
                    || "m4p".equalsIgnoreCase(fileExtension)
                    || "m4v".equalsIgnoreCase(fileExtension)
                    || "mp4".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 64) {
                    quality = 1;
                } else if (bitrate < 128) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if ("ogg".equalsIgnoreCase(fileExtension)
                    || "ogv".equalsIgnoreCase(fileExtension)
                    || "oga".equalsIgnoreCase(fileExtension)
                    || "ogx".equalsIgnoreCase(fileExtension)) {
                if (bitrate < 48) {
                    quality = 1;
                } else if (bitrate < 96) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            } else if (length != null && length < 30) {
                quality = 1;
            } else if (fileSize != null) {
                if (fileSize < (1 * 1024 * 1024)) {
                    quality = 1;
                } else if (fileSize < (3 * 1024 * 1024)) {
                    quality = 2;
                } else {
                    quality = 3;
                }
            }
        }
        return quality;
    }

    /**
     * TODO use a better analysis to map video attributes to quality for now
     * using the following articles as a guide for now.
     * 
     * Right now the scoring is somewhat arbitrary.
     * 
     * Returns 1 of 4 quality scores.
     * 
     * 0 - unscored 1 - poor 2 - good 3 - excellent
     */
    private static int toVideoQualityScore(String fileExtension, Long fileSize, Long bitrate,
            Long length, Long height, Long width) {
        int quality = 0;

        if ("mpg".equalsIgnoreCase(fileExtension) && height != null && width != null) {
            if ((height * width) < (352 * 240)) {
                quality = 1;
            } else if ((height * width) < (352 * 480)) {
                quality = 2;
            } else {
                quality = 3;
            }
        } else if (length != null && length < 60) {
            quality = 1;
        } else if (fileSize != null) {
            if (fileSize < (5 * 1024 * 1024)) {
                quality = 1;
            } else if (fileSize < (100 * 1024 * 1024)) {
                quality = 2;
            } else {
                quality = 3;
            }
        }
        return quality;
    }
}
