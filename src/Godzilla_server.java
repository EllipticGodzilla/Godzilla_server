import gui.Server_frame;
import gui.Terminal_panel;
import network.Net_listener;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.swing.*;
import java.awt.*;
import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.*;
import java.util.Random;
import java.util.Vector;

public class Godzilla_server {
    public static void main(String[] args) throws IOException, InvocationTargetException, NoSuchMethodException, IllegalAccessException, InstantiationException {
        Vector<Image> icons = new Vector<>();

        icons.add(new ImageIcon(Godzilla_server.class.getResource("images/icon_16.png")).getImage());
        icons.add(new ImageIcon(Godzilla_server.class.getResource("images/icon_32.png")).getImage());
        icons.add(new ImageIcon(Godzilla_server.class.getResource("images/icon_64.png")).getImage());
        icons.add(new ImageIcon(Godzilla_server.class.getResource("images/icon_128.png")).getImage());

        Server_frame.init().setIconImages(icons);
        Runtime.getRuntime().addShutdownHook(new Thread(shutdown_hook));
    }

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
            FileOutputStream file_writer = new FileOutputStream(Server_frame.project_path + "log.dat");
            file_writer.write(Terminal_panel.get_terminal_log().getBytes());

            file_writer.close();
        }
    };
}
