import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Jacek on 09.04.2017.
 */
public class ClientIn {
    private String name;
    private Socket socket;
    private int port;
    private int id;
    public Map<String, Integer> history;

    public ClientIn(String name, Socket socket) {
        this.name = name;
        this.socket = socket;
        this.port = socket.getPort();
        history = new HashMap<>();
    }


    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

}
