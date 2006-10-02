package com.limegroup.bittorrent;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import com.bitzi.util.Base32;
import com.limegroup.gnutella.RouterService;
import com.limegroup.gnutella.settings.SharingSettings;
import com.limegroup.gnutella.util.CommonUtils;
import com.limegroup.gnutella.util.FileUtils;
import com.limegroup.gnutella.util.MultiCollection;

/**
 * Information about the file hierarchy contained in the torrent.
 */
public class TorrentFileSystem implements Serializable {
	
	private static final long serialVersionUID = 6006838744525690869L;

	/* the name of the torrent */
	private String _name;
	
	/*
	 * the total length of this torrent.
	 */
	private long _totalSize;
	
	/**
	 * a list of <tt>TorrentFile</tt> for every file in this torrent
	 */
	private List<TorrentFile> _files;
	
	/**
	 * any folders that are contained in the torrent
	 */
	private Collection<File> _folders = new HashSet<File>();
	
	/**
	 * a view of the files and folders contained in this torrent
	 */
	private transient Collection<File> _filesAndFolders;
	
	/*
	 * A <tt>File</tt> pointing to the location where the incomplete
	 * torrent is written.
	 */
	private File _incompleteFile;

	/*
	 * A <tt> File </tt> pointing to the file/directory where the completed
	 * torrent will be moved.
	 */
	private File _completeFile;

	
    /**
     * Constructs the file system using the given BTData & hash information.
     * If any of the information is malformed, throws a ValueException.
     * 
     * @param data
     * @param numHashes
     * @param pieceLength
     * @param infoHash
     * @throws ValueException
     */
	TorrentFileSystem(BTData data, int numHashes, long pieceLength, byte [] infoHash) throws ValueException {
	    // name of the torrent, also specifying the directory under which to save the torrents.
		_name = CommonUtils.convertFileName(data.getName());

		// we need to check the name of the torrent, security risk!
		if (_name.length() == 0)
			throw new ValueException("bad torrent name");

		File incompleteDir = SharingSettings.INCOMPLETE_DIRECTORY.getValue();
		try {
			incompleteDir = incompleteDir.getCanonicalFile();
		} catch (IOException iox){}
		
		_incompleteFile = new File(incompleteDir, 
				Base32.encode(infoHash)+File.separator+_name);
		_completeFile = new File(SharingSettings.getSaveDirectory(), _name);
		
        if(data.getFiles() != null) {
            List<BTData.BTFileData> files = data.getFiles();
            List<TorrentFile> torrents = new ArrayList<TorrentFile>(files.size());
            for(BTData.BTFileData file : files)
                torrents.add(new TorrentFile(file.getLength(), new File(_completeFile, file.getPath()).getAbsolutePath()));
            
			if (files.size() == 0)
				throw new ValueException("bad metainfo, no files!");
			
			// add the beginning and ending chunks for each file.
			long position = 0;
			for (TorrentFile file : torrents) {
				file.setBegin((int) (position / pieceLength));
				position += file.length();
				file.setEnd((int) (position / pieceLength));
			}
			
			_files = torrents;
            
            // add folders, for easier conflict checking later on
            for(String folderPath : data.getFolders())
                _folders.add(new File(_completeFile, folderPath));
			_folders.add(_completeFile);
		} else {
            TorrentFile f = new TorrentFile(data.getLength(), _completeFile.getAbsolutePath());
            f.setBegin(0);
            f.setEnd(numHashes);
            
            _files = new ArrayList<TorrentFile>(1);
            _files.add(f);
		}
		
		_totalSize = calculateTotalSize(_files);
	}
	
	/**
	 * Accessor for the total size of this torrent
	 * 
	 * @return long size of the torrent
	 */
	public long getTotalSize() {
		return _totalSize;
	}

	/**
	 * @return the name of this torrent
	 */
	public String getName() {
		return _name;
	}

	/**
	 * @return a list of Files that will be used to write to during download
	 */
	public List<TorrentFile> getIncompleteFiles() {
		List<TorrentFile> ret = new ArrayList<TorrentFile>(_files);
		updateReferences(_incompleteFile,ret);
		return ret;
	}
	
	/**
	 * @return list of files holding the complete torrent.
	 */
	public List<TorrentFile> getFiles() {
		return _files;
	}
	
	/**
	 * @return view of the files and folders that will be created when
	 * the torrent download completes
	 */
	public Collection<File> getFilesAndFolders() {
		if (_filesAndFolders == null)
			_filesAndFolders = new MultiCollection<File>(_files,_folders);
		return _filesAndFolders;
	}
	
