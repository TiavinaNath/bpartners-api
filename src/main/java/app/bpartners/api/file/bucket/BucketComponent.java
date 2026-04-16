package app.bpartners.api.file.bucket;

import static java.io.File.createTempFile;

import app.bpartners.api.PojaGenerated;
import app.bpartners.api.file.hash.FileHash;
import app.bpartners.api.file.hash.FileHashAlgorithm;
import java.io.File;
import java.net.URL;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.DownloadFileRequest;
import software.amazon.awssdk.transfer.s3.model.FileDownload;
import software.amazon.awssdk.transfer.s3.model.UploadDirectoryRequest;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;
import software.amazon.awssdk.transfer.s3.progress.LoggingTransferListener;

@PojaGenerated
@SuppressWarnings("all")
@Component
@AllArgsConstructor
public class BucketComponent {

  private final BucketConf bucketConf;
  private final BucketLandingConf bucketLandingConf;

  public FileHash upload(File file, String bucketKey, boolean isPrincipalBucket) {
    return file.isDirectory()
        ? uploadDirectory(file, bucketKey, isPrincipalBucket)
        : uploadFile(file, bucketKey, isPrincipalBucket);
  }

  private String getBucketName(boolean isBucketPrincipal) {
    return isBucketPrincipal ? bucketConf.getBucketName() : bucketLandingConf.getBucketName();
  }

  private S3TransferManager getTransferManager(boolean isBucketPrincipal) {
    return isBucketPrincipal
        ? bucketConf.getS3TransferManager()
        : bucketLandingConf.getS3TransferManager();
  }

  private S3Presigner getPresigner(boolean isBucketPrincipal) {
    return isBucketPrincipal ? bucketConf.getS3Presigner() : bucketLandingConf.getS3Presigner();
  }

  private FileHash uploadDirectory(File file, String bucketKey, boolean isBucketPrincipal) {
    var request =
        UploadDirectoryRequest.builder()
            .source(file.toPath())
            .bucket(getBucketName(isBucketPrincipal))
            .s3Prefix(bucketKey)
            .build();
    var upload = getTransferManager(isBucketPrincipal).uploadDirectory(request);
    var uploaded = upload.completionFuture().join();
    if (!uploaded.failedTransfers().isEmpty()) {
      throw new RuntimeException("Failed to upload following files: " + uploaded.failedTransfers());
    }
    return new FileHash(FileHashAlgorithm.NONE, null);
  }

  private FileHash uploadFile(File file, String bucketKey, boolean isBucketPrincipal) {
    var request =
        UploadFileRequest.builder()
            .source(file)
            .putObjectRequest(req -> req.bucket(getBucketName(isBucketPrincipal)).key(bucketKey))
            .addTransferListener(LoggingTransferListener.create())
            .build();
    var upload = getTransferManager(isBucketPrincipal).uploadFile(request);
    var uploaded = upload.completionFuture().join();
    return new FileHash(FileHashAlgorithm.SHA256, uploaded.response().checksumSHA256());
  }

  @SneakyThrows
  public File download(String bucketKey, boolean isPrincipalBucket) {
    var destination =
        createTempFile(prefixFromBucketKey(bucketKey), suffixFromBucketKey(bucketKey));
    FileDownload download =
        getTransferManager(isPrincipalBucket)
            .downloadFile(
                DownloadFileRequest.builder()
                    .getObjectRequest(
                        GetObjectRequest.builder()
                            .bucket(getBucketName(isPrincipalBucket))
                            .key(bucketKey)
                            .build())
                    .destination(destination)
                    .build());
    download.completionFuture().join();
    return destination;
  }

  private String prefixFromBucketKey(String bucketKey) {
    return lastNameSplitByDot(bucketKey)[0];
  }

  private String suffixFromBucketKey(String bucketKey) {
    var splitByDot = lastNameSplitByDot(bucketKey);
    return splitByDot.length == 1 ? "" : splitByDot[splitByDot.length - 1];
  }

  private String[] lastNameSplitByDot(String bucketKey) {
    var splitByDash = bucketKey.split("/");
    var lastName = splitByDash[splitByDash.length - 1];
    return lastName.split("\\.");
  }

  public URL presign(String bucketKey, Duration expiration) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(bucketConf.getBucketName()).key(bucketKey).build();
    PresignedGetObjectRequest presignedRequest =
        bucketConf
            .getS3Presigner()
            .presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build());
    return presignedRequest.url();
  }

  public URL presignLanding(String bucketKey, Duration expiration, boolean isBucketPrincipal) {
    GetObjectRequest getObjectRequest =
        GetObjectRequest.builder().bucket(getBucketName(isBucketPrincipal)).key(bucketKey).build();
    PresignedGetObjectRequest presignedRequest =
        getPresigner(isBucketPrincipal)
            .presignGetObject(
                GetObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .getObjectRequest(getObjectRequest)
                    .build());
    return presignedRequest.url();
  }
}
