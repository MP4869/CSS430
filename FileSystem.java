public class FileSystem
{
    private SuperBlock superblock;
    private Directory directory;
    private FileTable filetable;
    private boolean[] isFreeTable;   // is set to true when its free, false if its not

    
    //constructor that has the number of diskblocks inputted into it.
    //creates the file system of passed in size and the bool isfreetable is created
    public FileSystem(int numDiskBlocks)
    {
        //superblock directory and filetable all instantiated
        superblock = new SuperBlock(numDiskBlocks);
        directory = new Directory(superblock.inodeBlocks);
        filetable = new FileTable(directory);

        // set all of isFreeTable to free
        isFreeTable = new boolean[numDiskBlocks];
        // first 3 are not free as that is the superblock and inode
        for (int i = 0; i < 3; i++)
        {
            isFreeTable[i] = false;
        }
        for (int i = 3; i < numDiskBlocks; i++)
        {
            isFreeTable[i] = true;
        }
        FileTableEntry directoryEntry = open("/", "r");
        int directorySize = fsize(directoryEntry);
        if (directorySize > 0) {
            byte[] directoryData = new byte[directorySize];
            read(directoryEntry, directoryData);
            directory.bytes2directory(directoryData);
        }
        close(directoryEntry);
    } 

    
    //sync cache
    void sync()
    {
        FileTableEntry Entry = this.open("/", "w");
        byte[] dataBuffer = this.directory.directory2bytes();
        this.write(Entry, dataBuffer);
        this.close(Entry);
        this.superblock.sync();
    }

    //formats the disk, erases everything and then recreates it based on based on passed in file count
    //returns true if everything works
    boolean format(int fCount)
    {
               // wait for files to all be erased
               while ( filetable.fempty( ) == false )
               ;
       
           //superblock directory and filetable all reformated or reinstatiated
           superblock.format( fCount );
           directory = new Directory(superblock.inodeBlocks);
           filetable = new FileTable(directory);
           return true;
    }

    
    //opens a file of the given file name, has 4 different modes with the following codes
    //w is write, w+ is write/ read, r is read, and a is append
    FileTableEntry open(String fileName, String code)
    {
        if (code == "w" || code == "w+" || code == "r" || code == "a")
        {
            FileTableEntry entry = filetable.falloc(fileName, code);
            return entry;
        }
        else
        {
            //any other case
            return null;
        }
    }

    
    //closes the inputed file entry, will not close unless count is 0 meaning nothing else is using it
    //returns true if closed false if not 
    boolean close(FileTableEntry fileEntry)
    {
        synchronized (fileEntry)
        {
            fileEntry.count--;
        }
        if (fileEntry.count == 0)
        {
            filetable.ffree(fileEntry);
            return true;
        }
        return false;
    }

    
    //returns the file size of the file entry passed in
    int fsize(FileTableEntry fileEntry)
    {
        return fileEntry.inode.length;
    }

    
    //reads the file entry inputed in and returns the file size
    int read(FileTableEntry fileEntry, byte[] buffer)
    {
        int bufSize=0;
        //if mode is write or append return -1 for failure
        if ((fileEntry.mode == "w") || (fileEntry.mode == "a"))
        {   
              return -1;
        }
       

        int iterator=0;
        int Size  = buffer.length;
        //synchronized to fileEntry
        synchronized(fileEntry)
        {
            //while loop to read data from the disk, while seek pointer is less the files size, AND file must data
            while (fileEntry.seekPtr < fsize(fileEntry) && (Size > 0))
            {
                //find the block that the inputed file is in
                int currB = getBlock(fileEntry, fileEntry.seekPtr);
                //if block not found end while loop
                if (currB == -1)
                {
                    break;
                }
                //data buffer with the size of the block size
                byte[] dataBuffer = new byte[Disk.blockSize];
                //read from the disk to the data buffer
                SysLib.rawread(currB, dataBuffer);

                //keeps track of offset from FileTableEntry's seek pointer for the location within the block
                int offset = fileEntry.seekPtr % Disk.blockSize;
                //blocks left to read into the system
                int remainingB = Disk.blockSize - iterator;
                //bytes left to be read in from disk
                int remainingF = fsize(fileEntry) - fileEntry.seekPtr;

                
                //checks which one to use leftover to use and its less then size
                if (remainingB < remainingF) {
                    iterator = remainingB;
                }
                else {
                    iterator = remainingF;
                }

                if (iterator > Size) 
                {
                    iterator = Size;
                }

                //copy data to the buffer
                System.arraycopy(dataBuffer, offset, buffer, bufSize, iterator);
                bufSize += iterator;
                fileEntry.seekPtr += iterator;
                Size -= iterator;

            }
            return bufSize;
        }
    }

    
   

    
    //writes the inputed file entry to the disk from the memory. 
    //returns the number bytes wrtten
    int write( FileTableEntry entry, byte[] buffer)
    {
        
        int bytesWritten = 0; // number of bytes written, return value
        int bufferSize = buffer.length;  // length of the buffer being inputed in
        int blockSize = Disk.blockSize; // size of a block of memory

        // if mode is read only end here 
        if (entry == null || entry.mode.equals("r"))
        {
            return -1;
        }     
        synchronized (entry)
        {

            //while buffer has more continue
            while (bufferSize > 0)
            {
                //location of write within the target block
                int blockloc = getBlock(entry, entry.seekPtr);
                // if the location is -1 get a new block
                if (blockloc == -1)
                {
                    short newLocation = this.findFreeBlock();
                    int pointer = trackBlockTar(entry, entry.seekPtr, newLocation);

                    // if test pointer is -3 there was a block error return -1 and end here after registering blocks
                    if (pointer == -3)
                    {
                        short freeBlock = this.findFreeBlock();
                        if (!trackIndex(entry, freeBlock))
                        {
                            return -1;
                        }
                            
                        if (trackBlockTar(entry, entry.seekPtr, newLocation) != 0)
                        {return -1;
                        }  
                    }
                    else if (pointer == -2 || pointer == -1)
                    {
                        return -1;
                    } 
                    blockloc = newLocation;
                }

               
                byte [] tempBuffer = new byte[blockSize]; //  buffer for holding data from the memory before being sent to the disk
                // read memory to temp buffer
                SysLib.rawread(blockloc, tempBuffer);
                int tempPointer = entry.seekPtr % blockSize;
                // how much more data can the block hold
                int offset = blockSize - tempPointer;

                //  buffer writes to the disk as long as there is space
                if (offset > bufferSize)
                {
                    System.arraycopy(buffer, bytesWritten, tempBuffer, tempPointer, bufferSize);
                    SysLib.rawwrite(blockloc,tempBuffer);
                    bytesWritten += bufferSize;
                    entry.seekPtr +=bufferSize;
                    bufferSize = 0;
                }
                else {
                    System.arraycopy( buffer, bytesWritten, tempBuffer, tempPointer, offset);
                    SysLib.rawwrite(blockloc, tempBuffer);
                    bufferSize -=offset;
                    entry.seekPtr += offset;
                    bytesWritten += offset;
                }
            }

            //reset FileTableEntry's seek 
            if (entry.seekPtr > entry.inode.length)
            {
                entry.inode.length = entry.seekPtr;
            }
            entry.inode.toDisk(entry.iNumber);
            return bytesWritten;
        }
    }

    
    //helper method
     //returns the index of a free block in memory
     short findFreeBlock()
     {
         for(short i = 3; i < isFreeTable.length; i++)
         {
             if (isFreeTable[i])
             {
                 isFreeTable[i] = false;
                 return i;
             }
         }
         //if here disk is full error to console and return -1 to represent this
         System.out.print("No space in disk");
         return -1;
     }

    //deletes a file using the inputed file name, return true if success false if there was an error
    boolean delete(String filename)
    {
        FileTableEntry entry = open(filename, "w");
        if(entry!=null)
        {
            return close(entry) && directory.ifree(entry.iNumber);
        }
        return false;
        
    }

    //sets the seek pointer of the inputed file entry
    int seek(FileTableEntry entry, int offset, int origin)
    {
        synchronized (entry)
        {
            //if origin is 0
                if(origin==0)
                {
                    if(offset < 0)
                    {
                        entry.seekPtr = 0;
                    }
                        
                    else if (offset > this.fsize(entry))
                    {
                        entry.seekPtr = this.fsize(entry);
                    }
                    
                    else
                    {
                        entry.seekPtr = offset;
                    }     
                }

                //if origin is 1
                if(origin==1)
                {
                    if((entry.seekPtr + offset) < 0)
                    {
                        entry.seekPtr = 0;
                    }

                    else if((entry.seekPtr + offset) > this.fsize(entry))
                    {
                        entry.seekPtr = this.fsize(entry);
                    }
                    
                    else
                    {
                        entry.seekPtr += offset;
                    }
                  
                }
                   
                //if origin is 2
               if(origin==2)
               {
                if((this.fsize(entry) + offset) < 0)
                {
                    entry.seekPtr = 0;
                }
    
                 else if((this.fsize(entry) + offset) > this.fsize(entry))
                 {
                    entry.seekPtr = this.fsize(entry);
                 }
                
                else
                {
                    entry.seekPtr = this.fsize(entry) + offset;
                }
               }
            
            return entry.seekPtr;
        }
    }

    //helper method
    //registers the target block to an inode pointer if it can
    int trackBlockTar(FileTableEntry fileEnt , int entry, short offset){
        int target =  entry/Disk.blockSize;
        if (target < 11)
        {
            if(fileEnt.inode.direct[target ] >= 0){
                return -1;
            }
            if ((target > 0 ) && (fileEnt.inode.direct[target - 1 ]==-1))
            {
                return -2;
            }

            fileEnt.inode.direct[target] = offset;
            return 0;
        }

        if ( fileEnt.inode.indirect < 0)
        {

            //if the indirect is less than zero
            return -3;
        }

        else{
            int block = (target - 11) * 2;
            byte[] dataBuffer = new byte[Disk.blockSize];
            SysLib.rawread(fileEnt.inode.indirect,dataBuffer);

          
            if ( SysLib.bytes2short(dataBuffer, block) > 0)
            {
                return -1;
            }
            else
            {
                SysLib.short2bytes(offset, dataBuffer, block);
                SysLib.rawwrite(fileEnt.inode.indirect, dataBuffer);
            }
        }
        return 0;
    }
    //helper method
    //find the address of the given file entry and returns that address, if it cant find it returns -1 to show that
    int getBlock(FileTableEntry entry, int offset)
    {
        //gets target from offset
        int target = (offset / Disk.blockSize);
       
        if (target < 11){
            return entry.inode.direct[target];
        }
        if (entry.inode.indirect < 0){
            return -1;
        }
       
        byte[] dataBuffer = new byte[Disk.blockSize];
        SysLib.rawread(entry.inode.indirect, dataBuffer);

        int block = (target - 11) * 2;
        return SysLib.bytes2short(dataBuffer, block);
    }
    
    //registers an index block for the inode.
    boolean trackIndex(FileTableEntry entry, short blockNumber)
    {
        byte[ ] dataBuffer = new byte[Disk.blockSize];

        for (int i = 0; i < 11; i++)
        {
            if (entry.inode.direct[i] == -1)
            {
                return false;
            }
        }

        if (entry.inode.indirect != -1)
        {
            return false;
        }

        entry.inode.indirect = blockNumber;
        
        for(int i = 0; i < (Disk.blockSize/2); i++)
        {
            SysLib.short2bytes((short) -1, dataBuffer, i * 2);
        }
        SysLib.rawwrite(blockNumber, dataBuffer);
        return true;
    }  
}