/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: Id3Handler.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.core;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Id3Handler {
	
	private static final int SUPPORTED_VERSION_2_2 = 2;
	private static final int SUPPORTED_VERSION_2_3 = 3; 
	
	private static final String[] genres = { "Blues", "Classic Rock",
			"Country", "Dance", "Disco", "Funk", "Grunge", "Hip-Hop", "Jazz",
			"Metal", "New Age", "Oldies", "Other", "Pop", "R&B", "Rap",
			"Reggae", "Rock", "Techno", "Industrial", "Alternative", "Ska",
			"Death Metal", "Pranks", "Soundtrack", "Euro-Techno", "Ambient",
			"Trip-Hop", "Vocal", "Jazz+Funk", "Fusion", "Trance", "Classical",
			"Instrumental", "Acid", "House", "Game", "Sound Clip", "Gospel",
			"Noise", "AlternRock", "Bass", "Soul", "Punk", "Space",
			"Meditative", "Instrumental Pop", "Instrumental Rock", "Ethnic",
			"Gothic", "Darkwave", "Techno-Industrial", "Electronic",
			"Pop-Folk", "Eurodance", "Dream", "Southern Rock", "Comedy",
			"Cult", "Gangsta", "Top 40", "Christian Rap", "Pop/Funk", "Jungle",
			"Native American", "Cabaret", "New Wave", "Psychadelic", "Rave",
			"Showtunes", "Trailer", "Lo-Fi", "Tribal", "Acid Punk",
			"Acid Jazz", "Polka", "Retro", "Musical", "Rock & Roll",
			"Hard Rock", "Folk", "Folk-Rock", "National Folk", "Swing",
			"Fast Fusion", "Bebob", "Latin", "Revival", "Celtic", "Bluegrass",
			"Avantgarde", "Gothic Rock", "Progressive Rock",
			"Psychedelic Rock", "Symphonic Rock", "Slow Rock", "Big Band",
			"Chorus", "Easy Listening", "Acoustic", "Humour", "Speech",
			"Chanson", "Opera", "Chamber Music", "Sonata", "Symphony",
			"Booty Bass", "Primus", "Porn Groove", "Satire", "Slow Jam",
			"Club", "Tango", "Samba", "Folklore", "Ballad", "Power Ballad",
			"Rhythmic Soul", "Freestyle", "Duet", "Punk Rock", "Drum Solo",
			"Acapella", "Euro-House", "Dance Hall", "Goa", "Drum & Bass",
			"Club-House", "Hardcore", "Terror", "Indie", "BritPop",
			"Negerpunk", "Polsk Punk", "Beat", "Christian Gangsta",
			"Heavy Metal", "Black Metal", "Crossover", "Contemporary C",
			"Christian Rock", "Merengue", "Salsa", "Thrash Metal", "Anime",
			"JPop", "SynthPop"};
	
	public static class Id3v1 {
		
		String id;
		String title;
		String artist;
		String album;
		String year;
		String comment;
		int track;
		int genre;
		
		static Id3v1 readFromFile(RandomAccessFile f) {
			
			byte[] buf = new byte[128];
			try {
				f.seek(f.length()-128);
				f.read(buf);
				
				Id3v1 info = new Id3v1();
				info.id = new String(buf, 0, 3);
				info.title = new String(buf, 3, 30);
				info.artist = new String(buf, 33, 30);
				info.album = new String(buf, 63, 30);
				info.year = new String(buf, 93, 4);
				if (0 == buf[125]) {
					info.comment = new String(buf, 97, 28);
					info.track = buf[126] >= 0 ? buf[126] : buf[126]+256;					
				} else {
					info.comment = new String(buf, 97, 30);
				}
				info.genre = buf[127] >= 0 ? buf[127] : buf[127]+256;
				
				return info;				
			} catch (IOException e) {
				return null;
			}
		}
		
		public void trimFields() {
			
			if (null != title) title = title.trim();
			if (null != artist) artist = artist.trim();
			if (null != album) album = album.trim();
			if (null != year) year = year.trim();
		}
	}
	
	public static class Id3Header {
		
	   String tag;
	   int versionMajor;
	   int versionRevision;
	   int flags;
	   int[] size = new int[4];
	   
	   static Id3Header readFromFile(RandomAccessFile f) {

			byte[] buf = new byte[10];
			try {
				f.read(buf);
				
				Id3Header h = new Id3Header();
				h.tag = new String(buf, 0, 3);
				h.versionMajor = buf[3] >= 0 ? buf[3] : buf[3]+256;
				h.versionRevision = buf[4] >= 0 ? buf[4] : buf[4]+256;
				h.flags = buf[5] >= 0 ? buf[5] : buf[5]+256;
				h.size[0] = buf[6] >= 0 ? buf[6] : buf[6]+256;
				h.size[1] = buf[7] >= 0 ? buf[7] : buf[7]+256;
				h.size[2] = buf[8] >= 0 ? buf[8] : buf[8]+256;
				h.size[3] = buf[9] >= 0 ? buf[9] : buf[9]+256;
				
				return h;				
			} catch (IOException e) {
				return null;
			}
		}
	} 	
	
	public static class FrameHeaderv23 {
		
		String tag;
		int size;
		int flags;
		
		static FrameHeaderv23 readFromFile(RandomAccessFile f) {
						
			try {
				
				
				FrameHeaderv23 h = new FrameHeaderv23();
				
				byte[] buf = new byte[4];
				f.read(buf);
				h.tag = new String(buf);
				h.size = f.readInt();
				h.flags = f.readUnsignedShort();
				
				return h;			
			} catch(Exception e) {
				return null;
			}
		}
		
		   static int getHeaderSize() {
			   return 10;
		   }
		   
		int getFrameSize() {
			return size;
		}
		   
	}
	
	public static class FrameHeaderv22 {
		
	   String tag;
	   byte[] size = new byte[3];
	   
	   static FrameHeaderv22 readFromFile(RandomAccessFile f) {
			
			try {				
				FrameHeaderv22 h = new FrameHeaderv22();
				
				byte[] buf = new byte[3];
				f.read(buf);
				h.tag = new String(buf, 0, 3);
				f.read(h.size);
				
				return h;			
			} catch(Exception e) {
				return null;
			}			
	   }
	   
	   static int getHeaderSize() {
		   return 6;
	   }
	   
	   int getFrameSize() {
		   
		   int b1 = size[0] >= 0 ? size[0] : size[0]+256;
		   int b2 = size[1] >= 0 ? size[1] : size[1]+256;
		   int b3 = size[2] >= 0 ? size[2] : size[2]+256;
		   
		   return (b1 << 16) + (b2 << 8) + b3;
	   }
	} 
	
	public static class Id3Info {
	    String artist;
	    String album;
	    String title;
	    String genre;
	    String year;
	    String encoder;
	    String trackNumber; 		
	}
	
	private static void handleFramev23(String tag, byte[] data, int ofs, int len, Id3Info info) {
		
		if ((null == data) || (0 == data.length)) {
			return;
		}
		
	    if ("TIT2".equals(tag)) {
	    	info.title = new String(data, ofs, len);
	    } else if ("TALB".equals(tag)) {
	    	info.album = new String(data, ofs, len);
	    } else if ("TPE1".equals(tag)) {
	    	info.artist = new String(data, ofs, len);
	    } else if ("TYER".equals(tag)) {
	    	info.year = new String(data, ofs, len);
	    } else if ("TCON".equals(tag)) {
	    	
	    	String genreName = new String(data, ofs, len);
	        for (int i = 0; i < genres.length; i++) {
	        	
	        	if (genres[i].equals(genreName)) {
	        		info.genre = Integer.toString(i);
	        	}
	        }
	    } else if ("TRCK".equals(tag)) {
	    	info.trackNumber = new String(data, ofs, len);
	    } else if ("TSSE".equals(tag)) {
	    	info.encoder = new String(data, ofs, len);
	    } 	
	}

	private static void handleFramev22(String tag, byte[] data, int ofs, int len, Id3Info info) {
		
	    if ((null == data) || (0 == data.length))
	        return;

	    if ("TT2".equals(tag)) {
	    	info.title = new String(data, ofs, len);
	    } else if ("TAL".equals(tag)) {
	    	info.album = new String(data, ofs, len);
	    } else if ("TP1".equals(tag)) {
	    	info.artist = new String(data, ofs, len);
	    } else if ("TYE".equals(tag)) {
	    	info.year = new String(data, ofs, len);
	    } else if ("TSI".equals(tag)) {
	    	info.genre = new String(data, ofs, len);
	    } else if ("TRK".equals(tag)) {
	        info.trackNumber = new String(data, ofs, len);
	    } else if ("TSS".equals(tag)) {
	    	info.encoder = new String(data, ofs, len);
	    } 
	}
	
	private static Id3Info readId3v2Tags(String fileName) {
		
		RandomAccessFile f = null;
		try {
			f = new RandomAccessFile(fileName, "r");
			long fileSize = f.length();
			
			Id3Header head = Id3Header.readFromFile(f);
			if (null == head) {
				return null;
			}
			
			if (!"ID3".equals(head.tag)) {
				return null;
			}
			
			if ((SUPPORTED_VERSION_2_2 != head.versionMajor) && (SUPPORTED_VERSION_2_3 != head.versionMajor)) {
				return null;
			}
			
			long size = (head.size[3] & 0x7F) | ((head.size[2] & 0x7F) << 7)
					| ((head.size[1] & 0x7F) << 14)
					| ((head.size[0] & 0x7F) << 21);
			if (fileSize < size) {
				return null;
			}
		
			if (0 != (head.flags & (1 << 6))) {
				
				int extHeaderSize = f.readInt();
				f.skipBytes(extHeaderSize);
			}
			
			Id3Info info = new Id3Info();
			int frameSize = 0;
			while (size > 0) {
				
				FrameHeaderv22 framev22 = null;
				FrameHeaderv23 framev23 = null;
				
				if (SUPPORTED_VERSION_2_2 == head.versionMajor) {
					
					framev22 = FrameHeaderv22.readFromFile(f);
					if (null == framev22) {
						return null;
					}
					
					frameSize = framev22.getFrameSize();
		        }
				
		        if (SUPPORTED_VERSION_2_3 == head.versionMajor) {
		        	
		        	framev23 = FrameHeaderv23.readFromFile(f);
		        	if (null == framev23) {
		        		return null;
		        	}
		        	
		        	frameSize = framev23.getFrameSize();
		        }

				// If the frame size is funky, skip it and move on
		        if ((0 == frameSize) || (fileSize < frameSize)) {
		        	break;
		        }		            
		        
		        byte[] frameData = new byte[frameSize];
		        int read = f.read(frameData);
		        if (read != frameSize) {
		        	return null;
		        }
		        
		        if (SUPPORTED_VERSION_2_2 == head.versionMajor) {
		        	handleFramev22(framev22.tag, frameData, 1, frameSize-1, info);		        	
		        } else {
		        	handleFramev23(framev23.tag, frameData, 1, frameSize-1, info);
		        }
		        
		        size -= (SUPPORTED_VERSION_2_3 == head.versionMajor ?
		        		FrameHeaderv23.getHeaderSize() : FrameHeaderv22.getHeaderSize()) + frameSize; 				 
			 }			
			
			return info;
			
		} catch (Exception e) {
			return null;
		} finally {
			try { f.close(); } catch(Exception e) {}
		}
	}
	
	private static Id3Info readId3v1Tags(String fileName, Id3Info info) {
		
		RandomAccessFile f = null;
		try {
			f = new RandomAccessFile(fileName, "r");
			Id3v1 id3 = Id3v1.readFromFile(f);
			if ((null == id3) || (!"TAG".equals(id3.id))) {
				return info;
			}
			
			if (null == info) {
				info = new Id3Info();
			}
			
			id3.trimFields();
			if ((null != id3.artist) && (0 < id3.artist.length())) {
				info.artist = id3.artist;
			}
			if ((null != id3.album) && (0 < id3.album.length())) {
				info.album = id3.album;
			}
			if ((null != id3.title) && (0 < id3.title.length())) {
				info.title = id3.title;
			}
			if ((null != id3.year) && (0 < id3.year.length())) {
				try {
					int intYear = Integer.parseInt(id3.year);
					if ((1000 <= intYear) && (intYear < 3000)) {
						info.year = id3.year;
					}
				} catch(Exception e) {				
				}			
			}
			
			if (0 != id3.track) {
				info.trackNumber = Integer.toString(id3.track); 
			}
			
			if (255 != id3.genre) {
				info.genre = Integer.toString(id3.genre);
			}
			
			return info;				
		} catch (FileNotFoundException e) {
			return info;
		} finally {
			try { f.close(); } catch(Exception e) {}
		}
		
	}
	
	public static Id3Info readId3Tags(String fileName) {
		
		return readId3v1Tags(fileName, readId3v2Tags(fileName));  
	}

}
