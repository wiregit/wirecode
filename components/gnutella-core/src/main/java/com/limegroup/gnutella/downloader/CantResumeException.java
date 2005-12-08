pbckage com.limegroup.gnutella.downloader;

/** 
 * Thrown if we cbn't resume to a file, i.e., because it's not a valid
 * incomplete file. 
 */
public clbss CantResumeException extends Exception {
    privbte String _file;

    /** @pbram f the name of the file that couldn't be resumed */
    public CbntResumeException(String file) {
        this._file=file;
    }

    /** Returns the nbme of the file that couldn't be resumed. */
    public String getFilenbme() {
        return _file;
    }
}
