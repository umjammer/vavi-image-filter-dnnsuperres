[![Maven Package](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/maven-publish.yml/badge.svg)](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/maven-publish.yml)
[![Java CI](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/maven.yml/badge.svg)](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/maven.yml)
[![CodeQL](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/codeql-analysis.yml/badge.svg)](https://github.com/umjammer/vavi-image-filter-dnnsuperres/actions/workflows/codeql-analysis.yml)
![Java](https://img.shields.io/badge/Java-8-b07219)

# vavi-image-filter-dnnsuperres

<img src="https://raw.githubusercontent.com/wiki/opencv/opencv/logo/OpenCV_logo_black.svg?sanitize=true" />

[BufferedImageOp](https://docs.oracle.com/javase/8/docs/api/java/awt/image/BufferedImageOp.html)
using pretrained models to OpenCV's DNNSuperRes

this is a fork of https://github.com/Araxeus/PNG-Upscale

## Install

* https://github.com/umjammer/vavi-image-filter-dnnsuperres/packages
* this project uses github packages. add a personal access token to `~/.m2/settings.xml`
* see https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-apache-maven-registry

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
     * otoh [CoreML](https://github.com/umjammer/rococoa/blob/0.8.5/rococoa-contrib/src/test/java/org/rococoa/cocoa/coreml/CoreMLTest.java)
     `REAL-ESRGAN` creates significant result (color is somehow different...)
   * https://github.com/umjammer/vavi-image-filter-ml/blob/main/src/test/resources/namacha.jpg ... i couldn't see differences
   * https://qiita.com/gomamitu/items/b4722741f6318d734bce ... i think there is no differences (magnifying is needed lol)
   * https://meknowledge.jpn.org/2021/05/28/python-super-resolution/ ... same impression
 * `Dnn#readNetFromTensorflow()`
 * resources in "jar in jar" for mac .app
   * dnnsuperres error in mac.app
   * because not "resources in jar (we can see those as individual files)" but "resources in a resource jar (we can see the jar only)"
     * -> need to extract
