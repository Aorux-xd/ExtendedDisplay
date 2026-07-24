package dev.ed.edcore.api.crypto;

public interface CryptoManager {
    enum Algorithm {
        BCRYPT,
        PBKDF2,
        SHA512
    }

    String hash(String secret, Algorithm algorithm);

    boolean verify(String secret, String encodedHash);

    String generateTotpSecret();

    boolean verifyTotp(String secret, int code);
}
