package network;

import java.io.IOException;

public interface On_arrival {
    void on_arrival(byte[] msg, Receiver receiver);
    default void timedout(Receiver receiver) throws IOException {
        receiver.stop();
    }
}