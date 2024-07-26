import gui.*;
import network.Net_listener;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.util.Arrays;
import java.util.Base64;
import java.util.Vector;

public class Godzilla_server {
    public static void main(String[] args) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Vector<Image> icons = new Vector<>();

        icons.add(new ImageIcon(Godzilla_server.class.getResource("/images/icon_16.png")).getImage());
        icons.add(new ImageIcon(Godzilla_server.class.getResource("/images/icon_32.png")).getImage());
        icons.add(new ImageIcon(Godzilla_server.class.getResource("/images/icon_64.png")).getImage());
        icons.add(new ImageIcon(Godzilla_server.class.getResource("/images/icon_128.png")).getImage());

        Server_frame.init().setIconImages(icons);
        Runtime.getRuntime().addShutdownHook(new Thread(shutdown_hook));

        ask_psw.success(); //richiede la password
    }

    private static TempPanel_action init_data = new TempPanel_action() {
        @Override
        public void success() {
            try {
//                i primi 32 byte dell'hash servono a controllare la validità della password,
//                gli altri vengono utilizzati come password AES per decifrare i file
                MessageDigest md = MessageDigest.getInstance("SHA3-512");
                byte[] hash = md.digest(input.elementAt(0).getBytes());

                byte[] comp_hash = Godzilla_server.class.getClassLoader().getResourceAsStream("files/FileKey.dat").readAllBytes();
                if (Arrays.compare(hash, 32, 64, comp_hash, 0, 32) == 0) { //se i primi 32 byte dei due hash sono uguali la password è corretta
                    SecretKey fileKey = new SecretKeySpec(Arrays.copyOfRange(hash, 0, 16), "AES"); //utilizza gli ultimi 32 byte dell'hash come password AES
                    IvParameterSpec iv  = new IvParameterSpec(Arrays.copyOfRange(hash, 16, 32));

                    Server_frame.file_decoder = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    Server_frame.file_encoder = Cipher.getInstance("AES/CBC/PKCS5Padding");
                    Server_frame.file_decoder.init(Cipher.DECRYPT_MODE, fileKey, iv);
                    Server_frame.file_encoder.init(Cipher.ENCRYPT_MODE, fileKey, iv);

                    Net_listener.init();
                    Net_listener.set_ready(); //inizializzazione completata
                }
                else { //se gli hash differiscono è stata inserita una password sbagliata
                    fail();
                }
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void fail() {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.SINGLE_MSG,
                    false,
                    "la password inserita non è corretta"
            ), ask_psw);
        }
    };

    private static TempPanel_action ask_psw = new TempPanel_action() {
        @Override
        public void success() {
            TempPanel.show(new TempPanel_info(
                    TempPanel_info.INPUT_REQ,
                    false,
                    "inserisci la password:"
            ).set_psw_indices(0), init_data);
        }

        @Override
        public void fail() {}
    };

    private static Runnable shutdown_hook = new Runnable() {
        @Override
        public void run() {
            try {
                Net_listener.stop();

                Net_listener.save_credentials();
                save_terminal_log();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        private void save_terminal_log() throws IOException {
            FileOutputStream file_writer = new FileOutputStream(Server_frame.project_path + "/database/log.dat");
            file_writer.write(Terminal_panel.get_terminal_log().getBytes());

            file_writer.close();
        }
    };
}
