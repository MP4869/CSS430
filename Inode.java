public class Inode {
	public final static int iNodeSize = 32;  
    public final static int directSize = 11; 

    public final static int NoError              = 0;
    public final static int ErrorBlockRegistered = -1;
    public final static int ErrorPrecBlockUnused = -2;
    public final static int ErrorIndirectNull    = -3;
    public int length;                 
    public short count;               
    public short flag;   //0 unused,1 used, 2 , 3.....

    public short direct[] = new short[directSize]; 
    public short indirect;                        

	//default constructor
    Inode ( ) {                 
	length = 0;
	count = 0;
	flag = 1;
	for ( int i = 0; i < directSize; i++ )
	    direct[i] = -1;
	indirect = -1;
    }

	// this method will create the inode from elements in the disk
	Inode ( short iNumber ) {           
		//get starting block       
		int blkNumber = 1 + iNumber / 16;          
		byte[] data = new byte[Disk.blockSize]; 
		SysLib.rawread( blkNumber, data );         
		//gets first of the inode
		int offset = ( iNumber % 16 ) * iNodeSize; 

		length = SysLib.bytes2int( data, offset ); // retrieve all data members
		offset += 4;                               // from data
		count = SysLib.bytes2short( data, offset );
		offset += 2;
		flag = SysLib.bytes2short( data, offset );
		offset += 2;
		for ( int i = 0; i < directSize; i++ ) {
			//converts the data from bytes to short
			direct[i] = SysLib.bytes2short( data, offset );
			offset += 2;
		}
		indirect = SysLib.bytes2short( data, offset );
		offset += 2;//unnneccesary
    }

 	
	// inode will be saved to disk in this method
	int toDisk( short iNumber ) {     
		//create mew byte array to write to
				byte[] data = new byte[Disk.blockSize];
				int offset = 0;
				//retrive all of the data
				SysLib.int2bytes(length, data, offset);
				offset += 4;
				SysLib.short2bytes(count, data, offset);
				offset += 2;
				SysLib.short2bytes(flag, data, offset);
				offset += 2;	
				for (int i = 0; i < directSize; i++) {
					//converts from shorts to bytes
					SysLib.short2bytes(direct[i], data, offset);
					offset += 2;
				}
				SysLib.short2bytes(indirect, data, offset);
			    offset+=2;
				//gets the starting block
				int bNumber = (iNumber / 16) + 1;
				offset+=2;
				byte[] buf = new byte[Disk.blockSize];
				//reads from disk
				SysLib.rawread(bNumber, buf);
				offset = (iNumber % 16) * iNodeSize;
				System.arraycopy(data, 0, buf, offset, iNodeSize);
				//writes the inode to the disk
				SysLib.rawwrite(bNumber, buf);
				return iNodeSize;
	}
}
