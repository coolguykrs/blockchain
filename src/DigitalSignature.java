
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.*;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

public class DigitalSignature {

    private static final String PUBLIC_KEY_FILE_PATH = "resources/keys/publicKey.pub";
    private static final String PRIVATE_KEY_FILE_PATH = "resources/keys/privateKey";

    //The method that signs the data using the private key
    public static byte[] sign(String data, PrivateKey privateKey) throws Exception {
        Signature rsa = Signature.getInstance("SHA1withRSA");
        rsa.initSign(privateKey);
        rsa.update(data.getBytes());
        return rsa.sign();
    }

    //Method to retrieve the Private Key from a file
    public static PrivateKey getPrivateFromFile(String fileName) {
        try {
            File f = new File(fileName);
            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int)f.length()];
            dis.readFully(keyBytes);
            dis.close();
            PKCS8EncodedKeySpec privSpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(privSpec);
        } catch (Exception e) {
            return null;
        }
    }

    //Method to retrieve the Public Key from a file
    public static PublicKey getPublicFromFile(String fileName) {
        try {
            File f = new File(fileName);
            FileInputStream fis = new FileInputStream(f);
            DataInputStream dis = new DataInputStream(fis);
            byte[] keyBytes = new byte[(int)f.length()];
            dis.readFully(keyBytes);
            dis.close();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            return null;
        }
    }

    public static PublicKey getPublicKey() {
        PublicKey pub = null;
        pub = getPublicFromFile(PUBLIC_KEY_FILE_PATH);
        if (pub == null) {
            KeyPair keyPair = newKeyPair();
            pub = keyPair.getPublic();
        }
        return pub;
    }

    public static String keyToString(byte[] bytes) {
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(bytes);
    }


    public static String signTransaction(String transaction) {
        PrivateKey privateKey;
        String result = "";
        try {
            privateKey = getPrivateFromFile(PRIVATE_KEY_FILE_PATH);
            if (privateKey == null) {
                KeyPair keyPair = newKeyPair();
                privateKey = keyPair.getPrivate();
            }
            byte[] encryptedTransaction = DigitalSignature.sign(transaction, privateKey);
            result = Base64.getEncoder().encodeToString(encryptedTransaction);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static KeyPair newKeyPair() {
        System.out.println("No keys found, generating new keys");
        KeyPair keyPair = RsaKeyGenerator.generateKeyPair();
        writeKeyToFile(keyPair.getPublic().getEncoded(), PUBLIC_KEY_FILE_PATH, "PUBLIC");
        System.out.println(keyToString(keyPair.getPublic().getEncoded()));
        writeKeyToFile(keyPair.getPrivate().getEncoded(), PRIVATE_KEY_FILE_PATH, "PRIVATE");
        return keyPair;
    }

    public static void writeKeyToFile(byte[] keyBytes, String filename, String pubOrPriv) {
        try {
            FileOutputStream fos = new FileOutputStream(filename);
            fos.write(keyBytes);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
