import FirmwareFile.*;
import java.util.*;
import java.util.zip.CRC32;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class UlpToBin {
    String filename;
    List<FlashBlock> flashBlock = new ArrayList<FlashBlock>();
    List<Byte> flashData = new ArrayList<Byte>();
    long dataStartAddress;

    // BitConverter.GetBytes메소드 java함수로 변경 및 byte[] 변경된 항목 추가하여 return
    /*
        ----------기존 c#소스 마샬링 부분도 하기 함수로 처리
        byte[] data = new byte[Marshal.SizeOf(block)];
        // 메모리 할당
        IntPtr ptr = Marshal.AllocHGlobal(Marshal.SizeOf(block));

        // 구조체 데이터를 위에서 할당한 메모리 블록으로 마샬링
        Marshal.StructureToPtr(block, ptr, false);

        //관리되지 않는 메모리 포인터의 데이터를 관리되는 8비트 부호 없는 정수 배열로 복사
        Marshal.Copy(ptr, data, 0, Marshal.SizeOf(block));

        //관리되지 않는 메모리에서 이전에 할당한 메모리를 해제
        Marshal.FreeHGlobal(ptr);

        // header에 data값 복사
        Buffer.BlockCopy(data, 0, header, pos, data.Length);
    */
    public byte[] InsertIntToByteArr(byte[] des,int value, int idx){
        int j = 0;
        for(int i =idx; i<idx+4; i++){
            des[i] = (byte) ((value >> (j*8)) & 0xFF);
            j++;
        }
        return des;
    }

    // 생성자
    public UlpToBin(String filename){
        this.filename = filename;
    }

    // ULP 형식 블록별 bin파일 생성
    public boolean MakeBin(){
        // 기존 초기화된 블록과 데이터 초기화
        flashBlock.clear();
        flashData.clear();
        try {
            Firmware firmware = MotorolaFileLoader.Load(filename);
            int iteratorCount = 0;
            for (var fwBlock : firmware.Blocks()) {
                try{
                    byte[] buffer = new byte[(int)fwBlock.getSize()];
                    Byte[] dt = fwBlock.getData();
                    
                    String filename = "C:\\Users\\tashu\\OneDrive\\대동\\004_농기계TMS\\888_c#기반소스\\testresult\\flashdata_" + iteratorCount + ".bin";
                    FileOutputStream fs = new FileOutputStream(filename);
                    for(int i=0; i<dt.length; i++){
                        buffer[i] = dt[i].byteValue();
                    }
                    //  fwBlock.getData() Byte[] to byte[]
                    fs.write(buffer,0,(int)fwBlock.getSize());
                    System.out.println("Buffer saved to file successfully.");
                    fs.close();
                }catch(Exception e){
                    System.out.println("An error occurred while saving buffer to file: " + e.getMessage());
                }
                iteratorCount++;
            }

        } catch (Exception e) {
            System.out.println("An error occurred while saving buffer to file: " + e.getMessage());
        }

        return true;
    }

    /* 
    ECU의 경우
    binary 의 시작주소를 기준으로 block을 나눔.
    블록을 나누는 기준은 데이터의 중간에 0xFF가 256 개 이상일 경우 0xFF 이전까지를 하나의 블록으로 나누고 이후 0xFF가 아닌 값이 나오는 구간부터 다음 블록으로 지정함.

    DDR의 경우 
    0번주소부터 4FFFFF 주소까지의 데이터가 입력되어 있음. 따라서 start값은 false로 시작
    해당 데이터에 대하여 ECU bin file 생성방법과 동일하게 block을 나누어주면 됨.
    첫번째 블록의 시작은 0xFF가 아닌값이 나오는 주소부터 시작됨.
     */
    public boolean MakeBlock(int startAddress, Byte[] data, int size, boolean start){
        boolean blockstart = start;
        long blockAddress = (long)startAddress;
        long dataSize = 0;
        long ffCount = 0;
        long iterationCount = 0;
        long crc32 = 0;

        for (byte b : data) {
            if(blockstart){
                dataSize++;
                if(b == (byte)0xFF){
                    //0xFF값 체크
                    ffCount++;
                }else{
                    ffCount = 0;
                }
                // block쪼개기
                // 데이터 중간에 0xFF값이 256개일경우 이전까지를 하나의 블록으로 묶음
                if(ffCount == 0x100){ 
                    // dataSize에서 0xFF값 제외
                    dataSize -= 256;
                    // 현재까지의 데이터를 블록으로 묶어서 리스트에 추가
                    flashBlock.add(new FlashBlock(blockAddress,dataStartAddress,dataSize,crc32));
                    Byte[] blockdata = new Byte[(int)dataSize];
                    System.arraycopy(data, (int)(blockAddress-startAddress), blockdata, 0, (int)dataSize);
                    flashData.addAll(Arrays.asList(blockdata));

                    // 다음 블록 시작 주소 계산을 위해 dataSize만큼 더함
                    dataStartAddress += dataSize;
                    blockstart = false;
                }

            }else{
                if(b== (byte)0xFF){ 
                }else{
                    // 블록의 시작은 0xFF가 아닌 값이 나오는 주소부터 시작
                    ffCount = 0;
                    dataSize = 1;
                    blockstart = true;
                    blockAddress = startAddress + iterationCount;
                }
            }
            iterationCount++;
        }

        if(blockstart){
            // 0xFF값이 256개가 되기 전에 바이트배열 읽기가 종료되었을 경우
            // 현재까지 데이터를 블록으로 묶어서 리스트에 추가
            // 상기 반복문에서의 block생성 로직과 같음
            flashBlock.add(new FlashBlock(blockAddress, dataStartAddress, dataSize, crc32));
            Byte[] blockdata = new Byte[(int)dataSize];
            System.arraycopy(data, (int)(blockAddress - startAddress), blockdata, 0, (int)dataSize);
            flashData.addAll(Arrays.asList(blockdata));
            dataStartAddress += dataSize;
        }

        return true;
    }

    // ulp파일 변환
    public boolean MakeDelphiBin(){
        flashBlock.clear();
        flashData.clear();
        
        try{
            // 펌웨어 객체 형태로 파일 로드
            // 펌웨어 로드 관련 .NET github라이브러리 참조하여 변환
            // https://github.com/jgonzalezdr/FirmwareFile
            Firmware firmware = MotorolaFileLoader.Load(filename);

            // 하기에 작성할 헤더 사이즈가 512이므로 512번째 인덱스 부터 읽기
            dataStartAddress = 512;
            
            for (var fwBlock : firmware.Blocks()) {
                MakeBlock((int)fwBlock.StartAddress,fwBlock.getData(), (int)fwBlock.getSize(), true);
            }

            try {
                int pos = 0;
                // 변환될 파일의 경로 및 이름
                String filename = "C:\\Users\\tashu\\Documents\\test_result\\flashdata_ulp.bin";
                // 헤더 사이즈 512로 설정
                // 헤더 값 설정 시작
                byte[] header = new byte[512];
                byte[] blockCount = new byte[Integer.BYTES]; 
                blockCount = InsertIntToByteArr(blockCount,flashBlock.size(),0);

        
                String sysname = "ECU";
                byte[] sysnameBytes = sysname.getBytes(StandardCharsets.US_ASCII);
                System.arraycopy(sysnameBytes, 0, header, pos, sysnameBytes.length);
                pos += 16;

                System.arraycopy(blockCount, 0, header, pos, blockCount.length);
                pos += blockCount.length;

                for (FlashBlock block : flashBlock) {
                    /*
                    원본 C# 소스 - FlashBlock 객체를 byte[]로 데이터 마샬링 
                    변환한 코드 -  객체 인스턴스별 value를 byte[]로 변환하여 byte[]로 반환
                     => ByteArrayOutputStream을 이용해 객체 자체를 byte[]로 변환하면 결과값이 다름
                    
                     C#
                     byte[] data = new byte[Marshal.SizeOf(block)];
                     IntPtr ptr = Marshal.AllocHGlobal(Marshal.SizeOf(block));
                     Marshal.StructureToPtr(block, ptr, false);
                     Marshal.Copy(ptr, data, 0, Marshal.SizeOf(block));
                     Marshal.FreeHGlobal(ptr);
                     Buffer.BlockCopy(data, 0, header, pos, data.Length);
                     pos += data.Length; 
                    */
                   byte[] data = new byte[Integer.BYTES * 4]; 
                   data = InsertIntToByteArr(data,(int)block.startAddr,0);
                   data = InsertIntToByteArr(data,(int)block.dataAddr,4);
                   data = InsertIntToByteArr(data,(int)block.size,8);
                   data = InsertIntToByteArr(data,(int)block.crc32,12);
                   System.arraycopy(data, 0, header, pos, data.length);
                   pos += data.length;
                }
                // 헤더 값 설정 종료

                try (FileOutputStream fileStream = new FileOutputStream(filename)) {
                    // 버퍼의 데이터를 파일로 저장
                    fileStream.write(header);
                    for (byte b : flashData) {
                        fileStream.write(b);
                    }
                }catch(Exception e){
                    System.out.println("File write Error : " + e.getMessage());
                }
                splitFile(filename);
                System.out.println("Buffer saved to file successfully.");
            } catch (Exception e) {
                System.out.println("An error occurred while saving buffer to file: " + e.getMessage());
            }

        }catch(Exception e){
            System.out.println("ERROR : " + e.getMessage());
        }

        return true;
    }

    // SCR형태 파일 변환
    public boolean MakeScrBin(){
        // flashdata_0.bin 형식의 파일 생성
        MakeBin();

        flashBlock.clear();
        flashData.clear();
        
        try {
            // 펌웨어 객체 형태로 파일 로드
            // 펌웨어 로드 관련 .NET github라이브러리 참조하여 변환
            // https://github.com/jgonzalezdr/FirmwareFile
            Firmware firmware = MotorolaFileLoader.Load(filename);

            List<FlashBlock> flashBlock = new ArrayList<FlashBlock>();
            List<Byte> flashData = new ArrayList<Byte>();
            dataStartAddress = 512;
            for (var fwBlock : firmware.Blocks()) {
                // 파일 훼손여부를 검증하기 위한 CRC32 객체
                CRC32 crc32 = new CRC32();
                Byte[] fwdata;
                if(fwBlock.StartAddress == 0x030000){
                    fwdata = new Byte[0xFF98];
                    System.arraycopy(fwBlock.getData(), 0, fwdata, 0, fwdata.length);
                }else{
                    fwdata = new Byte[(int)fwBlock.getSize()];
                    System.arraycopy(fwBlock.getData(), 0, fwdata, 0, fwdata.length);
                }
                byte[] result = new byte[fwdata.length];
                for(int i=0;i<result.length;i++){
                    result[i] = fwdata[i].byteValue();
                }
                crc32.update(result);

                //CRC32는 int32 value이므로 long으로 반환
                long crc = crc32.getValue();
                
                flashBlock.add(new FlashBlock(fwBlock.getStartAddress(), dataStartAddress, fwBlock.getSize(), crc));
                Byte[] data = new Byte[(int)fwBlock.getSize()];
                System.arraycopy(fwBlock.getData(),0,data,0,(int)fwBlock.getSize());
                flashData.addAll(Arrays.asList(data));
                dataStartAddress += fwBlock.getSize();
            }

            try {
                // 파일에 대한 FileStream작성
                int pos = 0;
                String filename = "C:\\Users\\tashu\\Documents\\test_result\\flashscrdata.bin";
                byte[] header = new byte[512];
                byte[] blockCount = new byte[Integer.BYTES]; 
                blockCount = InsertIntToByteArr(blockCount,flashBlock.size(),0);
                String sysname = "SCR";
                byte[] sysnameBytes = sysname.getBytes(StandardCharsets.US_ASCII);
                System.arraycopy(sysnameBytes, 0, header, pos, sysnameBytes.length);
                pos += 16;
                System.arraycopy(blockCount, 0, header, pos, blockCount.length);
                pos += blockCount.length;

                for (FlashBlock block : flashBlock) {
                    /*
                    원본 C# 소스 - FlashBlock 객체를 byte[]로 데이터 마샬링 
                    변환한 코드 -  객체 인스턴스별 value를 byte[]로 변환
                     => ByteArrayOutputStream을 이용해 객체 자체를 byte[]로 변환하면 결과값이 다름
                    
                     C#
                     byte[] data = new byte[Marshal.SizeOf(block)];
                     IntPtr ptr = Marshal.AllocHGlobal(Marshal.SizeOf(block));
                     Marshal.StructureToPtr(block, ptr, false);
                     Marshal.Copy(ptr, data, 0, Marshal.SizeOf(block));
                     Marshal.FreeHGlobal(ptr);
                     Buffer.BlockCopy(data, 0, header, pos, data.Length);
                     pos += data.Length; 
                    */
                    byte[] data = new byte[Integer.BYTES * 4]; 
                    data = InsertIntToByteArr(data,(int)block.startAddr,0);
                    data = InsertIntToByteArr(data,(int)block.dataAddr,4);
                    data = InsertIntToByteArr(data,(int)block.size,8);
                    data = InsertIntToByteArr(data,(int)block.crc32,12);
                    System.arraycopy(data, 0, header, pos, data.length);
                    pos += data.length;
                }
                
                try (FileOutputStream fileStream = new FileOutputStream(filename)) {
                    fileStream.write(header);
                    for (Byte b : flashData) {
                        fileStream.write(b);
                    }
                }catch(Exception e){
                    System.out.println("File write Error : " + e.getMessage());
                }
                splitFile(filename);
                System.out.println("Buffer saved to file successfully.");
            } catch (Exception e) {
                System.out.println("An error occurred while saving buffer to file: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("ERROR : " + e.getMessage());
        }

        return true;
    }

    // byte[] 값을 unsigned int32 값으로 변경
    // java에서 unsigned int32는 long으로 변환되여 mod64기반으로 계산하는 것을 고려
    public static int byteArrayToUInt32(byte[] data, int offset) {
        if (data == null || data.length < offset + 4) {
            return 0;
        }
        // unsigned int32는 상위 비트32개가 모두 0으로 채워진 64비트 값 이므로
        // 기존 비트 연산에 32만큼 추가로 더 밀어냄
        int result = ((data[offset] & 0xFF) << 32)
                + ((data[offset + 1] & 0xFF) << 40)
                + ((data[offset + 2] & 0xFF) << 48)
                + ((data[offset + 3] & 0xFF) << 56);

        return result;
    }

    // byte[] 값을 int16값으로 변경
    public static final short toInt16(byte[] value, int startIndex) throws IndexOutOfBoundsException {
        if (startIndex == value.length - 1) {
            throw new IndexOutOfBoundsException(String.format("index must be less than %d", value.length - 2));
        }
        return (short) (((value[startIndex + 1] & 0xFF) << 8) | (value[startIndex] & 0xFF));
    }

    public boolean MakeDdrBin(){
        flashBlock.clear();
        flashData.clear();
        
        try {
            /* 
            TripleDes Decrypt을 이용하여 파일을 복호화 한다.
            key = "DAEDONG FT4SCAN CONVERT ";
            iv는 사용하지 않음
            tripleDES mode: cipherMode.ECB
            tripleDES Padding: PaddingMode.None
            복호화 파일은 header 와 body로 구성되어 있음.
             */
            byte[] encryptdata = Files.readAllBytes(Paths.get(filename));
            // 키 생성 - 24바이트 키가 필요하여 뒤에 공백 필수.
            byte[] secretKey = "DAEDONG FT4SCAN CONVERT ".getBytes();
            SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey, "TripleDES");

            // ECB,NoPadding 옵션으로 복호화
            Cipher encryptCipher = Cipher.getInstance("TripleDES/ECB/NoPadding");
            encryptCipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] ddrData = encryptCipher.doFinal(encryptdata);

            int pos = 0;
            byte[] arrayBytes = new byte[4];
            System.arraycopy(ddrData, pos, arrayBytes, 0, 4);
            pos += 4;
            int size = byteArrayToUInt32(arrayBytes,0);

            arrayBytes = new byte[2];
            System.arraycopy(ddrData, pos, arrayBytes, 0, 2);
            pos += 2;

            arrayBytes = new byte[20];
            System.arraycopy(ddrData, pos, arrayBytes, 0, 20);
            pos += 20;

            System.arraycopy(ddrData, pos, arrayBytes, 0, 20);
            pos += 20;

            System.arraycopy(ddrData, pos, arrayBytes, 0, 20);
            pos += 20;

            System.arraycopy(ddrData, pos, arrayBytes, 0, 20);
            pos += 20;

            System.arraycopy(ddrData, pos, arrayBytes, 0, 20);
            pos += 20;

            byte[] bindata = new byte[size - pos - 4];
            System.arraycopy(ddrData, pos, bindata, 0, size - pos - 4);
            dataStartAddress = 512;
            Byte[] fwdata = new Byte[bindata.length];
            for(int i=0;i<bindata.length;i++){
                fwdata[i] = bindata[i];
            }
            MakeBlock(0, fwdata, fwdata.length, false);

            try {
                // 파일 스트림 생성
                pos = 0;
                // 파일명과 경로 설정
                String filename = "C:\\Users\\tashu\\Downloads\\flashdata_ddr.bin";
                byte[] header = new byte[512];
                byte[] blockCount = new byte[Integer.BYTES]; 
                blockCount = InsertIntToByteArr(blockCount,flashBlock.size(),0);

                String sysname = "ECU";
                byte[] sysnameBytes = sysname.getBytes(StandardCharsets.US_ASCII);
                System.arraycopy(sysnameBytes, 0, header, pos, sysnameBytes.length);
                pos += 16;

                System.arraycopy(blockCount, 0, header, pos, blockCount.length);
                pos += blockCount.length;

                for (FlashBlock block : flashBlock) {
                    /*
                    원본 C# 소스 - FlashBlock 객체를 byte[]로 데이터 마샬링 
                    변환한 코드 -  객체 인스턴스별 value를 byte[]로 변환하여 byte[]로 반환
                     => ByteArrayOutputStream을 이용해 객체 자체를 byte[]로 변환하면 결과값이 다름
                    
                     C#
                     byte[] data = new byte[Marshal.SizeOf(block)];
                     IntPtr ptr = Marshal.AllocHGlobal(Marshal.SizeOf(block));
                     Marshal.StructureToPtr(block, ptr, false);
                     Marshal.Copy(ptr, data, 0, Marshal.SizeOf(block));
                     Marshal.FreeHGlobal(ptr);
                     Buffer.BlockCopy(data, 0, header, pos, data.Length);
                     pos += data.Length; 
                    */
                   byte[] data = new byte[Integer.BYTES * 4]; 
                   data = InsertIntToByteArr(data,(int)block.startAddr,0);
                   data = InsertIntToByteArr(data,(int)block.dataAddr,4);
                   data = InsertIntToByteArr(data,(int)block.size,8);
                   data = InsertIntToByteArr(data,(int)block.crc32,12);
                   System.arraycopy(data, 0, header, pos, data.length);
                   pos += data.length;
                }

                try (FileOutputStream fileStream = new FileOutputStream(filename)) {
                    // 버퍼의 데이터를 파일로 저장
                    fileStream.write(header);
                    for (byte b : flashData) {
                        fileStream.write(b);
                    }
                    fileStream.close();
                }catch(Exception e){
                    System.out.println("File write Error : " + e.getMessage());
                }
                splitFile(filename);
                // 파일 저장 완료 시 출력
                System.out.println("Buffer saved to file successfully.");

            } catch (Exception e) {
                System.out.println("An error occurred while saving buffer to file: " + e.getMessage());
            }

        } catch (Exception e) {
            System.out.println("ERROR : " + e.getMessage());
        }
        
        return true;
    }

    // 바이너리 파일 256kb 이하로 분할하는 함수
    public void splitFile(String filename) {
        // split 파일명 설정용
        int pos = filename.lastIndexOf( "." );
        String PREFIX_FILE_DIR = filename.substring(0, pos);
    
        File ifile = new File(filename); 
        FileInputStream fis;
        FileOutputStream chunk;

        String newName;
        // 전체 파일 길이
        int fileSize = (int) ifile.length();
        // 반복을 위한 인덱스,읽은 파일 크기,나눌 파일 크기 초기화
        int nChunks = 0, read = 0, readLength = 256*1024;
        byte[] byteChunk;
        try {
            fis = new FileInputStream(ifile);
            /*
             * fileSize를 256kb씩 읽으면서 split파일 생성
             * fileSize에 읽은 길이 만큼 차감하며 read
            */
            while (fileSize > 0) {
                // 남은 filesize가 256kb이하일때 남은 filesize만큼만 읽도록 설정
                if (fileSize <= 256*1024) {
                    readLength = fileSize;
                }
                // 256kb / 256kb미만일 경우 해당 길이만큼 byte[] 초기화
                byteChunk = new byte[readLength];
                read = fis.read(byteChunk, 0, readLength);
                fileSize -= read;
                // assert를 사용하여 읽은 파일길이와 위에서 초기화한 byte[]의 길이가 일치하는지 검증
                assert(read==byteChunk.length);
                nChunks++;
                // 파일명 설정
                newName = PREFIX_FILE_DIR + "_splited_" + Integer.toString(nChunks - 1)+".bin";
                chunk = new FileOutputStream(new File(newName));
                chunk.write(byteChunk);
                chunk.flush();
                chunk.close();
                byteChunk = null;
                chunk = null;
            }
            fis.close();
            fis = null;
            System.out.println("file split completed");
        } catch ( Exception e ) {
            System.out.println("file split error");
        }
    }

}