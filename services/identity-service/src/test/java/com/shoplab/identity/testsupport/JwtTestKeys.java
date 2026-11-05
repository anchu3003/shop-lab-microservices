package com.shoplab.identity.testsupport;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

/**
 * Sinh cặp khoá RSA tạm cho test.
 *
 * <p><b>Vì sao không dùng {@code infra/keys/dev-*.pem}</b>: khoá dev bị gitignore (không có trong
 * repo) và đường dẫn phụ thuộc thư mục chạy → test sẽ vỡ ở máy khác hoặc trong CI. Sinh khoá ngay
 * trong test thì test tự chứa, chạy ở đâu cũng được, mà vẫn đi qua đúng code đọc PEM của {@code
 * JwtService}.
 *
 * <p>Tên class cố ý <b>không</b> khớp pattern của surefire ({@code Test*}, {@code *Test}, {@code
 * *Tests}, {@code *TestCase}) — nếu khớp, surefire sẽ cố chạy nó như một test class.
 */
public record JwtTestKeys(Path privateKeyPath, Path publicKeyPath, KeyPair keyPair) {

  private static final int KEY_SIZE = 2048;

  /** Sinh khoá và ghi ra PEM (PKCS#8 + SPKI) trong {@code dir}. */
  public static JwtTestKeys generateIn(Path dir) {
    try {
      KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
      generator.initialize(KEY_SIZE);
      KeyPair keyPair = generator.generateKeyPair();

      Path privateKeyPath = dir.resolve("test-private.pem");
      Path publicKeyPath = dir.resolve("test-public.pem");
      writePem(privateKeyPath, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
      writePem(publicKeyPath, "PUBLIC KEY", keyPair.getPublic().getEncoded());

      return new JwtTestKeys(privateKeyPath, publicKeyPath, keyPair);
    } catch (GeneralSecurityException | IOException e) {
      throw new IllegalStateException("Không sinh được khoá test", e);
    }
  }

  private static void writePem(Path path, String label, byte[] der) throws IOException {
    String body =
        Base64.getMimeEncoder(64, "\n".getBytes(StandardCharsets.US_ASCII)).encodeToString(der);
    Files.writeString(
        path, "-----BEGIN " + label + "-----\n" + body + "\n-----END " + label + "-----\n");
  }
}
