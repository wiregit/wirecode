package org.limewire.ui.swing.util;

import java.text.SimpleDateFormat;

import org.limewire.core.api.FilePropertyKey;
import org.limewire.core.api.library.PropertiableFile;
import org.limewire.util.CommonUtils;
import org.limewire.util.FileUtils;
import org.limewire.util.StringUtils;

import com.google.inject.Singleton;

@Singleton
class PropertiableHeadingsImpl implements PropertiableHeadings {
    private final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("M/d/yyyy");
    
    @Override
    public String getHeading(PropertiableFile propertiable) {
        Object property = propertiable.getProperty(FilePropertyKey.NAME);
        String name = property == null ? "" : property.toString();
        String renderName = "";
        switch (propertiable.getCategory()) {
        case AUDIO:
            String artist = getPropertyString(propertiable, FilePropertyKey.AUTHOR);
            String title = getPropertyString(propertiable, FilePropertyKey.TITLE);
            if (!StringUtils.isEmpty(artist) && !StringUtils.isEmpty(title)) {
                renderName = artist + " - " + title;
            } else {
                renderName = name;
            }
            break;
        case VIDEO:
        case IMAGE:
        case DOCUMENT:
        case PROGRAM:
        case OTHER:
        default:
            renderName = name + "." + getFileExtension(propertiable);
        }
        return renderName.trim();
    }
    
    private String getPropertyString(PropertiableFile propertiable, FilePropertyKey key) {
        Object property = propertiable.getProperty(key);
        return property == null ? null : property.toString();
    }

    private String getFileExtension(PropertiableFile propertiableFile) {
        return FileUtils.getFileExtension(propertiableFile.getFileName());
    }

    @Override
    public String getSubHeading(PropertiableFile propertiable) {
        String subheading = "";

        switch (propertiable.getCategory()) {
        case AUDIO: {
            String albumTitle = getPropertyString(propertiable, FilePropertyKey.ALBUM);
            Long qualityScore = CommonUtils.parseLongNoException(getPropertyString(propertiable, FilePropertyKey.QUALITY));
            Long length = CommonUtils.parseLongNoException(getPropertyString(propertiable, FilePropertyKey.LENGTH));

            boolean insertHypen = false;
            if (!StringUtils.isEmpty(albumTitle)) {
                subheading += albumTitle;
                insertHypen = true;
            }

            if (qualityScore != null) {
                if (insertHypen) {
                    subheading += " - ";
                }
                subheading += I18n.tr("{0} Quality", GuiUtils.toQualityString(qualityScore));
                insertHypen = true;
            }

            if (length != null) {
                if (insertHypen) {
                    subheading += " - ";
                }
                subheading += CommonUtils.seconds2time(length);
            }
        }
            break;
        case VIDEO: {
            Long qualityScore = CommonUtils.parseLongNoException(getPropertyString(propertiable, FilePropertyKey.QUALITY));
            Long length = CommonUtils.parseLongNoException(getPropertyString(propertiable, FilePropertyKey.LENGTH));

            boolean insertHyphen = false;
            if (qualityScore != null) {
                subheading += I18n.tr("{0} Quality", GuiUtils.toQualityString(qualityScore));
                insertHyphen = true;
            }

            if (length != null) {
                if (insertHyphen) {
                    subheading += " - ";
                }
                subheading += CommonUtils.seconds2time(length);
            }
        }
            break;
        case IMAGE: {
            Object time = propertiable.getProperty(FilePropertyKey.DATE_CREATED);
            if (time != null  && time instanceof Long) {
                subheading = DATE_FORMAT.format(new java.util.Date((Long) time));
            }
        }
            break;
        case PROGRAM: {
            Long fileSize = CommonUtils.parseLongNoException(getPropertyString(propertiable, FilePropertyKey.FILE_SIZE));
            if (fileSize != null) {
                subheading = GuiUtils.toUnitbytes(fileSize);
            }
        }
            break;
        case DOCUMENT:
        case OTHER:
        default: {
            // subheading = "{application name}";
            // TODO add name of program used to open this file, not included in
            // 5.0
            Long fileSize = CommonUtils.parseLongNoException(getPropertyString(propertiable, FilePropertyKey.FILE_SIZE));
            if (fileSize != null) {
                subheading = GuiUtils.toUnitbytes(fileSize);
            }
        }
        }
        return subheading;

    }
}
