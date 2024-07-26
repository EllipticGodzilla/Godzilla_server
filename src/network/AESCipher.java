package network;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

public class AESCipher {
    private Cipher encoder,
                   decoder;
    private Random random;
    private final SecretKey key;

    public AESCipher(byte[] key, byte[] rnd_seed) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        random = new Random(); //inizializza random
        random.setSeed(byte_array_to_long(rnd_seed));

        this.key = new SecretKeySpec(key, "AES"); //inizializza la key

        encoder = Cipher.getInstance("AES/CBC/PKCS5Padding"); //inizializza i cipher
        decoder = Cipher.getInstance("AES/CBC/PKCS5Padding");

        IvParameterSpec iv = next_iv(); //genera un iv casuale
        encoder.init(Cipher.ENCRYPT_MODE, this.key, iv);
        decoder.init(Cipher.DECRYPT_MODE, this.key, iv);
    }

    public byte[] encode(byte[] msg) throws IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] encoded_msg = encoder.doFinal(msg); //decodifica il messaggio
        regen_iv(); //rigenera gli iv

        return encoded_msg;
    }

    public byte[] decode(byte[] msg) throws IllegalBlockSizeException, BadPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] plain_msg = decoder.doFinal(msg);
        regen_iv();

        return plain_msg;
    }

    private long byte_array_to_long(byte[] arr) {
        long result = 0;
        for (int i = 0; i < 8; i++) {
            result |= arr[i]; //aggiunge un byte in fondo al long
            result <<= 8; //sposta tutti i bit a sinistra di 8 posizioni
        }

        return result;
    }

    private void regen_iv() throws InvalidAlgorithmParameterException, InvalidKeyException { //rigenera gli iv, in modo che cifrando più volte uno stesso messaggio il risultato sia sempre differente
        IvParameterSpec iv = next_iv();
        encoder.init(Cipher.ENCRYPT_MODE, key, iv);
        decoder.init(Cipher.DECRYPT_MODE, key, iv);
    }

    private IvParameterSpec next_iv() {
        byte[] iv_byte = new byte[encoder.getBlockSize()];
        random.nextBytes(iv_byte);

        return new IvParameterSpec(iv_byte);
    }
}
