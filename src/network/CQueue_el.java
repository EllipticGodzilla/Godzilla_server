package network;

public class CQueue_el {
    private byte[] msg;
    private boolean reply;
    private On_arrival action;

    public CQueue_el(byte[] msg, boolean reply, On_arrival action) {
        this.msg = msg;
        this.reply = reply;
        this.action = action;
    }

    public byte[] get_msg() {
        return msg;
    }

    public boolean get_reply() {
        return reply;
    }

    public On_arrival get_action() {
        return action;
    }
}
