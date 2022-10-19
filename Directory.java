import java.util.*;

public class Directory {
	private static int maxChars = 30; // limits fileName to 30 characters

    private int fsizes[];             // file size
    private char fnames[][];          // filename character array

    public Directory ( int maxInumber ) {   
	//max number sets the maximum files    
	fsizes = new int[maxInumber];           
	for ( int i = 0; i < maxInumber; i++ )  // all file sizes set to 0
	    fsizes[i] = 0;
		fnames = new char[maxInumber][maxChars];

	String root = "/";                      // entry(inode) 0 is "/"
	fsizes[0] = root.length( );
	root.getChars( 0, fsizes[0], fnames[0], 0 ); 
    }
	

	//converts from bytes to location in directory
	public void bytes2directory( byte data[] ) {
		//offset is defualted to zero
		int offset = 0;
        for (int i = 0; i < fsizes.length; i++) {
			//converts the file size from bytes to int and adjusts offset accordingly
            fsizes[i] = SysLib.bytes2int(data, offset);
            //offset is has 4 added as each int is 4 bytes
			offset += 4;  
        }
        for (int i = 0; i < fsizes.length; i++) {
			//creates the name from the data and offset
            String name = new String(data, offset, maxChars * 2);
            //chars of name are stored in fnames
			name.getChars(0, fsizes[i], fnames[i], 0);
            //*2 because 2 bytes in a char(in java atleast)
			offset += maxChars * 2;
        }
	}

	// converts directory info to byte array to write back on disk in the future
	public byte[] directory2bytes( ) {
		byte[] data = new byte[fsizes.length * 4 + fnames.length * maxChars * 2];

		int offset = 0;
		for ( int i = 0; i < fsizes.length; i++, offset += 4 )
			//changes by 4 each time in the loop because a int has 4 bytes
			SysLib.int2bytes( fsizes[i], data, offset );

		for ( int i = 0; i < fnames.length; i++, offset += maxChars * 2 ) {
			String tEntry = new String( fnames[i], 0, fsizes[i] );
			//creates byte array using tEntry
			byte[] bytes = tEntry.getBytes( );
			System.arraycopy( bytes, 0, data, offset, bytes.length );
			}
			return data;

		}

		//allocates new inode# for filename input
		public short ialloc ( String filename ) {
		// filename is the name of a file to be created.
		// allocates a new inode number for this filename.
		// i = 0 is already used for "/"
		short i;
		for ( i = 1; i < fsizes.length; i++ ) {
			if ( fsizes[i] == 0 ) {
			fsizes[i] = Math.min( filename.length( ), maxChars );
			filename.getChars( 0, fsizes[i], fnames[i], 0 );
			return i;
			}
		}
		return -1;			
    }


	//frees up the seleceted iNumber/iNode
    public boolean ifree ( short iNumber ) {
		if (fsizes[iNumber] > 0) {
            //if statment to check that the filesize actually has data
			//sets it to 0
			fsizes[iNumber] = 0;
            //returns true 
			return true;
        } 
        return false;
    }

	// returns the inumber corresponding to this filename
    public short namei( String filename ) {
	/////
		short i;
		for ( i = 0; i < fsizes.length; i++ ) {
			if ( fsizes[i] == filename.length( ) ) {
				//create new tableEntry for comparision later on
			String tableEntry = new String( fnames[i], 0, fsizes[i] );
			if ( filename.compareTo( tableEntry ) == 0 )
				return i;
			}
		}
		//finished
		return -1;
    }
	
	
}
