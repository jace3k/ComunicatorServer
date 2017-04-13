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
                String name = getClientName(incoming);
                boolean found = false;
                for(ClientIn c : client_table) {
                    if(c.getName().equals(name)) {
                        found = true;
                    }
                }
                if(!found) {
                    ClientIn client = new ClientIn(name, incoming);
                    client_table.add(client);
                    counter++;
                    System.out.println("Klient połączony");

                    new Thread(new ClientThread(client)).start();

                    sendActiveUsers();
                } else {
                    System.out.println("Nazwa już istnieje! Odrzucam.");
                    incoming.close();
                }
            } catch (Exception e) {
                System.out.println("Błąd w startServer.");
            }
        }
    }

    private String getClientName(Socket s) throws IOException {
        Scanner scanner = new Scanner(s.getInputStream());
        String line = scanner.nextLine();
        //scanner.close();
        return line;
    }

    private synchronized void sendActiveUsers() throws IOException {
        for(ClientIn c : client_table) {
            PrintWriter pw = new PrintWriter(c.getSocket().getOutputStream(),true);
            pw.println("~qxqxstart");
            for(ClientIn s : client_table) {
                pw.println(s.getPort()+"~"+s.getName());
                System.out.println("Aktywny: "+s.getName());
            }
            pw.println("~qxqxend");
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
                    String splitLine[] = line.split("~");
                    for(ClientIn cl : client_table) {

                        if(cl.getPort() == Integer.parseInt(splitLine[1])) {
                            PrintWriter writer = new PrintWriter(cl.getSocket().getOutputStream(), true);

                            writer.println(client.getName() + ": " + splitLine[2]);
                            out.println(client.getName() + ": " + splitLine[2]);
                            System.out.println(client.getName() + " wysłał do "+ cl.getName());
                        }
                    }
                }
                System.out.println("Wątek zakończony.");
                remove(client);

                client.getSocket().close();
                sendActiveUsers();

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Błąd w run().");
            }
        }
        private synchronized void remove(ClientIn cli) {
            client_table.remove(cli);
        }
    }
}