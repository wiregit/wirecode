package com.limegroup.gnutella;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;

import org.limewire.core.api.Category;
import org.limewire.core.api.file.CategoryManager;
import org.limewire.util.FileUtils;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSortedSet;

class CategoryManagerImpl implements CategoryManager {

    private final SortedSet<String> docExt = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("123", "abw", "accdb", "accde", "accdr", "accdt",
            "ans", "asc", "asp", "bdr", "chm", "css", "csv", "dat", "db", "dif", "diz", "doc",
            "docm", "docx", "dotm", "dotx", "dvi", "eml", "eps", "epsf", "fm", "grv", "gsa", "gts",
            "hlp", "htm", "html", "idb", "idx", "iif", "info", "js", "jsp", "kfl", "kwd", "latex",
            "lif", "lit", "log", "man", "mcw", "mdb", "mht", "mhtml", "mny", "msg", "obi", "odp",
            "ods", "odt", "ofx", "one", "onepkg", "ost", "pages", "pdf", "php", "pot", "potm",
            "potx", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx", "ps", "pub", "qba", "qbb", "qdb",
            "qbi", "qbm", "qbw", "qbx", "qdf", "qel", "qfp", "qpd", "qph", "qmd", "qsd", "rtf",
            "scd", "sdc", "sdd", "sdp", "sdw", "shw", "sldx", "sxc", "sxd", "sxp", "sxw", "t01",
            "t02", "t03", "t04", "t05", "t06", "t07", "t08", "t09", "t98", "t99", "ta0", "ta1",
            "ta2", "ta3", "ta4", "ta5", "ta6", "ta7", "ta8", "ta9", "tax", "tax2008", "tex",
            "texi", "toc", "tsv", "tvl", "txf", "txt", "wk1", "wk3", "wk4", "wks", "wp", "wp5",
            "wpd", "wps", "wri", "xhtml", "xlam", "xls", "xlsb", "xlsm", "xlsx", "xltm", "xltx",
            "xml", "xsf", "xsn", "qfx", "qif", "bud", "ofc", "pst", "mbf", "mn1", "mn2", "mn3",
            "mn4", "mn5", "mn6", "mn7", "mn8", "mn9", "m10", "m11", "m12", "m13", "m14", "m15",
            "m16", "boe", "box", "bri", "cnm", "dbx", "eml", "emlx", "idb", "idx", "maildb", "mbg",
            "mbs", "mbx", "mht", "msb", "msf", "msg", "nws", "pmi", "pmm", "pmx", "tbb", "toc",
            "vfb", "zmc", "stw", "odm", "ott", "wpt").build();

    private final SortedSet<String> audioExt = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("mp3", "mpa", "mp1", "mpga", "mp2", "ra", "rm",
            "ram", "rmj", "wma", "wav", "m4a", "m4p", "mp4", "lqt", "ogg", "med", "aif", "aiff",
            "aifc", "au", "snd", "s3m", "aud", "mid", "midi", "rmi", "mod", "kar", "ac3", "shn",
            "fla", "flac", "cda", "mka").build();

    private final SortedSet<String> videoExt = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("mpg", "mpeg", "mpe", "mng", "mpv", "m1v", "vob",
            "mp2", "mpv2", "mp2v", "m2p", "m2v", "mpgv", "vcd", "mp4", "dv", "dvd", "div", "divx",
            "dvx", "smi", "smil", "rm", "ram", "rv", "rmm", "rmvb", "avi", "asf", "asx", "wmv",
            "qt", "mov", "fli", "flc", "flx", "flv", "wml", "vrml", "swf", "dcr", "jve", "nsv",
            "mkv", "ogm", "cdg", "srt", "sub", "flv").build();

    private final SortedSet<String> imageExt = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("gif", "png", "bmp", "jpg", "jpeg", "jpe", "jif",
            "jiff", "jfif", "tif", "tiff", "iff", "lbm", "ilbm", "eps", "mac", "drw", "pct", "img",
            "bmp", "dib", "rle", "ico", "ani", "icl", "cur", "emf", "wmf", "pcx", "pcd", "tga",
            "pic", "fig", "psd", "wpg", "dcx", "cpt", "mic", "pbm", "pnm", "ppm", "xbm", "xpm",
            "xwd", "sgi", "fax", "rgb", "ras").build();

