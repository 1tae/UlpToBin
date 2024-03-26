package FirmwareFile;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;

public class MotorolaFileLoader {
    // c# 라이브러리에 설정되어있던 상수들
    private static int RECORD_TYPE_INDEX = 0;
    private static int BYTE_COUNT_INDEX = 2;
    private static int ADDRESS_INDEX = 4;

    private static int RECORD_TYPE_SIZE = 2;
    private static int BYTE_COUNT_SIZE = 2;
    private static int CHECKSUM_SIZE = 2;

    // recordType
    private enum RecordType {
        S0,
        S1,
        S2,
        S3,
        S4,
        S5,
        S6,
        S7,
        S8,
        S9
    }

    private static class Record {
        public RecordType Type;
        public long Address;
        public byte[] Data;

        public RecordType getType() {
            return this.Type;
        }

        public long getAddress() {
            return this.Address;
        }

        public byte[] getData() {
            return this.Data;
        }

        public Record(RecordType type, long address, byte[] data) {
            Type = type;
            Address = address;
            Data = data;
        }

    }

    public static Firmware Load(String filePath) throws Exception{
        return LoadAsync(filePath);
    }

    public static Firmware LoadAsync(String filePath) throws Exception{
        Path path = Paths.get(filePath);
        try (InputStream inputStream = new FileInputStream(path.toFile())) {
            return LoadAsync(inputStream);
        } catch (IOException e) {
            CompletableFuture<Firmware> future = new CompletableFuture<>();
            future.completeExceptionally(e);
            return null;
        }
    }

    // 파일에서 펌웨어 객체 추출
    public static Firmware LoadAsync(InputStream stream) throws Exception {
        Firmware fwFile = new Firmware(true);
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(stream));
        int lineNumber = 0;
        String line;
        while ((line = fileReader.readLine()) != null) {
            lineNumber++;
            line = line.trim();
            if (line.length() > 0) {
                try {
                    Record record = ProcessLine(line);
                    switch (record.getType()) {
                        case S1:
                        case S2:
                        case S3:
                            fwFile = processDataRecord(record, fwFile);
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    throw new Exception("Error In : " + lineNumber + e.getMessage());
                }
            }
        }
        return fwFile;
    }

    private static Record ProcessLine(String line) throws Exception {
        if (line.length() < (ADDRESS_INDEX + 4 + CHECKSUM_SIZE)) {
            throw new Exception("Truncated record");
        }

        // c# string recordTypeCode = line.Substring( RECORD_TYPE_INDEX, RECORD_TYPE_SIZE );
        String recordTypeCode = line.substring(RECORD_TYPE_INDEX, RECORD_TYPE_INDEX + RECORD_TYPE_SIZE);
        RecordType recordType = ConvertToRecordType(recordTypeCode);

        int byteCount;
        long address;
        int addressSize = GetAddressSize(recordType);

        try {
            /** c# 
                byteCount = Convert.ToInt32( line.Substring( BYTE_COUNT_INDEX, BYTE_COUNT_SIZE ), 16 );
                address = Convert.ToUInt32( line.Substring( ADDRESS_INDEX, addressSize ), 16 );
             */
            byteCount = Integer.parseInt(line.substring(BYTE_COUNT_INDEX, BYTE_COUNT_INDEX+BYTE_COUNT_SIZE), 16);
            address = Integer.parseInt(line.substring(ADDRESS_INDEX, ADDRESS_INDEX + addressSize), 16);
        } catch (Exception e) {
            throw new Exception("Invalid hexadecimal value", e);
        }

        if(line.length() != (ADDRESS_INDEX + (byteCount * 2))){
            throw new Exception("Invalid record length");
        }

        byte calculatedChecksum = (byte) byteCount;
        calculatedChecksum += (byte) ( ( address >> 0 ) & 0xFF );
        calculatedChecksum += (byte) ( ( address >> 8 ) & 0xFF );
        calculatedChecksum += (byte) ( ( address >> 16 ) & 0xFF );
        calculatedChecksum += (byte) ( ( address >> 24 ) & 0xFF );

        int dataSize = byteCount - ((CHECKSUM_SIZE + addressSize)/2);
        int dataIndex = ADDRESS_INDEX + addressSize;

        byte[] data = new byte[dataSize];

        for(int i=0; i<dataSize; i++){
            try {
                String hex = line.substring(dataIndex + (i*2),dataIndex + (i*2)+2);
                data[i] = (byte)Integer.parseInt(hex, 16);
                calculatedChecksum += data[i];
            } catch (Exception e) {
                throw new Exception("Invalid hexadecimal value" + e.getMessage(),e);
            }
        }

        calculatedChecksum = (byte) ~calculatedChecksum;
        byte checksum;

        try{
            String hex = line.substring( dataIndex + ( dataSize * 2 ), dataIndex + ( dataSize * 2 )+CHECKSUM_SIZE );
            checksum = (byte)Integer.parseInt( hex, 16 );
        }catch(Exception e){
            throw new Exception("Invalid hexadeciaml value " + e.getMessage(),e);
        }

        if(checksum != calculatedChecksum){
            throw new Exception("Invalid checksum (exprected: " + calculatedChecksum + "h, reported : " + checksum+ "h)");
        }

        return new Record(recordType, address, data);
    }

    private static RecordType ConvertToRecordType(String recordTypeCode) throws Exception {
        try {
            return RecordType.valueOf(recordTypeCode);
        } catch (Exception e) {
            throw new Exception("Unsupported record type '" + recordTypeCode + "'");
        }
    }

    private static int GetAddressSize(RecordType recordType) {
        switch (recordType) {
            case S2:
                return 6;
            case S3:
                return 8;
            default:
                return 4;
        }
    }

    private static Firmware processDataRecord(Record record, Firmware fwFile) throws Exception {
        fwFile.setData(record.getAddress(), record.getData());
        return fwFile;
    }

}
