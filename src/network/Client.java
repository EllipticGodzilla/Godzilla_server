package network;

import gui.Terminal_panel;

import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.Socket;
import java.util.Arrays;

public class Client implements Runnable {
    private Connection connection;

    public Client(Socket sck) throws IOException {
        this.connection = new Connection(sck);
    }

    @Override
    public void run() {
        try {
            Terminal_panel.terminal_write("un nuovo client si è connesso\n", false);
            connection.write(Net_listener.server_info, true); //invia informazioni del server ed il suo certificato, reply: true non ha nessun valore in questo momento poiché connection non è dinamico
            connection.write(Net_listener.certificate, true);
            Terminal_panel.terminal_write("inviato il certificato\n", false);

            byte[] key_check_bytes = Net_listener.decoder.doFinal(connection.wait_for_bytes()); //riceve la chiave AES ed o byte di check per la sessione e li decifra utilizzando la sua chiave privata
            byte[] session_key_bytes = Arrays.copyOfRange(key_check_bytes, 0, 32); //i primi 32 byte formano la chiave di sessione per AES
            byte[] check_bytes = Arrays.copyOfRange(key_check_bytes, 32, 40); //gli ultimi 6 byte sono i check byte

            AESCipher cipher = new AESCipher(session_key_bytes, check_bytes);

//            SecretKey session_key = new SecretKeySpec(session_key_bytes, "AES");
//            Cipher encoder = Cipher.getInstance("AES");
//            Cipher decoder = Cipher.getInstance("AES");
//
//            encoder.init(Cipher.ENCRYPT_MODE, session_key);
//            decoder.init(Cipher.DECRYPT_MODE, session_key);
            connection.set_cipher(cipher); //inizia a cifrare la connessione utilizzando la session key appena ricevuta

            connection.write(check_bytes, true); //invia i check byte cifrati con AES al client

            Terminal_panel.terminal_write("instaurata una connessione sicura, attendo per il login\n", false);

            connection.start_dinamic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
