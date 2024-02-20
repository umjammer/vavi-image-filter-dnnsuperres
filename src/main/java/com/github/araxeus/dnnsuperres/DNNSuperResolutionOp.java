/*
 * https://github.com/Araxeus/PNG-Upscale
 */

package com.github.araxeus.dnnsuperres;

import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_dnn_superres.DnnSuperResImpl;
import org.opencv.imgcodecs.Imgcodecs;
import vavix.util.ResourceList;

import static org.bytedeco.opencv.global.opencv_imgcodecs.imdecode;
import static org.bytedeco.opencv.global.opencv_imgcodecs.imencode;


/**
 * DNNSuperResolutionOp.
 * <p>
 * The OpenCV DnnSuperResImpl wrapper as a Java2D BufferedImageOp.
 *
 * @author <a href="mailto:umjammer@gmail.com">Naohide Sano</a> (nsano)
 * @version 0.00 2022/10/14 nsano initial version <br>
 */
@SuppressWarnings({"java:S106", "java:S2093"})
public class DNNSuperResolutionOp implements BufferedImageOp {

    /** */
    private static Logger logger = Logger.getLogger(DNNSuperResolutionOp.class.getName());

    /** */
    public void setLogger(Logger logger) {
        DNNSuperResolutionOp.logger = logger;
    }

    /** upscaler cache per model */
    private static final Map<String, DnnSuperResImpl> cache = new HashMap<>();

    /** scaling algorithm */
    public enum Algorithm {
        ES("ESPCN"),
        ED("EDSR"),
        FS("FSRCNN"),
        LA("LapSRN");
        final String name;

        Algorithm(String name) {
            this.name = name;
        }
    }

    /** algorithm and scale */
    public static class Mode {
        /** available models */
        public static final Mode[] MODES = {
                new Mode(DNNSuperResolutionOp.Algorithm.ES, 2),
                new Mode(DNNSuperResolutionOp.Algorithm.ES, 3),
                new Mode(DNNSuperResolutionOp.Algorithm.ES, 4),
                new Mode(DNNSuperResolutionOp.Algorithm.ED, 2),
                new Mode(DNNSuperResolutionOp.Algorithm.ED, 3),
                new Mode(DNNSuperResolutionOp.Algorithm.ED, 4),
                new Mode(DNNSuperResolutionOp.Algorithm.FS, 2),
                new Mode(DNNSuperResolutionOp.Algorithm.FS, 3),
                new Mode(DNNSuperResolutionOp.Algorithm.FS, 4),
                new Mode(DNNSuperResolutionOp.Algorithm.LA, 2),
                new Mode(DNNSuperResolutionOp.Algorithm.LA, 4),
                new Mode(DNNSuperResolutionOp.Algorithm.LA, 8)
        };

        public static Mode of(Algorithm algorithm, int scale) {
            return Arrays.stream(MODES).filter(m -> m.algorithm == algorithm && m.scale == scale).findFirst().orElseThrow(NoSuchElementException::new);
        }

        public DNNSuperResolutionOp.Algorithm algorithm;
        public int scale;

        public Mode(DNNSuperResolutionOp.Algorithm algorithm, int scale) {
            this.algorithm = algorithm;
            this.scale = scale;
        }

        @Override
        public String toString() {
            return algorithm.name + "x" + scale;
        }
    }

    /** opencv cannot deal files in jar */
    private static Path modelDir;

