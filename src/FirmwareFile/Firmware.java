package FirmwareFile;

import java.util.ArrayList;
import java.util.List;

public class Firmware {
    public boolean HasExplictAddress;    
    private List<FirmwareBlock> m_blocks = new ArrayList<FirmwareBlock>();
    public FirmwareBlock[] Blocks(){
        int cnt = this.m_blocks.size();
        return this.m_blocks.toArray(new FirmwareBlock[cnt]);
    }

    public Firmware(boolean hasExplicitAddress){
        this.HasExplictAddress = hasExplicitAddress;
    }

    public void setData(long startAddress, byte[] data) throws Exception{
        
        if( data.length == 0){
            return;
        }
        long endAddress = startAddress + data.length;
        FirmwareBlock startBlock = null;
        FirmwareBlock endBlock = null;
        // 중복된 블록 제거
        RemoveOverwrittenBlocks( startAddress, endAddress );

        for (var block : m_blocks) {
         if( ( block.StartAddress <= startAddress ) && ( ( block.StartAddress + block.getSize() ) >= startAddress ) ) {
            if(startBlock != null){
                throw new Exception("INTERNAL ERROR: Blocks are overlapping");
            }
            startBlock = block;
         }

         if((block.StartAddress <= endAddress) && ((block.StartAddress + block.getSize()) >= endAddress)){
            if(endBlock != null){
                throw new Exception("INTERNAL ERROR: Blocks are overlapping");
            }
            endBlock = block;
         }
        }

        if(endBlock == startBlock){
            endBlock =null;
        }

        if((startBlock != null) && (endBlock != null)){
            startBlock.setDataAtOffset((int) (startAddress - startBlock.StartAddress), data);
            int endBlockOffset = (int) (endAddress - endBlock.StartAddress);
            int tailSize = (int) (endBlock.getSize() - endBlockOffset);
            var tailData = new byte[tailSize];
            Byte[] beforeData = endBlock.getData();
            byte[] endBlockData = new byte[beforeData.length];
            for(int i=0; i< endBlockData.length; i++){
                endBlockData[i] = beforeData[i].byteValue();
            }
            try {
                System.arraycopy(endBlockData, endBlockOffset,tailData,0,tailSize);
            } catch (Exception e) {
                System.out.println(e.getMessage());
            }
            startBlock.AppendData( tailData );
            m_blocks.remove( endBlock );
        }else if( (startBlock != null) && (endBlock == null)){
            startBlock.setDataAtOffset((int)(startAddress - startBlock.StartAddress), data);
        }else if( (startBlock == null) && (endBlock != null)){
            endBlock.setDataAtOffset( - (int)(endBlock.StartAddress - startAddress), data);
        }else{
            m_blocks.add(new FirmwareBlock(startAddress, data));
        }
    }

    // 중복된 블록 제거
    private void RemoveOverwrittenBlocks(long startAddress, long endAddress){
        for (var block : m_blocks.toArray(new FirmwareBlock[m_blocks.size()])) {
            if( ( block.StartAddress >= startAddress ) && ( ( block.StartAddress + block.getSize() ) <= endAddress ) )
            {
                m_blocks.remove( block );
            }
        }
    }



}
