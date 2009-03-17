/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: ImageFormatHandler.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.image;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.LinkedHashMap;
import java.util.Map;

import org.bitpedia.collider.core.FormatHandler;
import org.bitpedia.collider.core.Submission;

public class ImageFormatHandler implements FormatHandler {
	
	private String errorString;

	public boolean supportsExtension(String ext) {
		return "bmp".equalsIgnoreCase(ext) || "gif".equalsIgnoreCase(ext)
				|| "jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)
				|| "png".equalsIgnoreCase(ext);
	}

	public boolean supportsMemAnalyze() {
		return false;
	}

	public boolean supportsFileAnalyze() {
		return true;
	}

	public void analyzeInit() {
	}

	public void analyzeUpdate(byte[] buf, int bufLen) {
	}

	public Map analyzeFinal() {
		return null;
	}
	
	private int[] parseBmp(DataInputStream stm) throws IOException {
		
		/* File must start with "BM" */
		if (('B' != stm.readByte()) || ('M' != stm.readByte())) {
			return null;
		}
		
		stm.skip(16);
		
		byte[] buf = new byte[12];
		stm.read(buf);
		
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		   
		int[] params = new int[3];
		params[0] = bb.getInt();
		params[1] = bb.getInt();
		bb.position(bb.position()+2);
		params[2] = bb.getShort();
		   
		return params;
	}
	
	private int[] parseGif(DataInputStream stm) throws IOException {
		
		/* File must start with "GIF" */
		if (('G' != stm.readByte()) || ('I' != stm.readByte()) || ('F' != stm.readByte())) {
			return null;
		}
		
		stm.skip(3);
		
		byte[] buf = new byte[5];
		stm.read(buf);
		
		ByteBuffer bb = ByteBuffer.wrap(buf);
		bb.order(ByteOrder.LITTLE_ENDIAN);
		
		int[] params = new int[3];
		params[0] = bb.getShort();
		params[1] = bb.getShort();
		/* packed byte: 
		 * Bits 8 and 5 are flags we don't need to worry about; 
		 * bits 6-8 and 1-3 are 3-bit descriptions of the number of bits, 
		 * minus 1, of "color resolution" and "bits per pixel" respectively.
		 * Usually these values are the same, but if they're not, take the
		 * larger of the two to be "bpp," since this is what standard 
		 * image editing programs seem to do.  I don't know why.
		 */
		byte packed = bb.get();
		int bpp1 = ((packed & 0x70) >> 4) + 1;
		int bpp2 = (packed & 0x07) + 1;
		if(bpp1 > bpp2) {
			params[2] = bpp1;
		} else {
			params[2] = bpp1;
		}
		   
		return params; 		
		
	}
	
	private int[] parseJpg(DataInputStream stm) throws IOException {
		   
		/* File must start with 0xFFD8FFE0, <2 byte field length>, "JFIF", 0x00 */
		if (0xFFD8FFE0 != stm.readInt()) {
//				((byte)0xFF != stm.readByte()) || ((byte)0xD8 != stm.readByte()) || 
//				((byte)0xFF != stm.readByte()) || ((byte)0xE0 != stm.readByte())) {
			return null;
		}
		
		short bytesLeft = stm.readShort();
		bytesLeft -= 2; /* 2 bytes of the field length indicator itself count */
		
		if (('J' != stm.readByte()) || ('F' != stm.readByte()) || 
				('I' != stm.readByte()) || ('F' != stm.readByte())) {
			return null;
		}
		
		bytesLeft -= 4;
		stm.skip(bytesLeft);   
		
		/* now we parse the file for the image information field.  JPEG fields
		 * have the general structure of: 0xFF, <1 byte field type>, <2 byte field
		 * length>, <x byte field data>
		 */
		while (true) {

			// uint8 type, samples, bits_per_sample;

			/* if there's no 0xFF marker, JPEG file is malformed */
			byte b = stm.readByte();
			if (-1 != b) {
				return null;
			}
			
			/* JPEG files are sometimes padded with sequential 0xFF bytes */
			byte type;
			do {
				type = stm.readByte();
			} while (0xFF == type);

			/* image information fields (for various types of compression) */
			switch (type & 0xFF) {
			case 0xC0:
			case 0xC1:
			case 0xC2:
			case 0xC3:
			case 0xC5:
			case 0xC6:
			case 0xC7:
			case 0xC9:
			case 0xCA:
			case 0xCB:
			case 0xCD:
			case 0xCE:
			case 0xCF:
				stm.skip(2); /* skip the field length */
				int bitsPerSample = stm.readByte();
				int[] params = new int[3];
				params[1] = stm.readShort();
				params[0] = stm.readShort();
				int samples = stm.readByte();
				params[2] = samples * bitsPerSample;
				return params;

			case 0xD9: /* if end of image, */
			case 0xDA: /* or beginning of compressed data, */
				return null; /* there was no image info (or we missed it) */

			/* if any other field, we don't care, so skip past it */
			default:
				bytesLeft = stm.readShort();
				/* since the length takes 2 bytes, length must be >= 2 */
				if (bytesLeft < 2) {
					return null;
				}
				bytesLeft -= 2;
				/* skip the rest of the field and go on the next one */
				stm.skip(bytesLeft);
			}
		}
	}
	