    /* file in jar -> temp dir */
    static {

        try {
            // temporary directory
            Path modelDir = Files.createTempDirectory(DNNSuperResolutionOp.class.getName());

            AtomicBoolean used = new AtomicBoolean();

//ResourceList.getResources(Pattern.compile(".*")).forEach(logger::finer);
            // resources in "jar"
            Collection<String> models = ResourceList.getResources(Pattern.compile("Models/.*\\.pb"));
logger.fine("models: " + models.size());
            // TODO resources in "jar in jar" (means in mac .app)
//            if (models.size() == 0) {
//                models = ResourceList.getResources(Pattern.compile(".*/vavi-image-filter-dnnsuperres.*\\.jar"));
//                if (models.size() >= 1) {
//                    List<String> modelsInJar = new ArrayList<>();
//                    ZipFile zip = new ZipFile(Paths.get(URI.create(models.iterator().next())).toFile());
//                } else {
//logger.severe("no files and jar");
//                }
//            }
            models.forEach(url -> {
logger.finer(url);
                if (!url.startsWith("file:")) { // means "jar://...\\.jar!foo/bar..."
                    // resources in "jar"
                    String inJarFile = url.substring(url.lastIndexOf('!') + 1);
logger.finer("injar: " + inJarFile);
                    Path out = modelDir.resolve(inJarFile);
                    if (!Files.exists(out.getParent())) {
                        try {
                            Files.createDirectories(out.getParent());
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    }
                    try (InputStream is = DNNSuperResolutionOp.class.getResourceAsStream("/" + inJarFile);
                         OutputStream os = Files.newOutputStream(out)) {
                        byte[] b = new byte[8192];
                        while (true) {
                            int r = is.read(b);
                            if (r < 0) break;
                            os.write(b, 0, r);
                        }
logger.finer("create: " + out);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                    used.set(true);
                }
            });

            if (used.get()) {
                // run on another project: resources as in jar resources
                DNNSuperResolutionOp.modelDir = modelDir;
            } else {
                // run on this project (i.e. on ide): resources as files
                DNNSuperResolutionOp.modelDir = Paths.get("src/main/resources");
            }
logger.fine("modelDir: " + DNNSuperResolutionOp.modelDir);

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
logger.info("shutdownHook");
                // remove temporary directory
                try (Stream<Path> s = Files.walk(modelDir)) {
                    s.sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }

                cache.values().forEach(sr -> {
                    sr.deallocate();
                    sr.close();
                });

            }));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** which mode */
    private final Mode mode;

    /** selected scaler */
    private DnnSuperResImpl scaler;

    /** creates a filter */
    public DNNSuperResolutionOp(Mode mode) {
        this.mode = mode;
        scaler = cache.get(mode.toString());
        if (scaler == null) {
            scaler = new DnnSuperResImpl();
logger.finer("Loading AI");
            String modelName = "Models/" + mode + ".pb";
logger.finer("Trying to read model from " + modelName);
            long t = System.currentTimeMillis();
            scaler.readModel(modelDir.resolve(modelName).toString());
            scaler.setModel(mode.algorithm.name.toLowerCase(), mode.scale);
logger.fine("preparing[" + mode + "]: " + (System.currentTimeMillis() - t) + " ms");
            cache.put(mode.toString(), scaler);
        }
    }

    @Override
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
logger.finer("Started DNNSuperResolutionOp Process [" + mode + "]");
        int width = src.getWidth();
        int height = src.getHeight();
        width *= mode.scale;
        height *= mode.scale;
        if (width > 6666 || height > 6666) {
            throw new IllegalArgumentException("ERROR: Expected output has a side that's bigger than 6666 pixels");
        }

        try (Mat imageNew = new Mat();
             Mat image = toMat(src)) {
logger.finer("Algorithm and Size Checked" + "\n \t Starting conversion");
long t = System.currentTimeMillis();
            scaler.upsample(image, imageNew);
logger.fine("upsample[" + mode + "]: " + (System.currentTimeMillis() - t) + " ms");

            if (imageNew.isNull()) {
                throw new IllegalStateException("Error UpScaling !");
            }
logger.fine("Image was successfully upScaled from " + "[" + src.getWidth() + "x" + src.getHeight() + "]" + "x" + mode + ", to " + "[" + width + "x" + height + "]" + "and saved to:");

            return toBufferedImage(imageNew);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** java -> opencv */
    private Mat toMat(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        baos.flush();
        Mat mat = imdecode(new Mat(new BytePointer(baos.toByteArray())), Imgcodecs.IMREAD_COLOR);
        if (mat.empty()) {
            throw new IllegalArgumentException("Error Loading Image");
        }
        return mat;
    }

    /** opencv -> java */
    private BufferedImage toBufferedImage(Mat mat) throws IOException {
        BytePointer bp = new BytePointer();
        imencode(".png", mat, bp);
        return ImageIO.read(new ByteArrayInputStream(bp.getStringBytes()));
    }

    @Override
    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(0, 0, src.getWidth(), src.getHeight());
    }

    @Override
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel dstCM) {
        if (dstCM == null) {
            dstCM = src.getColorModel();
        }
        return new BufferedImage(dstCM, dstCM.createCompatibleWritableRaster(src.getWidth(), src.getHeight()), dstCM.isAlphaPremultiplied(), null);
    }

    @Override
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        if (dstPt == null) {
            dstPt = new Point2D.Double();
        }
        dstPt.setLocation(srcPt.getX(), srcPt.getY());
        return dstPt;
    }

    @Override
    public RenderingHints getRenderingHints() {
        return null;
    }
}