	/**
	 * @return true if the given file would conflict with any of the
	 * files that will be used by this torrent when it completes
	 */
	public boolean conflicts(File f) {
		return getFilesAndFolders().contains(f);
	}
	
	/**
	 * @return true if the given file conflicts with any of the files
	 * used by this torrent during download
	 */
	public boolean conflictsIncomplete(File f) {
		return getBaseFile().getParentFile().equals(f);
	}

	/**
	 * @return <tt>File</tt> the parent file for all files in this torrent.
	 */
	public File getBaseFile() {
		return _incompleteFile;
	}

	/**
	 * @return <tt>File</tt> the parent file for all files in this torrent or
	 *         the only file in this torrent
	 */
	public File getCompleteFile() {
		return _completeFile;
	}
	
	/**
	 * changes the location where this torrent will be saved.
	 */
	public void setCompleteFile(File f) {
		updateReferences(f, _files);
		updateFolderReferences(f);
		_completeFile = f;
	}
	
	/**
	 * Updates the files in the specified collection to start at the 
	 * new base path.
	 * The files must already be absolute and be children of _completeFile.
	 * 
	 * @param completeBase the new base path.
	 *
	 */
	private void updateReferences(File completeBase, List<TorrentFile> l) {
		int offset = _completeFile.getAbsolutePath().length();
		String newPath = completeBase.getAbsolutePath();
		for (int i = 0; i < l.size(); i++) {
			TorrentFile current = l.get(i);
			TorrentFile updated = new TorrentFile(current.length(), newPath
					+ current.getPath().substring(offset));
			updated.setBegin(current.getBegin());
			updated.setEnd(current.getEnd());
			l.set(i,updated);
		}
	}
	
	/**
	 * Updates the folders in the torrent to start at the 
	 * new base path.
	 */
	private void updateFolderReferences(File completeBase) {
		int offset = _completeFile.getAbsolutePath().length();
		String newPath = completeBase.getAbsolutePath();
		Collection<File> newFolders = new HashSet<File>(_folders.size());
		for (File f: _folders) 
			newFolders.add(new File(newPath + f.getPath().substring(offset)));
		_folders.clear();
		_folders.addAll(newFolders); 
	}
	
	void moveToCompleteFolder() throws IOException {
		saveFile(_incompleteFile, _completeFile);
		
		FileUtils.deleteRecursive(_incompleteFile.getParentFile());
	}
	
	/**
	 * private helper, copies file, updates FileManager and _files
	 * 
	 * @param incFile
	 *            the <tt>File</tt> from incomplete directory
	 * @param completeDest
	 *            the <tt>File</tt> the file in the save destination
	 * @throws IOException on failure
	 */
	private void saveFile(File incFile, File completeDest) throws IOException {
		completeDest = completeDest.getCanonicalFile();

		FileUtils.setWriteable(completeDest.getParentFile());
		if (incFile.isDirectory()) {
			saveDirectory(incFile, completeDest);
			return;
		}

		// overwrite complete file and make it writeable
		completeDest.delete();
		FileUtils.setWriteable(completeDest);
		if (!FileUtils.forceRename(incFile, completeDest)) 
			throw new IOException("could not rename file " + incFile);
	}

	/**
	 * Saves a directory
	 * 
	 * @param incFile
	 *            the directory to move
	 * @param completeDir
	 *            the destination to move the directory to
	 * @throws IOException if failed.
	 */
	private void saveDirectory(File incFile, File completeDir) 
	throws IOException {

		// we will delete completeDir if it exists and if it is not a directory
		if (completeDir.exists()) {
			// completeDir is not a directory
			if (!completeDir.isDirectory()) {
				if (!(completeDir.delete() && completeDir.mkdirs())) 
					throw new IOException("could not create complete dir " + completeDir);
			}
		} else if (!completeDir.mkdirs()) {
			// completeDir does not exist...
			throw new IOException("could not create complete dir " + completeDir);
		}

		FileUtils.setWriteable(completeDir);

		for (File f : incFile.listFiles()) 
			saveFile(f, new File(completeDir, f.getName())); 

		// remove the empty directory from the incomplete folder.
		// all its contents have been moved to the shared folder
		FileUtils.deleteRecursive(incFile);
	}
	
	void addToLibrary() {
		for (File f : _folders)
			RouterService.getFileManager().addSharedFolder(f);
		for (File f : _files)
			RouterService.getFileManager().addFileIfShared(f);
	}
	
	private static long calculateTotalSize(List<TorrentFile> files) {
		long ret = 0;
		for (File f : files)
			ret += f.length();
		return ret;
	}
}