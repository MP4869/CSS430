import java.util.*;
public class FileTable {
// File Structure Table

	private ArrayList<FileTableEntry> table;
	private Directory dir;
    
    public FileTable ( Directory directory ) {// a default constructor
    table = new ArrayList<>();// instantiate a file table
	dir = directory;                      // instantiate the root directory
    }
    public int Size()
    {
        return this.table.size();
    }
	
    //allocates files given a filename and a r/w mode
	public synchronized FileTableEntry falloc( String filename, String mode ) {
        short iNum = -1;
        Inode inode = null;
        while (true) {
            //gets inum from the file name(uses / to check)
            iNum = (filename.equals("/") ? (short) 0 : dir.namei(filename));
            //if it isnt bogus
            if (iNum >= 0) {
                inode = new Inode(iNum);
                //sets mode according to r only(2)
                if (mode.equals("r")) {
                    if (inode.flag == 2 || inode.flag == 1 || inode.flag == 0) {
                        inode.flag = 2;
                        break;
                    } else if (inode.flag == 3) {
                        try {
                            //waits if it is in use
                            wait();
                        } catch (InterruptedException e) { }
                    }
                } else {
                    //if it is mode is not read
                    if (inode.flag == 1 || inode.flag == 0) {
                        //sets flag to used
                        inode.flag = 3;
                        break;
                    } else {
                        try {
                            //waits if in use
                            wait();
                        } catch (InterruptedException e) { }
                    }
                }
            } else if (!mode.equals("r")) {
                iNum = dir.ialloc(filename);
                //create new Inode and set flag to used
                inode = new Inode(iNum);
                inode.flag = 3;
                break;
            } else {
                return null;
            }
        }
        //increase reference
        inode.count++;
        inode.toDisk(iNum);
        //create and add new entry to the table
        FileTableEntry entry = new FileTableEntry(inode, iNum, mode);
        table.add(entry);
        return entry;
    }

    //method frees up the FileTableEntry input and decrements count of it
    //for the e, it makes space in the file table
    public synchronized boolean ffree( FileTableEntry e ) {
		if (table.remove(e)) {
            e.inode.flag = 0;
			e.inode.count--;
            //e.inode.flag = 0;
            e.inode.toDisk(e.iNumber);
            e = null;
            notify();
            return true;
        }
        return false;
    }

    //checks if is empty
    public synchronized boolean fempty( ) {
	return table.isEmpty( );             // return if table is empty
    }                                        // called before a format
}

