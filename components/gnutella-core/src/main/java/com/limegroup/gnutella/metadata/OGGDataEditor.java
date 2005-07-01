
package com.limegroup.gnutella.metadata;

import java.io.File;
import java.io.IOException;

import com.jcraft.jorbis.Comment;
import com.jcraft.jorbis.JOrbisException;
import com.jcraft.jorbis.VorbisFile;
import com.limegroup.gnutella.xml.LimeXMLReplyCollection;

/**
 * class which handles specifically the annotation of OGG files.
 * 
 * Note: the library is obviously a java translation from C (not even C++!)
 * very heavy use of arrays...
 */
public class OGGDataEditor extends AudioMetaDataEditor {
	
	/* (non-Javadoc)
	 * @see com.limegroup.gnutella.mp3.MetaDataEditor#commitMetaData(java.lang.String)
	 */
	public int commitMetaData(String filename) {
		VorbisFile vfile = null;
		try{
			File _file = new File(filename);
			vfile = new VorbisFile(filename);
			Comment [] comments = vfile.getComment();
			
			//do things the hard way (grr)
			
			Comment comment = (comments.length == 0 ||
						comments[0]==null) ? 
							new Comment() : comments[0];
			
			comment.comments=8;
			comment.comment_lengths= new int[8];
			
			byte [][] commentBytes = new byte[8][];
			
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
			
			tmp=OGGMetaData.LICENSE_TAG+"="+(license_!=null ? license_ :"");
			commentBytes[7]=tmp.getBytes("UTF-8");
			comment.comment_lengths[7] = commentBytes[7].length;
			
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
	
	
