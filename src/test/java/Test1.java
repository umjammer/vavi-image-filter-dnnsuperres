/*
 * Copyright (c) 2022 by Naohide Sano, All rights reserved.
 *
 * Programmed by Naohide Sano
 */

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import com.github.araxeus.dnnsuperres.DNNSuperResolutionOp;
import org.junit.jupiter.api.Test;
import vavi.util.Debug;


/**
 * Test1.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022-10-14 nsano initial version <br>
 */
public class Test1 {

    public static void main(String[] args) throws Exception {
        Test1 app = new Test1();
        app.test();
    }

    @Test
    void test() throws Exception {

        Path in = Paths.get("src/test/resources/samples/input.png");
        String fn = in.getFileName().toString();
        String base = fn.substring(0, fn.lastIndexOf('.'));
        String ext = fn.substring(fn.lastIndexOf('.'));

        for (DNNSuperResolutionOp.Mode mode : DNNSuperResolutionOp.MODES) {
            DNNSuperResolutionOp filter = new DNNSuperResolutionOp(mode);

            BufferedImage image = ImageIO.read(Files.newInputStream(in));

long t = System.currentTimeMillis();
            BufferedImage filteredImage = filter.filter(image, null);
Debug.println(mode + ", " + (System.currentTimeMillis() - t) + " ms");

            Path out = Paths.get("tmp", base + "(" + mode + ")" + ext);
            if (!Files.exists(out.getParent())) {
                Files.createDirectories(out.getParent());
            }
            ImageIO.write(filteredImage, "PNG", Files.newOutputStream(out));
        }
    }
}
