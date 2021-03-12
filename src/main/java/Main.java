import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

import static java.awt.image.BufferedImage.TYPE_INT_RGB;

public class Main extends JFrame {

    private final int FSIZE = 32;
    private final int FRAMEXOFFSET = 100;
    private BufferedImage original;
    private BufferedImage drop;
    private BufferedImage randomized;
    JPanel dropPanel = new JPanel();

    public static void main(String[] args) {
        new Main();
    }

    public Main() {
        super("MC Puzzler");

        this.setLayout(new FlowLayout());

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
                        shuffle();
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
            this.add(dropPanel);


            JPanel buttons = new JPanel();
            buttons.setLayout(new GridLayout(0, 3));

            JButton randomize = new JButton("Randomize!");
            randomize.addActionListener(a -> shuffle());
            buttons.add(randomize);

            JButton clear = new JButton("New");
            clear.addActionListener(a -> displayImage(drop));
            buttons.add(clear);

            JButton save = new JButton("Save");
            save.addActionListener(a -> {
                JFileChooser fileChooser = new JFileChooser();
                fileChooser.setDialogTitle("Where to save?");
                FileNameExtensionFilter filter = new FileNameExtensionFilter("Bitmap", "bmp");
                fileChooser.setFileFilter(filter);


                int userSelection = fileChooser.showSaveDialog(this);

                if (userSelection == JFileChooser.APPROVE_OPTION) {
                    File fileToSave = fileChooser.getSelectedFile();
                    if (!fileToSave.getAbsolutePath().endsWith(".bmp"))
                        fileToSave = new File(fileToSave + ".bmp");
                    try {
                        ImageIO.write(randomized, "BMP", fileToSave);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
            buttons.add(save);
            this.add(buttons);

            setLocationRelativeTo(null);
            this.setResizable(false);
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            this.setVisible(true);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void displayImage(BufferedImage img) {
        dropPanel.removeAll();
        JLabel lbl = new JLabel();
        lbl.setIcon(new ImageIcon(img));
        dropPanel.add(lbl);

        this.setSize(img.getWidth() + 20, img.getHeight() + FRAMEXOFFSET);

        this.revalidate();
        this.repaint();
    }

    private void shuffle() {
        if (original != null) {
            int gwidth = original.getWidth() / FSIZE;
            int gheight = original.getHeight() / FSIZE;
            int width = gwidth * FSIZE;
            int height = gheight * FSIZE;

            randomized = new BufferedImage(width, height, TYPE_INT_RGB);
            Set<Integer> rNumbers = getRandomNumbers(gwidth * gheight);

            int i = 0;
            for (Integer tile : rNumbers) {
                fillTile(randomized, i++, tile, gwidth);
            }
            displayImage(randomized);
        }
    }

    private void fillTile(BufferedImage img, int index, int tile, int gwidth) {
        int y = tile / gwidth;
        int x = tile - y * gwidth;
        BufferedImage subimg = original.getSubimage(x * FSIZE, y * FSIZE, FSIZE, FSIZE);
        int rotateRand = new Random().nextInt(3);
        for (; rotateRand > 0; rotateRand--) {
            subimg = rotate(subimg);
        }
        img.setRGB((index - (index / gwidth) * gwidth) * FSIZE, (index / gwidth) * FSIZE, FSIZE, FSIZE, subimg.getRGB(0, 0, FSIZE, FSIZE, null, 0, FSIZE), 0, FSIZE);
    }

    private BufferedImage rotate(BufferedImage img) {
        int width = img.getWidth();
        int height = img.getHeight();
        BufferedImage newImage = new BufferedImage(height, width, img.getType());

        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                newImage.setRGB(height - 1 - j, i, img.getRGB(i, j));

        return newImage;
    }


    private Set<Integer> getRandomNumbers(int count) {
        Random randNum = new Random();
        Set<Integer> set = new LinkedHashSet<Integer>();
        while (set.size() < count) {
            set.add(randNum.nextInt(count));
        }
        return set;
    }
}
