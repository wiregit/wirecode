
package com.limegroup.gnutella.metadata;

import java.io.*;

import com.jcraft.jorbis.*;
import com.jcraft.jogg.*;
 
import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;
import com.limegroup.gnutella.util.*;

/**
 * class which handles specifically the annotation of OGG files.
 * 
 * Note: the library is obviously a java translation from C (not even C++!)
 * very heavy use of arrays...
 */
public class OGGDataEditor extends AudioMetaDataEditor {
	
	File _file;
	
	/**
	 * The constructor for this editor tries to read data from an existing file.
	 * It is possible to invoke this constructor with one file and then commit
	 * the changes to another - it is up to the user.
	 * @param fileName the filename to create the editor for
	 */
	protected OGGDataEditor(String fileName) {
		_file = CommonUtils.getResourceFile(fileName);
		
		//first populate our own data fields..
		//if an exception is thrown the fields remain blank, which is ok.
		if (_file.exists())
			try {
				OGGMetaData existing = (OGGMetaData)MetaData.parse(_file);
				artist_=existing.getArtist();
				album_=existing.getAlbum();
				title_=existing.getTitle();
				year_=existing.getYear();
				track_=new String(""+existing.getTrack());
				comment_=existing.getComment();
				genre_= existing.getGenre();
			}catch(IOException ignored) {}
	}
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.mp3.MetaDataEditor#commitMetaData(java.lang.String)
	 */
	public int commitMetaData(String filename) {
		VorbisFile vfile = null;
		try{
			_file = CommonUtils.getResourceFile(filename);
			vfile = new VorbisFile(filename);
			Comment [] comments = vfile.getComment();
			
			//do things the hard way (grr)
			
			Comment comment = (comments.length == 0 ||
						comments[0]==null) ? 
							new Comment() : comments[0];
			
			comment.comments=7;
			comment.comment_lengths= new int[7];
			
			byte [][] commentBytes = new byte[7][];
			
			String tmp;
			
			
			tmp = OGGMetaData.TITLE_TAG+"="+(title_!=null ? title_ : "");
			commentBytes[0]=tmp.getBytes("UTF-8");
			comment.comment_lengths[0] = commentBytes[0].length;
			

			
			tmp=OGGMetaData.ARTIST_TAG+"="+(artist_!=null ? artist_ : "");
			commentBytes[1]=tmp.getBytes("UTF-8");
			comment.comment_lengths[1] = commentBytes[1].length;
			

			
			tmp=OGGMetaData.ALBUM_TAG+"="+(album_!=null ? album_: "");
			commentBytes[2]=tmp.getBytes("UTF-8");
			comment.comment_lengths[2] = commentBytes[2].length;
			

			tmp=OGGMetaData.COMMENT_TAG+"="+(comment_!=null ? comment_ :"");
			commentBytes[3]=tmp.getBytes("UTF-8");
			comment.comment_lengths[3] = commentBytes[3].length;
			
			
			tmp=OGGMetaData.GENRE_TAG+"="+(genre_!=null ? genre_ : "");
			commentBytes[4]=tmp.getBytes("UTF-8");
			comment.comment_lengths[4] = commentBytes[4].length;
			
			
			tmp=OGGMetaData.TRACK_TAG+"="+(track_ != null ? track_ : "");
			commentBytes[5]=tmp.getBytes();
			comment.comment_lengths[5] = commentBytes[5].length;
			
			
			tmp=OGGMetaData.DATE_TAG+"="+(year_!=null ? year_ :"");
			commentBytes[6]=tmp.getBytes("UTF-8");
			comment.comment_lengths[6] = commentBytes[6].length;
			
			
			comment.user_comments=commentBytes;
			
			JOrbisComment commentHandler = new JOrbisComment();
			commentHandler.update(comment,_file);
			
			
		}catch(JOrbisException failed){
			
			return LimeXMLReplyCollection.RW_ERROR;
		}catch(IOException failed){
			return LimeXMLReplyCollection.RW_ERROR;
		} 
		finally {
			try {
				if (vfile!=null)
				vfile.close();
			}catch(IOException ignored){};
		}
		
		return LimeXMLReplyCollection.NORMAL;
		
	}
}
	
	
