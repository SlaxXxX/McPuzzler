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
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Main extends JFrame {

    private final int FSIZE = 128;
    private final int FRAMEXOFFSET = 100;

    private ImageHandler imgHandler = new ImageHandler();
    private BufferedImage original;
    private BufferedImage drop;
    private BufferedImage preview;
    private BufferedImage shuffled;

    JPanel dropPanel = new JPanel();
    JSlider borderWidth = new JSlider(0, 10, 2);
    JSlider imgScale = new JSlider(100, 1500, 500);
    ColorChooserButton borderColor = new ColorChooserButton(Color.ORANGE);
    JCheckBox scaleSnap = new JCheckBox("Snap to Grid");

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        super("MC Puzzler");

        this.setLayout(new FlowLayout(FlowLayout.LEFT));

        try {
            drop = ImageIO.read(getClass().getResource("drop-files-here-extra.jpg"));
            displayImage(drop);
            dropPanel.setDropTarget(new DropTarget() {
                public synchronized void drop(DropTargetDropEvent evt) {
                    try {
                        evt.acceptDrop(DnDConstants.ACTION_COPY);
                        java.util.List<File> droppedFiles = (java.util.List<File>)
                                evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
                        original = ImageIO.read(droppedFiles.get(0));
                        previewImage();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            this.add(initButtonPanel());
            this.add(initBorderPanel());
            this.add(initScalePanel());
            this.add(dropPanel);

            setLocationRelativeTo(null);
            this.setResizable(false);
            this.setMinimumSize(new Dimension(950, 400));
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void previewImage() {
        if (original != null) {
            double scaleWidth = original.getWidth() * getScale();
            double scaleHeight = original.getHeight() * getScale();
            int gwidth = Math.max((int) (scaleWidth / FSIZE), 1);
            int gheight = Math.max((int) (scaleHeight / FSIZE), 1);
            int width = gwidth * FSIZE;
            int height = gheight * FSIZE;
            double scale = getScale();
            if (scaleSnap.isSelected() && scaleWidth >= width && scaleHeight >= height) {
                if (scaleWidth - width <= scaleHeight - height)
                    scale = (double) (width) / original.getWidth();
                else
                    scale = (double) (height) / original.getHeight();
            }

            preview = imgHandler.cloneScaledSubImage(original, 0, 0, width, height, scale);
            imgHandler.addBorder(preview, borderWidth.getValue(), borderColor.getSelectedColor());

            this.setTitle("MC Puzzler " + String.format("( %s | %s )", gwidth, gheight));
            displayImage(preview);
        }
    }

    private void shuffle() {
        if (preview != null) {
            shuffled = imgHandler.shuffle(preview, FSIZE);
            displayImage(shuffled);
        }
    }

    private float getScale() {
        return (float) imgScale.getValue() / 1000;
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

    private JPanel initButtonPanel() {
        JPanel buttons = new JPanel();
        buttons.setLayout(new GridLayout(0, 3));

        JButton randomize = new JButton("Shuffle!");
        randomize.addActionListener(a -> shuffle());
        buttons.add(randomize);

        JButton save = new JButton("Save");
        save.addActionListener(a -> save());
        buttons.add(save);

        JButton upload = new JButton("Upload");

        upload.addActionListener(a -> {
            try {
                Uploader.imgurUpload(this, shuffled);
            } catch (Exception e) {
                showErrorDialog(e);
            }
        });
        buttons.add(upload);

        return buttons;
    }

    private JPanel initBorderPanel() {
        JPanel border = new JPanel();
        border.add(new JLabel("Frame:"));

        JLabel widthLabel = new JLabel("2");

        borderWidth.setPreferredSize(new Dimension(100, 20));
        borderWidth.addChangeListener(a -> {
            widthLabel.setText("" + borderWidth.getValue());
            previewImage();
        });
        border.add(borderWidth);
        border.add(widthLabel);

        borderColor.addColorChangedListener(c -> previewImage());
        border.add(borderColor);

        return border;
    }

    private JPanel initScalePanel() {
        JPanel scale = new JPanel();
        scale.add(new JLabel("Size:"));

        imgScale.setPreferredSize(new Dimension(300, 20));
        imgScale.addChangeListener(a -> {
            previewImage();
        });
        scale.add(imgScale);
        scaleSnap.setSelected(true);
        scale.add(scaleSnap);

        return scale;
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
}
