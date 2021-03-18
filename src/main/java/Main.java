import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Main extends JFrame implements MouseWheelListener {

    private final int FSIZE = 128;
    private final int FRAMEXOFFSET = 100;
    private final float SCROLLSTEP = 1.05f;
    private boolean cropMode = false;
    private int offsetx, offsety;
    private double scale = 0.5;

    private final ImageHandler imgHandler = new ImageHandler();
    private BufferedImage original;
    private BufferedImage preview;
    private BufferedImage shuffled;

    JPanel dropPanel = new JPanel();
    JPanel buttonPanel = new JPanel();
    JPanel borderPanel = new JPanel();
    JPanel cropPanel = new JPanel();
    JSlider borderWidth = new JSlider(0, 10, 2);
    ColorChooserButton borderColor = new ColorChooserButton(Color.ORANGE);
    JButton cropButton = new JButton("Edit cropping");
    JTextField xGridField = new JTextField("6");
    JTextField yGridField = new JTextField("5");

    int draggedAtX, draggedAtY;

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        super("MC Puzzler");

        this.setLayout(new FlowLayout(FlowLayout.LEFT));

        try {
            BufferedImage drop = ImageIO.read(getClass().getResource("drop-files-here-extra.jpg"));
            displayImage(drop);
            dropPanel.setDropTarget(new DropTarget() {
                public synchronized void drop(DropTargetDropEvent evt) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY);
                        java.util.List<File> droppedFiles = (java.util.List<File>)
                                evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        original = ImageIO.read(droppedFiles.get(0));
                        cropButton.setEnabled(true);
                        previewImage();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            this.add(initCropPanel());
            this.add(initButtonPanel());
            this.add(initBorderPanel());
            this.add(dropPanel);

            setLocationRelativeTo(null);
            this.setResizable(false);
            this.setMinimumSize(new Dimension(700, 300));
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            this.addMouseWheelListener(this);
            addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    draggedAtX = e.getX();
                    draggedAtY = e.getY();
                }
            });
            addMouseMotionListener(new MouseMotionAdapter() {
                public void mouseDragged(MouseEvent e) {
                    if (cropMode) {
                        offsetx -= draggedAtX - e.getX();
                        offsety -= draggedAtY - e.getY();
                        draggedAtX = e.getX();
                        draggedAtY = e.getY();
                        previewImage();
                    }
                }
            });

            this.setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void previewImage() {
        if (original != null) {
            try {
                int width = Integer.parseInt(xGridField.getText()) * FSIZE;
                int height = Integer.parseInt(yGridField.getText()) * FSIZE;
                preview = imgHandler.cloneScaledSubImage(original, offsetx, offsety, width, height, scale);
                if (!cropMode)
                    imgHandler.addBorder(preview, borderWidth.getValue(), borderColor.getSelectedColor());
                displayImage(preview);
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private void shuffle() {
        if (preview != null) {
            shuffled = imgHandler.shuffle(preview, FSIZE);
            displayImage(shuffled);
        }
    }

    private void displayImage(BufferedImage img) {
        dropPanel.removeAll();
        JLabel lbl = new JLabel();
        lbl.setIcon(new ImageIcon(img));
        dropPanel.add(lbl);

        this.setSize(img.getWidth() + 40, img.getHeight() + FRAMEXOFFSET);

        this.revalidate();
        this.repaint();
    }

    private void save() {
        if (shuffled == null) {
            JOptionPane.showMessageDialog(this, "Image is empty. Did you shuffle at least once?");
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Where to save?");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("PNG", "png");
        fileChooser.setFileFilter(filter);


        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getAbsolutePath().endsWith(".png"))
                fileToSave = new File(fileToSave + ".png");
            try {
                ImageIO.write(shuffled, "png", fileToSave);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void toggleCrop() {
        if (original != null) {
            cropMode = !cropMode;
            xGridField.setEnabled(cropMode);
            yGridField.setEnabled(cropMode);
            setChildrenEnabled(buttonPanel, !cropMode);
            setChildrenEnabled(borderPanel, !cropMode);
            cropButton.setText(cropMode ? "Crop!" : "Edit cropping");
        }
        previewImage();
    }

    private JPanel initButtonPanel() {
        buttonPanel.setLayout(new GridLayout(0, 3));

        JButton randomize = new JButton("Shuffle!");
        randomize.addActionListener(a -> shuffle());
        buttonPanel.add(randomize);

        JButton save = new JButton("Save");
        save.addActionListener(a -> save());
        buttonPanel.add(save);

        JButton upload = new JButton("Upload");

        upload.addActionListener(a -> {
            try {
                Uploader.imgurUpload(this, shuffled);
            } catch (Exception e) {
                showErrorDialog(e);
            }
        });
        buttonPanel.add(upload);

        return buttonPanel;
    }

    private JPanel initBorderPanel() {
        borderPanel.add(new JLabel("Frame:"));

        JLabel widthLabel = new JLabel("2");

        borderWidth.setPreferredSize(new Dimension(100, 20));
        borderWidth.addChangeListener(a -> {
            widthLabel.setText("" + borderWidth.getValue());
            previewImage();
        });
        borderPanel.add(borderWidth);
        borderPanel.add(widthLabel);

        borderColor.addColorChangedListener(c -> previewImage());
        borderPanel.add(borderColor);

        return borderPanel;
    }

    private JPanel initCropPanel() {
        cropButton.addActionListener(a -> toggleCrop());
        cropPanel.add(cropButton);

        xGridField.setPreferredSize(new Dimension(25, 20));
        yGridField.setPreferredSize(new Dimension(25, 20));
        xGridField.addActionListener(e -> previewImage());
        yGridField.addActionListener(e -> previewImage());
        cropPanel.add(xGridField);
        cropPanel.add(new JLabel("x"));
        cropPanel.add(yGridField);

        setChildrenEnabled(cropPanel, false);
        return cropPanel;
    }

    private void showErrorDialog(Exception e) {
        int n = JOptionPane.showOptionDialog(this,
                e.getMessage(),
                "Whoops",
                JOptionPane.DEFAULT_OPTION,
                JOptionPane.ERROR_MESSAGE,
                null,     //do not use a custom Icon
                new Object[]{"Copy full Exception to Clipboard"},
                "Copy full Exception to Clipboard"); //default button title
        if (n != JOptionPane.CLOSED_OPTION) {
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new StringSelection(e.getClass() + ": " + e.getMessage() + "\n" +
                    Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString)
                            .collect(Collectors.joining("\n"))), null);
        }
    }

    void setChildrenEnabled(Component component, boolean enabled) {
        for (Component child : ((Container) component).getComponents()) {
            child.setEnabled(enabled);
        }
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        if (cropMode) {
            if (e.getWheelRotation() > 0)
                scale = Math.max(0.1, scale / SCROLLSTEP);
            else
                scale = Math.min(2, scale * SCROLLSTEP);
            previewImage();
        }
    }
}
