[![Release](https://jitpack.io/v/umjammer/vavi-image-filter-dnnsuperres.svg)](https://jitpack.io/#umjammer/vavi-image-filter-dnnsuperres)
[![Java CI](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-8-b07219)

# vavi-image-filter-dnnsuperres

<img src="https://raw.githubusercontent.com/wiki/opencv/opencv/logo/OpenCV_logo_black.svg?sanitize=true" />

[BufferedImageOp](https://docs.oracle.com/javase/8/docs/api/java/awt/image/BufferedImageOp.html)
using pretrained models to OpenCV's DNNSuperRes

this is a fork of https://github.com/Araxeus/PNG-Upscale

## Install

https://jitpack.io/#umjammer/vavi-image-filter-dnnsuperres

```shell
 $ mvn -P lwjgl-natives-macos-amd64 -Djavacpp.platform=macosx-x86_64 install
```

## Usage

```java
    DNNSuperResolutionOp filter = new DNNSuperResolutionOp(DNNSuperResolutionOp.MODES[0]);
    BufferedImage image = ImageIO.read(Files.newInputStream(in));
    BufferedImage filteredImage = filter.filter(image, null);
    ImageIO.write(filteredImage, "PNG", Files.newOutputStream(out));
```

## TODO

 * quality
   * models this project provided ... 
     * [sample image](src/test/resources/samples/input.png) makes good result, but others...
     * otoh [OpenML](https://github.com/umjammer/rococoa/blob/0.8.5/rococoa-contrib/src/test/java/org/rococoa/cocoa/coreml/CoreMLTest.java)
     `REAL-ESRGAN` creates significant result (color is somehow different...)
   * https://github.com/umjammer/vavi-image-filter-ml/blob/main/src/test/resources/namacha.jpg ... i couldn't see differences
   * https://qiita.com/gomamitu/items/b4722741f6318d734bce ... i think there is no differences (magnifying is needed lol)
 * `Dnn#readNetFromTensorflow()`