	private int[] parsePng(DataInputStream stm) throws IOException {
		
		/* File must start with 0x89, "PNG", 0x0D0A1A0A */
		if (stm.readByte() != (byte)0x89 || stm.readByte() != 'P'
				|| stm.readByte() != 'N' || stm.readByte() != 'G'
				|| stm.readByte() != 0x0D || stm.readByte() != 0x0A
				|| stm.readByte() != 0x1A || stm.readByte() != 0x0A) {
			return null;
		}

		/* Skip IHDR chunk length (since we know its structure already) */
		stm.skip(4);

		/*
		 * Make sure this really is an IHDR chunk (the first chunk must be an
		 * IHDR chunk in a valid PNG file)
		 */
		if (stm.readByte() != 'I' || stm.readByte() != 'H'
				|| stm.readByte() != 'D' || stm.readByte() != 'R') {
			return null;
		}

		int[] params = new int[3];

		/* Read in our data */
		params[0] = stm.readInt();
		params[1] = stm.readInt();

		/* bpp depends on bit_depth and color_type. */
		int bitDepth = stm.readByte();
		int colorType = stm.readByte();

		switch (colorType) {
		case 0: /* grayscale */
		case 3: /* pixels are palette indices */
			params[2] = bitDepth;
			break;
		case 2: /* pixels are RGB triples */
			params[2] = bitDepth * 3;
			break;
		case 4:
			params[2] = bitDepth * 2; /* pixels are grayscale + alpha */
			break;
		case 6:
			params[2] = bitDepth * 4; /* pixels are RGB triples + alpha */
			break;
		default:
			return null; /* invalid color_type */
		}

		return params; 		
	}

	public Map analyzeFile(String fileName) {
		
		DataInputStream stm = null;
		try {
			stm = new DataInputStream(new FileInputStream(fileName));
			String ext = Submission.extractExt(fileName);
			int[] imgAttrs;
			String format;
			
			if ("bmp".equalsIgnoreCase(ext)) {
				imgAttrs = parseBmp(stm);
				format = "BMP";
			} else if ("gif".equalsIgnoreCase(ext)) {
				imgAttrs = parseGif(stm);
				format = "GIF";
			} else if ("jpg".equalsIgnoreCase(ext) || "jpeg".equalsIgnoreCase(ext)) {
				imgAttrs = parseJpg(stm);
				format = "JPEG";
			} else if ("png".equalsIgnoreCase(ext)) {
				imgAttrs = parsePng(stm);
				format = "PNG";
			} else {
				return null;
			}
			
			if (null == imgAttrs) {
				return null;
			}
			
			Map attrs = new LinkedHashMap();
			attrs.put("tag.image.width", ""+imgAttrs[0]);
			attrs.put("tag.image.height", ""+imgAttrs[1]);
			attrs.put("tag.image.bpp", ""+imgAttrs[2]);
			attrs.put("tag.image.format", format);
			
			return attrs;
			
		} catch (IOException e) {
			errorString = e.getMessage();
			return null;
		} finally {
			try { stm.close(); } catch (IOException e) {}
		}
	}

	public String getError() {
		return errorString;
	}

}
