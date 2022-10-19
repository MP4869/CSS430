
public class SuperBlock {
    private final int defaultInodeBlocks = 64;
    public int totalBlocks;
    public int inodeBlocks;
    public int freeList;
	

	public SuperBlock( int diskSize ) {
        //creates a new superBlock
		byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
        //retrives all data onto superblock
        totalBlocks =  SysLib.bytes2int(superBlock, 0);
        inodeBlocks =SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock, 8);
        if (totalBlocks == diskSize && inodeBlocks > 0 && freeList >= 2) {
            return;
        } else {
            //changes total blocks and calls format method
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
	}
	
	//  helper function
	void sync( ) {
		byte[] superBlock = new byte[Disk.blockSize];
		SysLib.int2bytes( totalBlocks, superBlock, 0 );
		SysLib.int2bytes( inodeBlocks, superBlock, 4 );
		SysLib.int2bytes( freeList, superBlock, 8 );
		SysLib.rawwrite( 0, superBlock );
		SysLib.cerr( "Superblock synchronized\n" );
    }

    //calls format method with default
    void format( ) {
		// default format with 64 inodes
		format( defaultInodeBlocks );
    }

    //block gets formated
	 void format( int files ) {
		inodeBlocks = files;
        for (short i = 0; i < inodeBlocks; i++) {
            //creates a newInode for each i with default flag 0(empty)
            Inode newNode = new Inode();
            newNode.flag = 0;

            newNode.toDisk(i);
        }
        //updates a list of all free blocks
        freeList = 2 + inodeBlocks * 32 / Disk.blockSize;

        for (int i = freeList; i < this.totalBlocks; i++) {
            //new block for each element in superlist
            byte[] Block = new byte[Disk.blockSize];
            for (int j = 0; j < Disk.blockSize; j++) {
                //sets default to 0
                Block[j] = 0;
            }
            SysLib.int2bytes(i + 1, Block, 0);
            //writes block(i position)
            SysLib.rawwrite(i, Block);
        }
        sync();
	 }
	
	// you implement
    //gives a free block
	public int getFreeBlock( ) {

        if(freeList<0)
        {
            //check if freelist has room
            return -1;
        }
      else {
			int position = freeList;
            byte[] Block = new byte[Disk.blockSize];
            SysLib.rawread(position, Block);
            //changes freelist
            freeList = SysLib.bytes2int(Block, 0);

            SysLib.int2bytes(0, Block, 0);
            //writes onto the Block
            SysLib.rawwrite(position, Block);
			return position;
        }
		//return -1;
    
	}
	
    //returns the block associated with block number
	public boolean returnBlock( int bNumber ) {
		if(bNumber < 0) 
		{
            //checks if BlockNumber is not bogus
			return false;
		}
        if(bNumber > totalBlocks)
        {
            return false;
        }
		else{

            byte[] superBlock = new byte[Disk.blockSize];
            //creates a new superBlock
            for (int i = 0; i < Disk.blockSize; i++) 
			{
                //defaults ech element to 0
                superBlock[i] = 0;
            }
            SysLib.int2bytes(freeList, superBlock, 0);
            //changes freelist to the input blockNumber and finally returns true
            SysLib.rawwrite(bNumber, superBlock);
            freeList = bNumber;
            return true;
        }
	}
	
}
