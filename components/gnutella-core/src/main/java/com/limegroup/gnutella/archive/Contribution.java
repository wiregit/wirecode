package com.limegroup.gnutella.archive;

import java.io.IOException;
import java.util.Set;

import com.limegroup.gnutella.FileDesc;

public interface Contribution {
    
	/**
	 * @return the verification URL that should be used for the contribution
	 */
	public String getVerificationUrl();

	/**
	 * @return normalized identifier
	 */
	public String requestIdentifier(String identifier)
			throws IdentifierUnavailableException, IOException;

	/**
	 * only call this after having successfully called requestIdentifier()
	 * @return
	 */
	public String getIdentifier();

	public void upload() throws IOException;

	public void addFileDesc(FileDesc fd);

	public void removeFileDesc(FileDesc fd);

	public boolean containsFileDesc(FileDesc fd);

	public void cancel();

	/**
	 * @return a set of the files in the collection
	 * 
	 * I'm guessing that LinkedHashMap returns a LinkedHashSet for keySet() 
	 * so the order should be in the order they were added
	 *         
	 */
	public Set<FileDesc> getFileDescs();

	public void setTitle(String title);

	public String getTitle();
	
	public void setDescription( String description ) throws DescriptionTooShortException;
	
	public String getDescription();

	public void setMedia(int media);

	public int getMedia();

	public void setCollection(int collection);

	public int getCollection();

	public void setType(int type);

	public int getType();

	public String getPassword();

	public void setPassword(String password);

	public String getUsername();

	public void setUsername(String username);

	/**
	 * Fields You can include whatever fields you like, but the following are
	 * known (possibly semantically)  by the Internet Archive
	 * 
	 * Movies and Audio: date, description, runtime
	 * 
	 * Audio: creator, notes, source, taper 	 
	 *  
	 * Movies: color, contact, country, credits, director, producer,
	 *		production_company, segments, segments, sound, sponsor, shotlist 
	 *
	 * Also see the Dublin Core: http://dublincore.org/documents/dces/
	 * 
	 */

	public void setField(String field, String value);

	public String getField(String field);

	public void removeField(String field);

	public void addListener(UploadListener l);

	public void removeListener(UploadListener l);

	public int getFilesSent();

	public int getTotalFiles();

	public long getFileBytesSent();

	public long getFileSize();

	public long getTotalBytesSent();

	public long getTotalSize();

	public String getFileName();

	public ContributionState getID();
	
}