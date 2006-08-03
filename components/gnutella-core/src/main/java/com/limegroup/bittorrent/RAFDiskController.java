package com.limegroup.bittorrent;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.limegroup.gnutella.Assert;
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.IOUtils;

/**
 * implementation of the DiskController interface using
 * <tt>RandomAccessFile</tt> for io.
 */
class RAFDiskController<F extends File> implements DiskController<F> {
	
	private static final Log LOG = LogFactory.getLog(RAFDiskController.class);
	
	/*
	 * The files of this torrent as an array
	 */
	private final List<F> _files;

	/**
	 * The instances RandomAccessFile for all files contained in this torrent
	 * LOCKING: this reference as well as the elements of the array 
	 */
	private RandomAccessFile[] _fos = null;
	
	/**
	 * a marker that the current file is currently opening.
	 */
	private static final RandomAccessFile[] OPENING = new RandomAccessFile[0];
	
	RAFDiskController(List<F> files) {
		_files = files;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#write(long, byte[])
	 */
	public void write(long startOffset, byte[] data) throws IOException {
		int written = 0;
		int filesSize = _files.size();
		for (int i = 0; i < filesSize && written < data.length; i++) {
			File current = _files.get(i);
			if (startOffset < current.length()) {
				RandomAccessFile currentFile;
				synchronized(this) {
					if (!isOpen())
						throw new IOException("file closed");
					currentFile = _fos[i];
				}
				currentFile.seek(startOffset);
				int toWrite = (int) Math.min(current.length()- startOffset,
						data.length - written);
				
				currentFile.write(data, written, toWrite);
				startOffset += toWrite;
				written += toWrite;
			} 
			startOffset -= current.length();
		}
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#isOpen()
	 */
	public synchronized boolean isOpen() {
		return _fos != null && _fos != OPENING;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#open(boolean, boolean)
	 */
	public List<F> open(boolean complete, boolean isVerifying) throws IOException {
		synchronized(this) {
			if (_fos != null)
				throw new IOException("Files already open(ing)!");
			_fos = OPENING;
		}
		RandomAccessFile []fos = new RandomAccessFile[_files.size()];
		
		// position of the first byte of a file in the torrent
		long pos = 0;
		
		List<F> filesToVerify = null;
		for (int i = 0; i < _files.size(); i++) {
			F file = _files.get(i);

			// if the file is complete, just open it for reading and be done
			// with it
			if (complete) {
				LOG.info("opening torrent in read-only mode");
				fos[i] = new RandomAccessFile(file, "r");
			} else {
				LOG.info("opening torrent in read-write");
				if (!file.exists()) {

					// Ensure that the directory this file is in exists & is
					// writeable.
					// TODO: make sure this doesn't allow trickery with names
					File parentFile = file.getParentFile();
					if (parentFile != null) {
						parentFile.mkdirs();
						FileUtils.setWriteable(parentFile);
					}
					
					file.createNewFile();
					
					// a file is missing, so this must be a new download.
					// if it was not, we need to reverify every file.
					if (!isVerifying) {
						isVerifying = true;
						i = -1; // restart the loop
						continue;
					}

				} 
				
				FileUtils.setWriteable(file);
				fos[i] = new RandomAccessFile(file, "rw");
				
				// if a file exists, try to verify it
				if (isVerifying && file.length() > 0) {
					if (filesToVerify == null)
						filesToVerify = new ArrayList<F>(_files.size());
					filesToVerify.add(file);
				}
			}

			// increment pos to point to the first byte of the next file
			pos += file.length();
		}
		
		synchronized(this) {
			Assert.that(_fos == OPENING);
			_fos = fos;
		}
		
		return filesToVerify;
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#close()
	 */
	public void close() {
		LOG.debug("closing the file");
		final RandomAccessFile [] fos;
		synchronized(this) {
			if (!isOpen())
				return;
			fos = _fos;
			_fos = null;
		}
		// close the files
		for (RandomAccessFile f : fos)
			IOUtils.close(f);
	}

	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#setReadOnly(com.limegroup.bittorrent.TorrentFile)
	 */
	public void setReadOnly(F completed) {
		// TODO: decide if files should be moved to the save
		// location as they are completed.. cool but not trivial
		try {
			int index = _files.indexOf(completed);
			RandomAccessFile rf;
			synchronized(this) {
				if (!isOpen()) 
					return;
				rf = _fos[index];
				_fos[index] = null;
			}
			rf.close();
			rf = new RandomAccessFile(completed.getPath(), "r");
			synchronized(this) {
				if(!isOpen())
					return;
				_fos[index] = rf;
			}
		} catch (FileNotFoundException bs) {
			ErrorService.error(bs);
		} catch (IOException ignored){}
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.bittorrent.DiskController#read(long, byte[], int, int, boolean)
	 */
	public int read(long position, byte[] buf, int offset,
			int length, boolean flush) throws IOException {
		
		if (position < 0)
			throw new IllegalArgumentException("cannot seek negative position "+position);
		else if (offset + length > buf.length)
			throw new ArrayIndexOutOfBoundsException(
					"buffer to small to store supplied number of bytes");
		
		int read = 0;
		for (int i = 0; i < _files.size() && read < length; i++) {
			File f = _files.get(i);
			while (position < f.length() && read < length) {
				RandomAccessFile currentFile;
				synchronized(this) {
					if (!isOpen())
						throw new IOException("file closed");
					currentFile = _fos[i];
				}
				Assert.that(currentFile != null, "file being read & verified at the same time");
				
				if (flush)
					currentFile.getChannel().force(false);
				
				long currentLength = currentFile.length();
				if (currentLength < f.length() && position >= currentLength)
					return read;
				int toRead = (int) Math.min(currentLength - position, length
						- read);
				currentFile.seek(position);
				int t_read = currentFile.read(buf, read + offset, toRead);
				if (t_read == -1)
					throw new IOException();
				position += t_read;
				read += t_read;
			}
			position -= f.length();
		}
		return read;
	}
}
