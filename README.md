## filename 수정하여 result 파일 경로와 파일명 설정

앱 실행 시 Form1.Form1() 동작
## Form1.Form()
    1.java swing 기반 업로드 화면
    2.파일 업로드 유형 선택
        ECU : .ulp 파일
        DCU : .s19 파일
        DDR : .ddr 파일
    3. 파일 업로드 후 파일 유형 별 함수 실행
        UlpToBin 생성자로 파일명 설정 
        case ECU : UlpToBin.MakeDelphiBin()
        case DCU : UlpToBin.MakeScrBin()
        case DDR : UlpToBin.MakeDdrBin()



## Firmware firmware = MotorolaFileLoader.Load(filename);
        펌웨어 파일 로더 클래스의 Load/LoadAsync 메서드를 사용하여 펌웨어 인스턴스를 로드
        펌웨어 인스턴스의 Blocks 속성을 통해 펌웨어 메모리 블록(Firmware Block)에 액세스
        https://github.com/jgonzalezdr/FirmwareFile .NET 라이브러리 기반으로 작성



## CASE 'ECU' - UlpToBin.MakeDelphiBin()
    ulp파일 변환 함수
    
    1. MakeBlock() 
        binary 의 시작주소를 기준으로 block을 나눔.
        블록을 나누는 기준은 데이터의 중간에 0xFF가 256 개 이상일 경우 0xFF 이전까지를 하나의 블록으로 나누고 이후 0xFF가 아닌 값이 나오는 구간부터 다음 블록으로 지정
        생성한 블록의 주소정보를 FlashBlock로 생성하여 flashBlock(List<FlashBlock>) 전역 변수에 추가
        생성한 블록의 데이터를 byte[]로 flashData(List<Byte>) 전역 변수에 추가
    
    2. 헤더 생성
        길이 512의 byte[] 생성한 후 헤더정보를 byte로 변환하여 System.arraycopy
        MakeBlock()에서 flashBlock에 추가한 FlashBlock객체를 byte[]로 마샬링하여 생성한 byte[]에 추가

    3. 파일쓰기 
        헤더를 우선 파일에 쓰고 
        MakeBlock()에서 추가된 flashData를 반복하여 파일에 씀



## CASE 'DCU' - UlpToBin.MakeScrBin()
    1. MakeBin()
        펌웨어 파일 로더 클래스의 Load/LoadAsync 메서드를 사용하여 펌웨어 인스턴스를 로드
        로드한 파일의 블록별로 반복하며 byte[]로 변환 후 파일 생성
    
    2. CRC32 기반 데이터 생성
        firmware.Blocks의 주소정보와 CRC32값으로 FlashBlock로 생성하여 flashBlock(List<FlashBlock>) 전역 변수에 추가
        firmware.Blocks의 데이터를 byte[]로 flashData(List<Byte>) 전역 변수에 추가

    3. 헤더 생성
        길이 512의 byte[] 생성한 후 헤더정보를 byte로 변환하여 System.arraycopy
        CRC32 데이터 생성 시 flashBlock에 추가한 FlashBlock객체를 byte[]로 마샬링하여 생성한 byte[]에 추가

    4. 파일쓰기 
        헤더를 우선 파일에 쓰고 
        flashData를 반복하여 파일에 씀



## CASE 'DDR' - UlpToBin.MakeDdrBin()
    1. Decrypt
        TripleDes Decrypt을 이용하여 파일을 복호화
        key = "DAEDONG FT4SCAN CONVERT " - 공백까지 24자리;
        iv는 사용하지 않음
        tripleDES mode: cipherMode.ECB
        tripleDES Padding: PaddingMode.None
        복호화 파일은 header 와 body로 구성되어 있음.    
    
    2. MakeBlock()
        복호화한 bindata 기반으로 block을 나눔
        0xFF가 아닌 값이 나오는 주소부터 시작
        블록을 나누는 기준은 데이터의 중간에 0xFF가 256 개 이상일 경우 0xFF 이전까지를 하나의 블록으로 나누고 이후 0xFF가 아닌 값이 나오는 구간부터 다음 블록으로 지정
        생성한 블록의 주소정보를 FlashBlock로 생성하여 flashBlock(List<FlashBlock>) 전역 변수에 추가
        생성한 블록의 데이터를 byte[]로 flashData(List<Byte>) 전역 변수에 추가
    
     3. 헤더 생성
        길이 512의 byte[] 생성한 후 헤더정보를 byte로 변환하여 System.arraycopy
        MakeBlock()에서 flashBlock에 추가한 FlashBlock객체를 byte[]로 마샬링하여 생성한 byte[]에 추가

    4. 파일쓰기 
        헤더를 우선 파일에 쓰고 
        MakeBlock()에서 추가된 flashData를 반복하여 파일에 씀
        
    
