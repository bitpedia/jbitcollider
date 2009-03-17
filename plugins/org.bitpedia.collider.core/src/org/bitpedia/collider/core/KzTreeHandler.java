/* (PD) 2006 The Bitzi Corporation
 * Please see http://bitzi.com/publicdomain for more info.
 *
 * $Id: KzTreeHandler.java,v 1.2 2006/07/14 04:58:39 gojomo Exp $
 */
package org.bitpedia.collider.core;

public class KzTreeHandler {
	
	
	/* MD5 hash result size, in bytes */
	private static final int MD5_SIZE = 16;

	/* size of each block independently tiger-hashed, not counting leaf 0x00 prefix */
	private static final int KZTREE_BLOCKSIZE = 1024*32;

	/* size of input to each non-leaf hash-tree node, not counting node 0x01 prefix */
	private static final int KZTREE_NODESIZE = MD5_SIZE*2;

	/* default size of interim values stack, in MD5SIZE
	 * blocks. If this overflows (as it will for input
	 * longer than 2^128 in size), havoc may ensue. */
	private static final int KZTREE_STACKSIZE = MD5_SIZE*113; 	
	
	private int count; /* total blocks processed */
	private byte[] leaf; /* leaf in progress */
	private int blockIndex; /* leaf data */
	private int index; /* index into block */
	private int topIndex; /* top (next empty) stack slot */
	private byte[] nodes;/* stack of interim node values */
	private int gen;
	
	public void analyzeInit() {
		
		leaf = new byte[KZTREE_BLOCKSIZE];
		nodes = new byte[KZTREE_STACKSIZE];
		
		count = 0;
		blockIndex = 0; // working area for blocks
		index = 0;   // partial block pointer/block length
		topIndex = 0; 		
	}
	
	public void analyzeUpdate(byte[] buffer, int ofs, int len) {
		
		
		if (0 != index) { 
			/* Try to fill partial block */
			int left = KZTREE_BLOCKSIZE - index;
			if (len < left) {
				System.arraycopy(buffer, ofs, leaf, blockIndex+index, len);
				index += len;
				return; /* Finished */
			} else {
				System.arraycopy(buffer, ofs, leaf, blockIndex+index, left);
				index = KZTREE_BLOCKSIZE;
				kztreeBlock();
				ofs += left;
				len -= left;
			}
		}

		while (KZTREE_BLOCKSIZE <= len) {
			System.arraycopy(buffer, ofs, leaf, blockIndex, KZTREE_BLOCKSIZE);
			index = KZTREE_BLOCKSIZE;
			kztreeBlock();
			ofs += KZTREE_BLOCKSIZE;
			len -= KZTREE_BLOCKSIZE;
		}
		
		if (0 != (index = len)) {    /* This assignment is intended */
			/* Buffer leftovers */
			System.arraycopy(buffer, ofs, leaf, blockIndex, len);
		} 		
	}
	
	/* A full KZTREE_BLOCKSIZE bytes have become available; 
	 * hash those, and possibly composite together siblings. */
	private void kztreeBlock()	{

	  byte[] md5 = Md5Handler.md5(leaf, index);
	  System.arraycopy(md5, 0, nodes, topIndex, md5.length);
	  topIndex += MD5_SIZE;
	  
	  ++count;
	  gen = count; 
	  while(gen == ((gen >> 1)<<1)) { // while evenly divisible by 2...
	    kztreeCompose();
	    gen = gen >> 1;
	  }
	} 	
	
	private void kztreeCompose() {
		
		if(gen != ((gen >> 1)<<1)) { // compose of generation with odd population
		    // MD5 the only child in place
			//MD5(ctx->top - MD5SIZE,MD5SIZE,ctx->top - MD5SIZE);
			byte[] childMd5 = Md5Handler.md5(nodes, topIndex-MD5_SIZE, MD5_SIZE);
			System.arraycopy(childMd5, 0, nodes, topIndex-MD5_SIZE, MD5_SIZE);
		    
			return;
		}
		int nodeIndex = topIndex - KZTREE_NODESIZE;
		byte[] md5 = Md5Handler.md5(nodes, nodeIndex, KZTREE_NODESIZE);
		System.arraycopy(md5, 0, nodes, nodeIndex, MD5_SIZE);
		topIndex -= MD5_SIZE;              // update top ptr 
	}
	
	
	public byte[] analyzeFinal() {

		// do last partial block, if any
		if(0 < index) {
		    kztreeBlock();
		}
		  
		while(1 < gen) {
		    kztreeCompose();
			gen = (gen + 1) / 2;
		}
		
		if(1 == count) {
			// for the single block case, hash again
			kztreeCompose();
		}
		if(0 == count) {
			// for the zero-length input case, hash nothing.
			kztreeBlock();
		}
		
		byte[] digest = new byte[MD5_SIZE];
		System.arraycopy(nodes, 0, digest, 0, MD5_SIZE);
		
		return digest;
	}
	

}
