/*
 * Copyright (c) 2023 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;

import com.github.araxeus.dnnsuperres.DNNSuperResolutionOp;
import vavi.awt.image.resample.AwtResampleOp;
import vavi.imageio.ImageConverter;
import vavi.util.Debug;


/**
 * IconMaker.
 *
 * <pre><code>
 * $ iconutil -c icns hoge.iconset
 * </code></pre>
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2023-05-08 nsano initial version <br>
 */
public class IconMaker {

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        String imageName = "/Users/nsano/src/vavi/vavi-image-sandbox/tmp/shii.png";

        Path out = Paths.get("tmp/icns").resolve(imageName.substring(imageName.lastIndexOf('/') + 1, imageName.lastIndexOf('.')) + ".iconset");
        if (!Files.exists(out)) Files.createDirectories(out);
        BufferedImage image = ImageIO.read(Paths.get(imageName).toFile());

        String[] iconNames = {
                "icon_512x512@2x.png",
                "icon_512x512.png",
                "icon_256x256@2x.png",
                "icon_256x256.png",
                "icon_128x128@2x.png",
                "icon_128x128.png",
                "icon_32x32@2x.png",
                "icon_32x32.png",
                "icon_16x16@2x.png",
                "icon_16x16.png"
        };

        if (image.getType() != BufferedImage.TYPE_INT_ARGB) {
            ImageConverter imageConverter = ImageConverter.getInstance();
            imageConverter.setColorModelType(BufferedImage.TYPE_INT_ARGB);
            image = imageConverter.toBufferedImage(image);
        }
Debug.println("image: " + image.getWidth());

        Pattern pattern = Pattern.compile("_(\\d+)");
        for (String iconName : iconNames) {
            Matcher matcher = pattern.matcher(iconName);
            if (matcher.find()) {
                int size = Integer.parseInt(matcher.group(1));
                if (iconName.contains("@2x")) size *= 2;

                double s = (double) size / image.getWidth();

                BufferedImage filteredImage;
                if (image.getWidth() == size) {
Debug.println("same: " + image.getWidth() + " -> " + size);
                    filteredImage = image;
                } else if (image.getWidth() < size) {
Debug.println("enlarge: " + image.getWidth() + " -> " + size + " (" + (int) s + ")");
                    // TODO DNNSuperResolutionOp cannot deal transparency
                    filteredImage = new DNNSuperResolutionOp(DNNSuperResolutionOp.Mode.of(DNNSuperResolutionOp.Algorithm.ED, (int) s)).filter(image, null);
                } else {
Debug.println("reduce: " + image.getWidth() + " -> " + size);
                    filteredImage = new AwtResampleOp(s, s).filter(image, null);
                }

Debug.println("output: " + filteredImage.getWidth() + "x" + filteredImage.getHeight());
                ImageIO.write(filteredImage, "png", out.resolve(iconName).toFile());
            }
        }
Debug.println("done");
    }
}
