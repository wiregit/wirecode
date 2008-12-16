package org.limewire.core.api;

import java.util.Arrays;
import java.util.List;

import org.limewire.i18n.I18nMarker;
import org.limewire.ui.swing.util.I18n;


public enum Category {

    AUDIO(I18nMarker.marktr("Audio"), I18nMarker.marktr("Audio")),
    VIDEO(I18nMarker.marktr("Video"), I18nMarker.marktr("Videos")),
    IMAGE(I18nMarker.marktr("Image"), I18nMarker.marktr("Images")),
    DOCUMENT(I18nMarker.marktr("Document"), I18nMarker.marktr("Documents")),
    PROGRAM(I18nMarker.marktr("Program"), I18nMarker.marktr("Programs")),
    OTHER(I18nMarker.marktr("Other"), I18nMarker.marktr("Other")),
    ;
    
    private final String plural;
    private final String singular;
    
    Category(String singular, String plural) {
        this.singular = singular;
        this.plural = plural;
    }
    
    public String getSingularName() {
        return I18n.tr(singular);
    }
    
    @Override
    public String toString() {
        return I18n.tr(plural);
    }
    
    public static List<Category> getCategoriesInOrder() {
        return Arrays.asList(AUDIO, VIDEO, IMAGE, DOCUMENT, PROGRAM, OTHER);
    }
}