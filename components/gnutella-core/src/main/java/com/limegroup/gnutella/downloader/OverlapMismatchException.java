package com.limegroup.gnutella.downloader;

/**
 * Thrown when overlapped download bytes mismatch, i.e., bytes written to disk
 * don't match non-zero bytes already there.
 */
public class OverlapMismatchException extends Exception {
    private long _fileOffset;
    private long _fileLength;
    private int _bytesDownloaded;
    private int _amountToCheck;
    private int _errorOffset;

    /**
     * @param fileOffset the position to write in the temp file
     * @param fileLength the length of the temp file
     * @param bytesDownloaded the number of bytes downloaded during this
     *  iteration
     * @param amountToCheck the number of bytes to read from disk for
     *  checking, typically min(bytesDownloaded, fileLength-fileOffset)
     * @param errorOffset the offset of the first error in the block read
     */
    public OverlapMismatchException(long fileOffset,
                                    long fileLength,
                                    int bytesDownloaded,
                                    int amountToCheck,
                                    int errorOffset) {
        this._fileOffset=fileOffset;
        this._fileLength=fileLength;
        this._bytesDownloaded=bytesDownloaded;
        this._amountToCheck=amountToCheck;
        this._errorOffset=errorOffset;
    }
    
    public long getFileOffset() { return _fileOffset; }
    public long getFileLength() { return _fileLength; }
    public int getBytesDownloaded() { return _bytesDownloaded; }
    public int getAmountToCheck() { return _amountToCheck; }
    public int getErrorOffset() { return _errorOffset; }    
}
