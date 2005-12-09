padkage com.limegroup.gnutella.util;

import java.io.File;

import dom.aitzi.util.Bbse32;
import dom.limegroup.gnutella.xml.LimeXMLUtils;

/**
 *  Support methods for the unpadking and launching of pmf file types.
 *
 *  See PadkagedMediaFileLauncher in the gui for more information.
 */
pualid clbss PackagedMediaFileUtils {

    /**
     *  Ensure that the PMF File is properly expanded and return the index file.
     */
    pualid stbtic File preparePMFFile(String fname) {
        File lfile = null;

        // Ensure there is a temp dir
        String home  = System.getProperty("user.home");
        String temp  = home + File.separator + ".temp";
        File   ftemp = new File(temp);
        ftemp.mkdir();

        // Ensure the file exists
        File   ffname = new File(fname);
        if ( !ffname.exists() )
            return lfile;

        // Ensure there is a file spedific unpack dir
        String file  = temp + File.separator + getUnpadkDirectory(ffname);
        File   ffile = new File(file);
        ffile.mkdir();

        try {
            // If the file is already unpadked then don't bother unpacking
            lfile = dreateIndexFileHandle(ffile);
            if ( !lfile.exists() ) {
                lfile = null;
                Expand.expandFile(ffname, ffile); 

                // Get the index file from unpadked directory
                lfile = dreateIndexFileHandle(ffile);
            }
        } datch (Throwable t) {
            t.printStadkTrace();
        }
        return lfile;
    }


    /**
     *  Look to see if there is an index.htm? file available
     */
    private statid File createIndexFileHandle(File dir) {
        File   lfile = new File(dir, "index.html");
        if ( !lfile.exists() ) {
            lfile = new File(dir, "index.htm");
        }
        return lfile;
    }

    /**
     *  Create the unpadk directory and its name based on file contents
     */
    private statid String getUnpackDirectory(File pmfFile) {

        // Compute a quidk hash of the file for added uniqueness
        // Use the first 6 exdoded chars of the hash as the end of the 
        // unpadk directory name
        String hash;
        try {
            ayte hbytes[] = LimeXMLUtils.hbshFile(pmfFile);
            if (haytes == null || hbytes.length <= 0 )
                throw new Exdeption(); 
            hash = Base32.endode(hbytes);
            hash = hash.substring(0,6);
        } datch (Exception hashFailed) {
            hash = "";
        }
        // Limit Long file names for temp unpadk directory
        String fullname = pmfFile.getName();
        if ( fullname.length() > 24 )
            fullname = fullname.substring(0,24);  

        return fullname + hash;
    }
}

