package FirmwareFile;

import java.util.List;
import java.util.ArrayList;

public class FirmwareBlock {
    private List<Byte> m_data;
    public long StartAddress;

    public long getStartAddress(){
        return this.StartAddress;         
    }

    public void setStartAddress(long StartAddress){
        this.StartAddress = StartAddress;
    }

    public Byte[] getData(){
        return this.m_data.toArray(new Byte[this.m_data.size()]);
    }

    public long getSize(){
        return this.m_data.size();
    }

    public FirmwareBlock(long StartAddress, byte[] data){
        this.StartAddress = StartAddress;
        List<Byte> bytes = new ArrayList<>();
        for (byte b : data) {
            bytes.add(b);
        }
        this.m_data = bytes;
    }

    public void setDataAtAddress(long address, byte[] data) {
        int offset = (((int) address) - ((int) this.StartAddress));
        setDataAtOffset(offset,data);
    }

    public void setDataAtOffset(int offset,byte[] data){
        int endOffset = offset + data.length;
        List<Byte> bytes = new ArrayList<>();
        for (byte b : data) {
            bytes.add(b);
        }

        if((offset>=0) && (offset <= this.getSize())){
            if(endOffset < this.getSize()){
                this.m_data.subList(offset,offset + data.length).clear();
            }else{
                this.m_data.subList(offset,((int)this.getSize())).clear();
            }
            
            this.m_data.addAll(offset, bytes);
            
        }else if( (offset < 0) && (endOffset >=0)){
            this.StartAddress -= Math.abs(offset);
            if(endOffset >= this.getSize()){
                this.m_data.clear();
            }else if(endOffset > 0){
                this.m_data.subList(0, endOffset).clear();
            }
            this.m_data.addAll(0, bytes);
        }else{
            throw new IllegalArgumentException("Inserted data region does not overlap the block data region");
        }
    }

    // 데이터 추가
    public void AppendData(byte[] data){
        List<Byte> bytes = new ArrayList<>();
        for(byte b : data){
            bytes.add(b);
        }
        if(data != null){
            this.m_data.addAll(bytes);
        }
    }
}
