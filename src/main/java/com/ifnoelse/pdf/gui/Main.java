package com.ifnoelse.pdf.gui;

import com.ifnoelse.pdf.PDFContents;
import com.ifnoelse.pdf.PDFUtil;
import com.itextpdf.text.exceptions.BadPasswordException;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Created by ifnoelse on 2017/3/2 0002.
 */
public class Main {

    private static final String ERROR_TITLE = "错误";
    private static final String INFO_TITLE = "通知";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(Main::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("PDF书签工具");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLocationRelativeTo(null); // 居中显示

        // 创建主面板
        JPanel mainPanel = createMainPanel(frame);

        frame.getContentPane().add(mainPanel);
        frame.setVisible(true);
    }

    private static JPanel createMainPanel(JFrame frame) {
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // 顶部面板 - 文件选择区域
        TopPanelComponents topPanelComponents = createTopPanel(frame);
        mainPanel.add(topPanelComponents.topPanel, BorderLayout.NORTH);

        // 中部面板 - 目录内容输入区域
        JTextArea textArea = new JTextArea();
        textArea.setPlaceholder("请在此填入目录内容");
        JScrollPane scrollPane = new JScrollPane(textArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // 底部面板 - 操作按钮区域
        JPanel bottomPanel = createBottomPanel(textArea, topPanelComponents.filePathField, topPanelComponents.pageIndexOffset, frame);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // 设置拖拽功能
        setupDragAndDrop(textArea, topPanelComponents.filePathField);

        // 设置文本监听器
        setupTextListener(textArea, bottomPanel);

        return mainPanel;
    }

    private static TopPanelComponents createTopPanel(JFrame frame) {
        JPanel topPanel = new JPanel(new BorderLayout());

        // 文件路径输入框
        JTextField filePathField = new JTextField();
        filePathField.setEditable(false);
        filePathField.setPlaceholder("请选择PDF文件");
        topPanel.add(filePathField, BorderLayout.CENTER);

        // 页码偏移量和文件选择按钮
        JTextField pageIndexOffset = new JTextField();
        pageIndexOffset.setPlaceholder("页码偏移量");
        pageIndexOffset.setPreferredSize(new Dimension(100, 25));
        JButton fileSelectorBtn = new JButton("选择文件");

        JPanel rightTopPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        rightTopPanel.add(pageIndexOffset);
        rightTopPanel.add(fileSelectorBtn);
        topPanel.add(rightTopPanel, BorderLayout.EAST);

        // 文件选择按钮事件
        fileSelectorBtn.addActionListener(e -> selectFile(frame, filePathField));

        // 页码偏移量焦点监听器
        pageIndexOffset.addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                validateOffset(pageIndexOffset.getText());
            }
        });

        return new TopPanelComponents(topPanel, filePathField, pageIndexOffset);
    }

    private static JPanel createBottomPanel(JTextArea textArea, JTextField filePathField,
                                            JTextField pageIndexOffset, JFrame frame) {
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));

        JButton getContentsBtn = new JButton("获取目录");
        JButton contentsGeneratorBtn = new JButton("生成目录");
        getContentsBtn.setEnabled(false);

        bottomPanel.add(getContentsBtn);
        bottomPanel.add(contentsGeneratorBtn);

        // 获取目录按钮事件
        getContentsBtn.addActionListener(e -> {
            String contents = PDFContents.getContentsByUrl(textArea.getText());
            textArea.setText(contents);
        });

        // 生成目录按钮事件
        contentsGeneratorBtn.addActionListener(e -> generateBookmark(
                textArea, filePathField, pageIndexOffset, frame));

        return bottomPanel;
    }

    private static void setupDragAndDrop(JTextArea textArea, JTextField filePathField) {
        textArea.setDropTarget(new DropTarget() {
            @Override
            public synchronized void drop(DropTargetDropEvent dtde) {
                try {
                    dtde.acceptDrop(DnDConstants.ACTION_COPY);
                    @SuppressWarnings("unchecked")
                    List<File> files = (List<File>) dtde.getTransferable()
                            .getTransferData(DataFlavor.javaFileListFlavor);
                    if (!files.isEmpty()) {
                        File file = files.get(0);
                        String fileName = file.getName();
                        if (fileName.toLowerCase().endsWith(".pdf")) {
                            filePathField.setText(file.getPath());
                        } else {
                            showDialog(ERROR_TITLE, "文件类型错误", "仅支持PDF格式文件", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    showDialog(ERROR_TITLE, "拖放失败", "无法解析拖入的内容", JOptionPane.ERROR_MESSAGE);
                }
            }
        });
    }

    private static void setupTextListener(JTextArea textArea, JPanel bottomPanel) {
        textArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateGetContentsButton(textArea, bottomPanel);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateGetContentsButton(textArea, bottomPanel);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateGetContentsButton(textArea, bottomPanel);
            }
        });
    }

    private static void selectFile(JFrame frame, JTextField filePathField) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF文件", "pdf"));
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            filePathField.setText(file.getPath());
        }
    }

    private static void validateOffset(String offsetStr) {
        if (offsetStr != null && !offsetStr.isEmpty()) {
            for (char c : offsetStr.toCharArray()) {
                if (!Character.isDigit(c)) {
                    showDialog(ERROR_TITLE, "偏移量设置错误", "页码偏移量只能为整数", JOptionPane.ERROR_MESSAGE);
                    break;
                }
            }
        }
    }

    private static void updateGetContentsButton(JTextArea textArea, JPanel bottomPanel) {
        JButton getContentsBtn = (JButton) bottomPanel.getComponent(0);
        getContentsBtn.setEnabled(textArea.getText().trim().startsWith("http"));
    }

    private static void generateBookmark(JTextArea textArea, JTextField filePathField,
                                         JTextField pageIndexOffset, JFrame frame) {
        String fp = filePathField.getText();
        if (fp == null || fp.isEmpty()) {
            showDialog(ERROR_TITLE, "PDF文件路径为空", "PDF文件路径不能为空，请选择PDF文件", JOptionPane.ERROR_MESSAGE);
            return;
        }

        Path srcPath = Paths.get(fp.replace("\\", "/"));
        String srcFileName = srcPath.getFileName().toString();
        String baseName = srcFileName.substring(0, srcFileName.lastIndexOf('.'));
        String extension = srcFileName.substring(srcFileName.lastIndexOf('.'));
        Path destPath = srcPath.getParent().resolve(baseName + "_含目录" + extension);

        String offsetStr = pageIndexOffset.getText();
        int offset = 0;
        if (offsetStr != null && !offsetStr.isEmpty()) {
            try {
                offset = Integer.parseInt(offsetStr);
            } catch (NumberFormatException ex) {
                showDialog(ERROR_TITLE, "偏移量无效", "请输入有效的整数作为页码偏移量", JOptionPane.ERROR_MESSAGE);
                return;
            }
        }

        String content = textArea.getText();
        if (content == null || content.isEmpty()) {
            showDialog(ERROR_TITLE, "目录内容为空", "目录内容不能为空，请填写PDF书籍目录URL或者填入目录文本", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            PDFUtil.addBookmark(content, srcPath.toString(), destPath.toString(), offset);
            showDialog(INFO_TITLE, "添加目录成功！", "文件存储在" + destPath.toAbsolutePath(), JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            String errInfo = ex.getMessage();
            if (ex.getCause() instanceof BadPasswordException || ex instanceof BadPasswordException) {
                errInfo = "PDF已加密，无法完成修改";
            }
            showDialog(ERROR_TITLE, "添加目录错误", errInfo, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static void showDialog(String title, String header, String content, int messageType) {
        JOptionPane.showMessageDialog(null, content, title, messageType);
    }

    // 辅助类用于返回顶部面板的组件引用
    private static class TopPanelComponents {
        final JPanel topPanel;
        final JTextField filePathField;
        final JTextField pageIndexOffset;

        TopPanelComponents(JPanel topPanel, JTextField filePathField, JTextField pageIndexOffset) {
            this.topPanel = topPanel;
            this.filePathField = filePathField;
            this.pageIndexOffset = pageIndexOffset;
        }
    }
}

class JTextField extends javax.swing.JTextField {
    private String placeholder;

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        this.setToolTipText(placeholder);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && placeholder != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(Color.GRAY);
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                g2.drawString(placeholder, getInsets().left, g.getFontMetrics().getAscent() + getInsets().top);
            } finally {
                g2.dispose();
            }
        }
    }
}

class JTextArea extends javax.swing.JTextArea {
    private String placeholder;

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (getText().isEmpty() && placeholder != null) {
            Graphics2D g2 = (Graphics2D) g.create();
            try {
                g2.setColor(Color.GRAY);
                g2.setFont(getFont().deriveFont(Font.ITALIC));
                g2.drawString(placeholder, getInsets().left, g.getFontMetrics().getAscent() + getInsets().top);
            } finally {
                g2.dispose();
            }
        }
    }
}
