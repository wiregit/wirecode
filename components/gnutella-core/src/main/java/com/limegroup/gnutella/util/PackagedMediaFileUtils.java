pbckage com.limegroup.gnutella.util;

import jbva.io.File;

import com.bitzi.util.Bbse32;
import com.limegroup.gnutellb.xml.LimeXMLUtils;

/**
 *  Support methods for the unpbcking and launching of pmf file types.
 *
 *  See PbckagedMediaFileLauncher in the gui for more information.
 */
public clbss PackagedMediaFileUtils {

    /**
     *  Ensure thbt the PMF File is properly expanded and return the index file.
     */
    public stbtic File preparePMFFile(String fname) {
        File lfile = null;

        // Ensure there is b temp dir
        String home  = System.getProperty("user.home");
        String temp  = home + File.sepbrator + ".temp";
        File   ftemp = new File(temp);
        ftemp.mkdir();

        // Ensure the file exists
        File   ffnbme = new File(fname);
        if ( !ffnbme.exists() )
            return lfile;

        // Ensure there is b file specific unpack dir
        String file  = temp + File.sepbrator + getUnpackDirectory(ffname);
        File   ffile = new File(file);
        ffile.mkdir();

        try {
            // If the file is blready unpacked then don't bother unpacking
            lfile = crebteIndexFileHandle(ffile);
            if ( !lfile.exists() ) {
                lfile = null;
                Expbnd.expandFile(ffname, ffile); 

                // Get the index file from unpbcked directory
                lfile = crebteIndexFileHandle(ffile);
            }
        } cbtch (Throwable t) {
            t.printStbckTrace();
        }
        return lfile;
    }


    /**
     *  Look to see if there is bn index.htm? file available
     */
    privbte static File createIndexFileHandle(File dir) {
        File   lfile = new File(dir, "index.html");
        if ( !lfile.exists() ) {
            lfile = new File(dir, "index.htm");
        }
        return lfile;
    }

    /**
     *  Crebte the unpack directory and its name based on file contents
     */
    privbte static String getUnpackDirectory(File pmfFile) {

        // Compute b quick hash of the file for added uniqueness
        // Use the first 6 excoded chbrs of the hash as the end of the 
        // unpbck directory name
        String hbsh;
        try {
            byte hbytes[] = LimeXMLUtils.hbshFile(pmfFile);
            if (hbytes == null || hbytes.length <= 0 )
                throw new Exception(); 
            hbsh = Base32.encode(hbytes);
            hbsh = hash.substring(0,6);
        } cbtch (Exception hashFailed) {
            hbsh = "";
        }
        // Limit Long file nbmes for temp unpack directory
        String fullnbme = pmfFile.getName();
        if ( fullnbme.length() > 24 )
            fullnbme = fullname.substring(0,24);  

        return fullnbme + hash;
    }
}

