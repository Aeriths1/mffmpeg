import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.dnd.*;
import java.util.List;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.DataFlavor;
import java.util.concurrent.TimeoutException;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

// 明确导入 JOptionPane
import javax.swing.JOptionPane;

public class VideoCompressorGUI extends JFrame {
    private JTextField inputField;
    private JTextField outputField;
    private JLabel originalSizeLabel;
    private JLabel compressedSizeLabel;
    private JLabel percentageLabel;
    private JButton compressButton;
    private JProgressBar progressBar;
    private JButton openFolderButton;

    private static final Color BACKGROUND_COLOR = new Color(240, 240, 240);
    private static final Color BUTTON_COLOR = new Color(70, 130, 180);
    private static final Color BUTTON_HOVER_COLOR = new Color(100, 160, 210);
    private static final Color TEXT_COLOR = new Color(50, 50, 50);
    private static final Color COMPRESS_BUTTON_COLOR = new Color(76, 175, 80); // 绿色
    private static final Color COMPRESS_BUTTON_HOVER_COLOR = new Color(106, 205, 110); // 浅绿色

    private static final String ALLOWED_VIDEO_EXTENSIONS = "mp4|avi|mov|mkv|flv|wmv|webm";
    private static final Pattern SAFE_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9_\\-\\./\\\\]+$");
    private static final String BASE_ALLOWED_DIR = System.getProperty("user.home");

    // 在类的顶部声明一个成员变量
    private final JFrame frame;

    public VideoCompressorGUI() {
        frame = this;
        setTitle("视频压缩工具");
        setSize(800, 400); // 增加默认尺寸
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        getContentPane().setBackground(BACKGROUND_COLOR);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBackground(BACKGROUND_COLOR);
        mainPanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        // 文件选择面板
        JPanel filePanel = new JPanel(new GridBagLayout());
        filePanel.setBackground(BACKGROUND_COLOR);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 5, 5, 5);

        addLabel("选择视频文件:", filePanel, gbc, 0, 0);
        inputField = new JTextField(20);
        styleTextField(inputField);
        gbc.gridx = 1;
        gbc.gridy = 0;
        gbc.weightx = 1;
        filePanel.add(inputField, gbc);

        JButton browseButton = new JButton("浏览");
        styleButton(browseButton);
        browseButton.addActionListener(e -> browseVideoFile(inputField));
        gbc.gridx = 2;
        gbc.gridy = 0;
        gbc.weightx = 0;
        filePanel.add(browseButton, gbc);

        addLabel("输出文件夹:", filePanel, gbc, 0, 1);
        outputField = new JTextField(20);
        styleTextField(outputField);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.weightx = 1;
        filePanel.add(outputField, gbc);

        JButton outputBrowseButton = new JButton("浏览");
        styleButton(outputBrowseButton);
        outputBrowseButton.addActionListener(e -> browseOutputFolder());
        gbc.gridx = 2;
        gbc.gridy = 1;
        gbc.weightx = 0;
        filePanel.add(outputBrowseButton, gbc);

        mainPanel.add(filePanel, BorderLayout.NORTH);

        // 大小显示面板
        JPanel sizePanel = new JPanel(new GridLayout(1, 2, 20, 0));
        sizePanel.setBackground(BACKGROUND_COLOR);
        sizePanel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel originalPanel = createSizePanel("压缩前", originalSizeLabel = new JLabel("0 MB"));
        JPanel compressedPanel = createSizePanel("压缩后", compressedSizeLabel = new JLabel("0 MB"));

        sizePanel.add(originalPanel);
        sizePanel.add(compressedPanel);

        mainPanel.add(sizePanel, BorderLayout.CENTER);

        // 压缩按钮和百分比标签
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBackground(BACKGROUND_COLOR);

        JPanel compressButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        compressButtonPanel.setBackground(BACKGROUND_COLOR);

        compressButton = new JButton("压缩");
        styleButton(compressButton);
        compressButton.addActionListener(e -> compressVideo());
        compressButtonPanel.add(compressButton);

        bottomPanel.add(compressButtonPanel, BorderLayout.WEST);

        openFolderButton = new JButton("跳转到生成文件");
        styleButton(openFolderButton);
        openFolderButton.setVisible(false);
        openFolderButton.addActionListener(e -> openOutputFolder());
        bottomPanel.add(openFolderButton, BorderLayout.EAST);

        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        add(mainPanel, BorderLayout.CENTER);

        // 进度条
        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setForeground(BUTTON_COLOR);
        progressBar.setBackground(Color.WHITE);
        progressBar.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        add(progressBar, BorderLayout.SOUTH);

        // 为整个窗口添加拖放支持
        this.setDropTarget(new DropTarget(this, new DropTargetListener() {
            public void drop(DropTargetDropEvent event) {
                event.acceptDrop(DnDConstants.ACTION_COPY);
                Transferable transferable = event.getTransferable();
                DataFlavor[] flavors = transferable.getTransferDataFlavors();
                for (DataFlavor flavor : flavors) {
                    try {
                        if (flavor.isFlavorJavaFileListType()) {
                            @SuppressWarnings("unchecked")
                            List<File> files = (List<File>) transferable.getTransferData(flavor);
                            for (File file : files) {
                                String filePath = sanitizePath(removeFilePrefix(file.getAbsolutePath()));
                                if (isValidVideoFile(filePath)) {
                                    inputField.setText(filePath);
                                    updateOriginalSize(filePath);
                                    event.dropComplete(true);
                                    return;
                                }
                            }
                            // 如果没有找到有效的视频文件
                            JOptionPane.showMessageDialog(frame, "请拖放有效的视频文件。支持的格式：mp4, avi, mov, mkv, flv, wmv, webm", "无效的文件类型", JOptionPane.WARNING_MESSAGE);
                        }
                    } catch (Exception e) {
                        System.err.println("处理拖放文件时发生错误: " + e.getMessage());
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(frame, "处理文件时发生错误: " + e.getMessage(), "错误", JOptionPane.ERROR_MESSAGE);
                    }
                }
                event.dropComplete(false);
                System.out.println("没有找到有效的视频文件");
            }

            public void dragEnter(DropTargetDragEvent event) {}
            public void dragOver(DropTargetDragEvent event) {}
            public void dropActionChanged(DropTargetDragEvent event) {}
            public void dragExit(DropTargetEvent event) {}
        }));

        // 在构造函数中更新输入字段的监听器
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateSize(); }
            public void removeUpdate(DocumentEvent e) { updateSize(); }
            public void insertUpdate(DocumentEvent e) { updateSize(); }

            private void updateSize() {
                SwingUtilities.invokeLater(() -> updateOriginalSize(inputField.getText()));
            }
        });

        // 添加输入字段的监听器
        inputField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateInputValidity(); }
            public void removeUpdate(DocumentEvent e) { updateInputValidity(); }
            public void insertUpdate(DocumentEvent e) { updateInputValidity(); }

            private void updateInputValidity() {
                SwingUtilities.invokeLater(() -> {
                    String inputPath = sanitizePath(removeFilePrefix(inputField.getText()));
                    if (!inputPath.isEmpty() && !isValidVideoFile(inputPath)) {
                        inputField.setBackground(new Color(255, 200, 200)); // 浅红色背景表示无效输入
                    } else {
                        inputField.setBackground(Color.WHITE);
                    }
                });
            }
        });

        outputField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updateOutputValidity(); }
            public void removeUpdate(DocumentEvent e) { updateOutputValidity(); }
            public void insertUpdate(DocumentEvent e) { updateOutputValidity(); }

            private void updateOutputValidity() {
                SwingUtilities.invokeLater(() -> {
                    String outputPath = sanitizePath(removeFilePrefix(outputField.getText()));
                    if (!outputPath.isEmpty() && !isValidOutputFolder(outputPath)) {
                        outputField.setBackground(new Color(255, 200, 200)); // 浅红色背景表示无效输入
                    } else {
                        outputField.setBackground(Color.WHITE);
                    }
                });
            }
        });

        setVisible(true);
    }

    private JPanel createSizePanel(String title, JLabel sizeLabel) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        titleLabel.setForeground(new Color(120, 120, 120));
        titleLabel.setBorder(new EmptyBorder(10, 15, 5, 15));
        panel.add(titleLabel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.setBackground(Color.WHITE);
        centerPanel.setBorder(new EmptyBorder(0, 15, 10, 15));

        sizeLabel.setFont(new Font("Arial", Font.BOLD, 36)); // 增大字体大小
        sizeLabel.setForeground(new Color(50, 50, 50));
        centerPanel.add(sizeLabel, BorderLayout.NORTH);
        
        if (title.equals("压缩后")) {
            percentageLabel = new JLabel();
            percentageLabel.setHorizontalAlignment(SwingConstants.LEFT);
            percentageLabel.setFont(new Font("Arial", Font.BOLD, 18)); // 增大字体
            percentageLabel.setForeground(new Color(76, 175, 80)); // 绿色
            percentageLabel.setBorder(new EmptyBorder(5, 0, 0, 0));
            percentageLabel.setVisible(false);
            centerPanel.add(percentageLabel, BorderLayout.SOUTH);
        }

        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private void addLabel(String text, JPanel panel, GridBagConstraints gbc, int x, int y) {
        JLabel label = new JLabel(text);
        label.setForeground(TEXT_COLOR);
        label.setFont(new Font("Arial", Font.BOLD, 14));
        gbc.gridx = x;
        gbc.gridy = y;
        gbc.weightx = 0;
        panel.add(label, gbc);
    }

    private void styleTextField(JTextField textField) {
        textField.setFont(new Font("Arial", Font.PLAIN, 14));
        textField.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(200, 200, 200)),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)));
    }

    private void styleButton(JButton button) {
        Color buttonColor = button == compressButton ? COMPRESS_BUTTON_COLOR : BUTTON_COLOR;
        Color hoverColor = button == compressButton ? COMPRESS_BUTTON_HOVER_COLOR : BUTTON_HOVER_COLOR;

        button.setBackground(buttonColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);
        button.setFont(new Font("Arial", Font.BOLD, 14)); // 确保字体加粗的
        button.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
        button.setOpaque(true);
        button.setBorderPainted(false);

        button.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                button.setBackground(buttonColor);
            }
        });
    }

    private void browseVideoFile(JTextField field) {
        JFileChooser fileChooser = new JFileChooser();
        FileNameExtensionFilter filter = new FileNameExtensionFilter(
            "视频文件 (mp4, avi, mov, mkv, flv, wmv, webm)", "mp4", "avi", "mov", "mkv", "flv", "wmv", "webm");
        fileChooser.setFileFilter(filter);
        fileChooser.setCurrentDirectory(new File(BASE_ALLOWED_DIR));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = sanitizePath(selectedFile.getAbsolutePath());
            if (isValidVideoFile(filePath)) {
                field.setText(filePath);
                updateOriginalSize(filePath);
            } else {
                JOptionPane.showMessageDialog(this, "请选择有效的视频文件。支持的格式：mp4, avi, mov, mkv, flv, wmv, webm", "无效的文件类型", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void browseOutputFolder() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setCurrentDirectory(new File(BASE_ALLOWED_DIR));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String folderPath = removeFilePrefix(selectedFile.getAbsolutePath());
            if (isValidOutputFolder(folderPath)) {
                outputField.setText(folderPath);
            } else {
                JOptionPane.showMessageDialog(this, "请选择有效的输出文件夹。", "无效的输出路径", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void updateOriginalSize(String filePath) {
        filePath = sanitizePath(removeFilePrefix(filePath));
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            long sizeInBytes = file.length();
            double sizeInMB = sizeInBytes / (1024.0 * 1024.0);
            originalSizeLabel.setText(String.format("%.1f MB", sizeInMB));
            compressedSizeLabel.setText("0.0 MB");
            percentageLabel.setText("");
            percentageLabel.setVisible(false);
            openFolderButton.setVisible(false);
        } else {
            System.out.println("文件不存在或不是有效文件: " + filePath);
            originalSizeLabel.setText("0.0 MB");
        }
    }

    private void compressVideo() {
        String inputPath = sanitizePath(removeFilePrefix(inputField.getText()));
        String outputFolder = sanitizePath(removeFilePrefix(outputField.getText()));

        if (!isValidVideoFile(inputPath)) {
            JOptionPane.showMessageDialog(this, "请选择有效的视频文件。支持的格式：mp4, avi, mov, mkv, flv, wmv, webm", "无效的输入文件", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (!isValidOutputFolder(outputFolder)) {
            JOptionPane.showMessageDialog(this, "请选择有效的输出文件夹。", "无效的输出路径", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File inputFile = new File(inputPath);
        String baseName = inputFile.getName().replaceFirst("[.][^.]+$", "");
        String outputPath = getUniqueOutputPath(outputFolder, baseName);

        new Thread(() -> {
            Process process = null;
            try {
                SwingUtilities.invokeLater(() -> {
                    compressButton.setEnabled(false);
                    progressBar.setValue(0);
                });

                System.out.println("开始压缩视频...");
                List<String> command = new ArrayList<>(Arrays.asList(
                    "ffmpeg", "-i", inputPath,
                    "-c:v", "libx264", "-tag:v", "avc1",
                    "-movflags", "faststart",
                    "-crf", "30", "-preset", "superfast",
                    outputPath
                ));
                ProcessBuilder pb = new ProcessBuilder(command);
                pb.redirectErrorStream(true);
                process = pb.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                Pattern timePattern = Pattern.compile("time=([\\d:.]+)");
                Pattern durationPattern = Pattern.compile("Duration: (\\d{2}:\\d{2}:\\d{2}\\.\\d{2})");
                String duration = null;

                long startTime = System.currentTimeMillis();
                long timeout = 300000; // 5分钟超时

                while ((line = reader.readLine()) != null) {
                    System.out.println("FFmpeg输出: " + line);
                    if (System.currentTimeMillis() - startTime > timeout) {
                        throw new TimeoutException("压缩操作超时");
                    }

                    if (duration == null) {
                        Matcher durationMatcher = durationPattern.matcher(line);
                        if (durationMatcher.find()) {
                            duration = durationMatcher.group(1);
                            System.out.println("视频总时长: " + duration);
                        }
                    }

                    Matcher timeMatcher = timePattern.matcher(line);
                    if (timeMatcher.find() && duration != null) {
                        String time = timeMatcher.group(1);
                        int progress = (int) (getSeconds(time) / getSeconds(duration) * 100);
                        SwingUtilities.invokeLater(() -> {
                            progressBar.setValue(progress);
                            updateCompressedSize(new File(outputPath));
                        });
                    }
                }

                int exitCode = process.waitFor();
                if (exitCode == 0) {
                    SwingUtilities.invokeLater(() -> {
                        progressBar.setValue(100);
                        File compressedFile = new File(outputPath);
                        updateCompressedSize(compressedFile);
                        calculateAndDisplayPercentage();
                    });
                    System.out.println("视频压缩成功");
                } else {
                    throw new RuntimeException("压缩失败，退出代码: " + exitCode);
                }
            } catch (Exception ex) {
                System.err.println("压缩过程中发生错误: " + ex.getMessage());
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "压缩失败: " + ex.getMessage());
                });
            } finally {
                if (process != null) {
                    process.destroyForcibly();
                }
                SwingUtilities.invokeLater(() -> compressButton.setEnabled(true));
            }
        }).start();
    }

    private String getUniqueOutputPath(String outputFolder, String baseName) {
        String extension = "_compressed.mp4";
        File outputFile = new File(outputFolder, baseName + extension);
        int count = 1;

        while (outputFile.exists()) {
            outputFile = new File(outputFolder, baseName + "(" + count + ")" + extension);
            count++;
        }

        return outputFile.getAbsolutePath();
    }

    private void updateCompressedSize(File compressedFile) {
        long sizeInBytes = compressedFile.length();
        double sizeInMB = sizeInBytes / (1024.0 * 1024.0);
        compressedSizeLabel.setText(String.format("%.1f MB", sizeInMB));
    }

    private void calculateAndDisplayPercentage() {
        double originalSize = Double.parseDouble(originalSizeLabel.getText().replace(" MB", ""));
        double compressedSize = Double.parseDouble(compressedSizeLabel.getText().replace(" MB", ""));
        double percentage = ((originalSize - compressedSize) / originalSize) * 100;
        percentageLabel.setText(String.format("节省了 %d%% 空间", Math.round(percentage)));
        percentageLabel.setVisible(true);
        openFolderButton.setVisible(true);
    }

    private double getSeconds(String time) {
        String[] parts = time.split(":");
        double seconds = 0;
        for (String part : parts) {
            seconds = seconds * 60 + Double.parseDouble(part);
        }
        return seconds;
    }

    private void openOutputFolder() {
        try {
            String outputPath = sanitizePath(removeFilePrefix(outputField.getText()));
            Desktop.getDesktop().open(new File(outputPath));
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "无法打开输出文件夹: " + ex.getMessage());
        }
    }

    private boolean isVideoFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mp4") || name.endsWith(".avi") || name.endsWith(".mov") 
            || name.endsWith(".mkv") || name.endsWith(".flv") || name.endsWith(".wmv") 
            || name.endsWith(".webm");
    }

    private String formatFileSize(long sizeInBytes) {
        double sizeInMB = sizeInBytes / (1024.0 * 1024.0);
        return String.format("%.1f MB", sizeInMB);
    }

    private String removeFilePrefix(String path) {
        try {
            if (path.startsWith("file:")) {
                // 使用 URI 类来正确解析文件 URL
                URI uri = new URI(path);
                path = Paths.get(uri).toString();
            }
            // 处理 Windows 路径
            if (path.matches("^/[A-Za-z]:/.+")) {
                path = path.substring(1);
            }
            System.out.println("处理后的路径: " + path);
            return path;
        } catch (URISyntaxException e) {
            System.err.println("无效的文件 URI: " + path);
            return path; // 返回原始路径，而不是空字符串
        }
    }

    private String sanitizePath(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        // 移除可能导致路径遍历的模式
        path = path.replaceAll("\\.\\.+", "").replaceAll("//+", "/");
        // 使用 Path 类规范化路径
        Path normalizedPath = Paths.get(path).normalize();
        return normalizedPath.toString();
    }

    private boolean isValidVideoFile(String filePath) {
        if (filePath == null || filePath.isEmpty()) {
            return false;
        }
        
        File file = new File(filePath);
        if (!file.exists() || !file.isFile()) {
            return false;
        }
        
        String fileName = file.getName().toLowerCase();
        return fileName.matches(".*\\.(" + ALLOWED_VIDEO_EXTENSIONS + ")$");
    }

    private boolean isValidOutputFolder(String folderPath) {
        if (folderPath == null || folderPath.isEmpty()) {
            return false;
        }
        folderPath = removeFilePrefix(folderPath);
        File folder = new File(folderPath);
        return folder.exists() && folder.isDirectory() && folder.canWrite();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        SwingUtilities.invokeLater(VideoCompressorGUI::new);
    }
}