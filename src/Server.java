import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Created by Jacek on 06.04.2017.
 */
public class Server {
    private ArrayList<ClientIn> client_table = new ArrayList<>();
    int counter = 1;

    public void startServer() {
        while (true) {
            try (ServerSocket s = new ServerSocket(8189)) {

                Socket incoming = s.accept();
                String name = "Klient " + counter;
                ClientIn client = new ClientIn(name, incoming);
                client_table.add(client);
                counter++;
                System.out.println("Klient połączony");



                new Thread(new ClientThread(client)).start();

                sendActiveUsers();
            } catch (Exception e) {
                System.out.println("Błąd w startServer.");
            }
        }
    }

    private void sendActiveUsers() throws IOException {
        for(ClientIn c : client_table) {
            PrintWriter pw = new PrintWriter(c.getSocket().getOutputStream(),true);
            pw.println("qxqxstart");
            for(ClientIn s : client_table) {
                pw.println(s.getPort()+"~"+s.getName());
                System.out.println("Aktywny: "+s.getName());
            }
            pw.println("qxqxend");
        }
        System.out.println("Rozesłano userów do wszystkich.");
    }


    class ClientThread implements Runnable {
        ClientIn client;

        ClientThread(ClientIn client) {
            this.client = client;
        }
        @Override
        public void run() {
            try {
                Scanner in = new Scanner(client.getSocket().getInputStream());
                PrintWriter out = new PrintWriter(client.getSocket().getOutputStream(),true);
                out.println("Witaj na serwerze! <"+ client.getName()+">");

                while(in.hasNextLine()) {
                    String line = in.nextLine();
                    System.out.println("Wiadomość: " + line);

                    for(ClientIn cl : client_table) {
                        String splitLine[] = line.split("~");
                        if(cl.getPort() == Integer.parseInt(splitLine[0])) {
                            PrintWriter writer = new PrintWriter(cl.getSocket().getOutputStream(), true);

                            writer.println(client.getName() + ": " + splitLine[1]);
                            out.println(client.getName() + ": " + splitLine[1]);
                            System.out.println(client.getName() + " wysłał do "+ cl.getName());
                        }
                    }
                }
                System.out.println("Wątek zakończony.");
                client_table.remove(client);

                client.getSocket().close();
                sendActiveUsers();

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Błąd w run().");
            }
        }
    }
}