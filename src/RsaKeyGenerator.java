import java.security.KeyPair;
import java.security.KeyPairGenerator;

public class RsaKeyGenerator {

    public static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            return kpg.genKeyPair();
        } catch (Exception e) {
            System.out.println("Couldn't generate new keypair");
            return null;
        }
    }
}
