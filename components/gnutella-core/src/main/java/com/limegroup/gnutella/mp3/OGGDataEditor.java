
package com.limegroup.gnutella.mp3;

import java.io.*;

import com.jcraft.jorbis.*;
import com.jcraft.jogg.*;

import com.limegroup.gnutella.ErrorService;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;

/**
 * class which handles specifically the annotation of OGG files.
 * 
 * Note: the library is obviously a java translation from C (not even C++!)
 * very heavy use of arrays...
 */
public class OGGDataEditor extends AudioMetaDataEditor {
	
	File _file;
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.mp3.MetaDataEditor#commitMetaData(java.lang.String)
	 */
	public int commitMetaData(String filename) {
		
		try{
			_file = new File(filename);
			VorbisFile vfile = new VorbisFile(filename);
			Comment [] comments = vfile.getComment();
			
			
			//do things the hard way (grr)
			
			Comment comment = (comments.length == 0 ||
						comments[0]==null) ? 
							new Comment() : comments[0];
			
			comment.comments=7;
			comment.comment_lengths= new int[7];
			
			byte [][] commentBytes = new byte[7][];
			
			String tmp;
			
			System.out.println(title_);
			tmp = OGGMetaData.TITLE_TAG+"="+title_;
			commentBytes[0]=tmp.getBytes();
			comment.comment_lengths[0] = tmp.length();
			
			System.out.println(artist_);
			tmp=OGGMetaData.ARTIST_TAG+"="+artist_;
			commentBytes[1]=tmp.getBytes();
			comment.comment_lengths[1] = tmp.length();
			
			System.out.println(album_);
			tmp=OGGMetaData.ALBUM_TAG+"="+album_;
			commentBytes[2]=tmp.getBytes();
			comment.comment_lengths[2] = tmp.length();
			
			System.out.println(comment_);
			tmp=OGGMetaData.COMMENT_TAG+"="+comment_;
			commentBytes[3]=tmp.getBytes();
			comment.comment_lengths[3] = tmp.length();
			
			System.out.println(genre_);
			tmp=OGGMetaData.GENRE_TAG+"="+genre_;
			commentBytes[4]=tmp.getBytes();
			comment.comment_lengths[4] = tmp.length();
			
			System.out.println(track_);
			tmp=OGGMetaData.TRACK_TAG+"="+track_;
			commentBytes[5]=tmp.getBytes();
			comment.comment_lengths[5] = tmp.length();
			
			System.out.println(year_);
			tmp=OGGMetaData.DATE_TAG+"="+year_;
			commentBytes[6]=tmp.getBytes();
			comment.comment_lengths[6] = tmp.length();
			
			
			comment.user_comments=commentBytes;
			
			JOrbisComment commentHandler = new JOrbisComment();
			commentHandler.update(comment,_file);
			
			
		}catch(JOrbisException failed){
			
			return LimeXMLReplyCollection.RW_ERROR;
		}catch(IOException failed){
			return LimeXMLReplyCollection.RW_ERROR;
		}
		
		return LimeXMLReplyCollection.NORMAL;
		
	}
}
	
	