import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

public class ImageHandler {

    public BufferedImage shuffle(BufferedImage template, int frameSize) {
        int gwidth = template.getWidth() / frameSize;
        int gheight = template.getHeight() / frameSize;
        int width = gwidth * frameSize;
        int height = gheight * frameSize;

        BufferedImage shuffled = new BufferedImage(width, height, template.getType());

        Set<Integer> rNumbers = getRandomNumbers(gwidth * gheight);
        int i = 0;
        for (Integer tile : rNumbers) {
            fillTile(template, shuffled, i++, tile, frameSize);
        }
        return shuffled;
    }

    public void addBorder(BufferedImage img, int thickness, Color color) {
        if (thickness == 0)
            return;

        Graphics2D g = (Graphics2D) img.getGraphics();
        g.setColor(color);
        g.setStroke(new BasicStroke(thickness * 2));
        g.drawRect(0, 0, img.getWidth(), img.getHeight());
        g.dispose();
    }

    public BufferedImage cloneScaledSubImage(BufferedImage source, int x, int y, int width, int height, double scale) {
        BufferedImage img = new BufferedImage(width, height, source.getType());
        final AffineTransform at = AffineTransform.getScaleInstance(scale, scale);
        final AffineTransformOp ato = new AffineTransformOp(at, AffineTransformOp.TYPE_BICUBIC);
        return ato.filter(source, img);
    }

    private void fillTile(BufferedImage reference, BufferedImage imageToFill, int index, int tile, int tileSize) {
        int gWidth = reference.getWidth() / tileSize;
        int y = tile / gWidth;
        int x = tile - y * gWidth;
        BufferedImage subimg = reference.getSubimage(x * tileSize, y * tileSize, tileSize, tileSize);
        int rotateRand = new Random().nextInt(3);
        for (; rotateRand > 0; rotateRand--) {
            subimg = rotate(subimg);
        }
        imageToFill.setRGB((index - (index / gWidth) * gWidth) * tileSize, (index / gWidth) * tileSize, tileSize, tileSize,
                subimg.getRGB(0, 0, tileSize, tileSize, null, 0, tileSize), 0, tileSize);
    }

    private BufferedImage rotate(BufferedImage img) {
        int width = img.getHeight();
        int height = img.getWidth();
        BufferedImage newImage = new BufferedImage(width, height, img.getType());

        for (int i = 0; i < width; i++)
            for (int j = 0; j < height; j++)
                newImage.setRGB(height - 1 - j, i, img.getRGB(i, j));

        return newImage;
    }

    private Set<Integer> getRandomNumbers(int count) {
        Random randNum = new Random();
        Set<Integer> set = new LinkedHashSet<>();
        while (set.size() < count) {
            set.add(randNum.nextInt(count));
        }
        return set;
    }
}
