package app.bpartners.api.service.annotation;

import static app.bpartners.api.service.annotation.ExportAreaPictureAnnotationPDFProcessor.IMAGE_FORMAT;
import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

@Slf4j
class ImageCompressorTest {

  ImageCompressor subject = new ImageCompressor();

  @Test
  void compressByteArray_should_produce_valid_image_under_target_size() throws IOException {
    byte[] originalBytes =
        new ClassPathResource("files/image-with-vegetation.jpg").getInputStream().readAllBytes();

    byte[] actual = subject.compressImage(originalBytes);

    assertTrue(actual.length <= originalBytes.length);
    BufferedImage actualBuffered = ImageIO.read(new ByteArrayInputStream(actual));
    assertNotNull(actualBuffered);
  }

  @Test
  void compress_image_should_respect_target_size_and_max_dimensions() throws IOException {
    BufferedImage original =
        ImageIO.read(new ClassPathResource("files/image-with-vegetation.jpg").getInputStream());
    long originalSize = getImageSizeBytes(original);

    BufferedImage actual = subject.compressImage(original);

    long actualSize = getImageSizeBytes(actual);

    log.info("Original size (bytes): {}", originalSize);
    log.info("Actual size (bytes):   {}", actualSize);
    log.info("Difference:            {}", originalSize - actualSize);
    log.info("Original dimensions:   {}x{}", original.getWidth(), original.getHeight());
    log.info("Actual dimensions:     {}x{}", actual.getWidth(), actual.getHeight());

    assertTrue(
        actualSize <= originalSize,
        "Expected compressed size (" + actualSize + ") to be <= original size (" + originalSize + ")"
    );
  }
  private long getImageSizeBytes(BufferedImage image) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(image, IMAGE_FORMAT, baos);
    return baos.size();
  }

  @Test
  void compressImage_should_throw_when_image_format_invalid() {
    BufferedImage img = null;

    assertThrows(RuntimeException.class, () -> subject.compressImage(img));
  }

  @Test
  void compressImage_should_throw_when_invalid_image_bytes() {
    byte[] invalid = "invalid".getBytes();

    assertThrows(RuntimeException.class, () -> subject.compressImage(invalid));
  }
}
