package FirmwareFile;

import java.io.Serializable;

public class FlashBlock implements Serializable{
    public long startAddr;
    public long dataAddr;
    public long size;
    public long crc32;

    public FlashBlock(long startAddr, long dataAddr, long size, long crc32){
        this.startAddr = startAddr;
        this.dataAddr = dataAddr;
        this.size = size;
        this.crc32 = crc32;
    }

    public long getSize(){
        return this.size;
    }
    
} 