    private final SortedSet<String> programOsxLinuxExt = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("app", "bin", "mdb", "sh", "csh", "awk", "pl",
            "rpm", "deb", "gz", "gzip", "z", "bz2", "zoo", "tar", "tgz", "taz", "shar", "hqx",
            "sit", "dmg", "7z", "jar", "zip", "nrg", "cue", "iso", "jnlp", "rar", "sh").build();

    private final SortedSet<String> programWindowsExt = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).add("exe", "zip", "jar", "cab", "msi", "msp", "arj",
            "rar", "ace", "lzh", "lha", "bin", "nrg", "cue", "iso", "jnlp", "bat", "lnk", "vbs")
            .build();

    private final SortedSet<String> programExt = ImmutableSortedSet.orderedBy(
            String.CASE_INSENSITIVE_ORDER).addAll(programOsxLinuxExt).addAll(programWindowsExt).build();
    
    private final class SetPredicate implements Predicate<String> {
        private final SortedSet<String> set;
        
        public SetPredicate(SortedSet<String> set) {
            this.set = set;
        }
        
        @Override
        public boolean apply(String input) {
            return set.contains(input);
        }
    }
    
    private final Predicate<String> audioPred = new SetPredicate(audioExt);
    private final Predicate<String> videoPred = new SetPredicate(videoExt);
    private final Predicate<String> programPred = new SetPredicate(programExt);
    private final Predicate<String> imagePred = new SetPredicate(imageExt);
    private final Predicate<String> docPred = new SetPredicate(docExt);
    private final Predicate<String> otherPred = new Predicate<String>() {
        public boolean apply(String input) {
            // This would be a terrible implementation for other categories,
            // but for OTHER it is OK because all we can do is say,
            // "do i belong in another category?... if not -> other"
            return getCategoryForExtension(input) == Category.OTHER;
        };
    };
    
    private final Predicate<String> programOsLinuxPred = new SetPredicate(programOsxLinuxExt);
    private final Predicate<String> programWindowsPred = new SetPredicate(programWindowsExt);

    @Override
    public Category getCategoryForExtension(String extension) {
        // note: the extension sets are all case insensitive,
        // so... no lowercasing required.
        if(audioExt.contains(extension)) {
            return Category.AUDIO;
        } else if(videoExt.contains(extension)) {
            return Category.VIDEO;
        } else if(programExt.contains(extension)) {
            return Category.PROGRAM;
        } else if(imageExt.contains(extension)) {
            return Category.IMAGE;
        } else if(docExt.contains(extension)) {
            return Category.DOCUMENT;
        } else {
            return Category.OTHER;
        }
    }

    @Override
    public Category getCategoryForFile(File file) {
        // note: the extension sets are all case insensitive,
        // so... no lowercasing required.        
        String extension = FileUtils.getFileExtension(file);
        return getCategoryForExtension(extension);
    }

    @Override
    public Collection<String> getExtensionsForCategory(Category category) {
        switch(category) {
        case AUDIO:
            return audioExt;
        case DOCUMENT:
            return docExt;
        case IMAGE:
            return imageExt;
        case PROGRAM:
            return programExt;
        case VIDEO:
            return videoExt;
        case OTHER:
            return Collections.emptySet();
        }
        
        throw new IllegalStateException("invalid category: " + category);
    }

    @Override
    public Predicate<String> getFilterForCategory(Category category) {
        switch(category) {
        case AUDIO:
            return audioPred;
        case DOCUMENT:
            return docPred;
        case IMAGE:
            return imagePred;
        case PROGRAM:
            return programPred;
        case VIDEO:
            return videoPred;
        case OTHER:
            return otherPred;
        }
        
        throw new IllegalStateException("invalid category: " + category);
    }

    @Override
    public Predicate<String> getOsxAndLinuxProgramsFilter() {
        return programOsLinuxPred;
    }

    @Override
    public Predicate<String> getWindowsProgramsFilter() {
        return programWindowsPred;
    }

}
