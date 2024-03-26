import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;

public class Form1 extends JFrame{
    // 다른 파일에서 사용하는 구조체
    private JPanel panel;
    private JButton button;
    private JTextField textField;
    private JRadioButton[] radioField;
    private ButtonGroup group = new ButtonGroup();

    // swing기반 winform 생성하여 파일 업로드 후 변경할 수 있도록
    public Form1() {
        panel = new JPanel();
        button = new JButton("파일 열기");

        textField = new JTextField(20);
        radioField = new JRadioButton[3];

        String[] option = {"ECU","DCU","DDR"};

        for (int i=0; i<3; i++) {
            radioField[i] = new JRadioButton(option[i]);
            radioField[i].setActionCommand(option[i]);
            group.add(radioField[i]);
            panel.add(radioField[i]);
        };

        button.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Open File");

                // 파일 형식 제한
                fileChooser.setFileFilter(new javax.swing.filechooser.FileFilter() {
                    public boolean accept(File file) {
                        if (file.isDirectory()) {
                            return true;
                        }
                        String fileName = file.getName();
                        return fileName.endsWith(".ulp") || fileName.endsWith(".s19") || fileName.endsWith(".ddr");
                    }
        
                    public String getDescription() {
                        return "Flash Files (*.ulp, *.s19, *.ddr)";
                    }
                });
                fileChooser.setMultiSelectionEnabled(false);
                

                int result = fileChooser.showOpenDialog(panel);
                if (result == JFileChooser.APPROVE_OPTION) {
                    File file = fileChooser.getSelectedFile();
                    textField.setText(file.getAbsolutePath());

                    UlpToBin ulpToBin = new UlpToBin(file.getAbsolutePath());
                    String type = group.getSelection().getActionCommand();
                    // 파일형식에 따라 다른 변환기 호출
                    switch (type) {
                        case "ECU":
                            // ECU파일 변환
                            ulpToBin.MakeDelphiBin();
                            break;
                        case "DCU":
                            // DCU파일 변환
                            ulpToBin.MakeScrBin();
                            break;
                        case "DDR":
                            // DDR파일 변환
                            ulpToBin.MakeDdrBin();
                            break;
                        default:
                            JOptionPane.showMessageDialog(panel, "시스템을 선택하세요.");
                            break;
                    }
                }
            }
        });

        panel.add(button);
        panel.add(textField);
        radioField[0].setSelected(true);

        add(panel);

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }


}
