package app.bpartners.api.service.aws;

import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.file.BucketKeyRetriever;
import app.bpartners.api.file.bucket.ExtendedBucketComponent;
import app.bpartners.api.file.hash.FileHash;
import java.io.File;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class S3Service {
  private final ExtendedBucketComponent extendedBucketComponent;
  private final BucketKeyRetriever bucketKeyRetriever;

  public String presignURL(
      FileType fileType, String fileId, String idUser, Long expirationInSeconds) {
    String key = bucketKeyRetriever.apply(fileType, fileId, idUser);
    return extendedBucketComponent.presign(key, Duration.ofSeconds(expirationInSeconds)).toString();
  }

  @SneakyThrows
  public FileHash uploadFile(FileType fileType, String fileId, String idUser, File fileToUpload) {
    String key = bucketKeyRetriever.apply(fileType, fileId, idUser);
    return extendedBucketComponent.upload(fileToUpload, key, true);
  }

  public File downloadFile(FileType fileType, String fileId, String idUser) {
    String key = bucketKeyRetriever.apply(fileType, fileId, idUser);
    return extendedBucketComponent.download(key, true);
  }

  public File downloadLandingFile(String key) {
    return extendedBucketComponent.download(key, false);
  }

  public String uploadLandingFile(File file, String key) {
    extendedBucketComponent.upload(file, key, false);
    return extendedBucketComponent.presignLanding(key, Duration.ofSeconds(60), false).toString();
  }
}
