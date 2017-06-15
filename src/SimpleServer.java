import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Scanner;

public class SimpleServer {

    ArrayList<ClientThread> clients = new ArrayList<>();
    SQLConnection sqlConnection = new SQLConnection();

    public void startServer() {
        try (ServerSocket s = new ServerSocket(8189)) {
            System.out.println("Wystatrował.");
            while (true) {
                Socket incoming = s.accept();
                ClientThread client = new ClientThread(incoming);
                clients.add(client);
                new Thread(client).start();

                new Thread(() -> {
                    try {
                        Thread.sleep(1000);
                        for(ClientThread c : clients) {
                            sendMessage(8, c.nick, c.nick, buildActiveFriends(8+"/"+c.nick), new PrintWriter(c.socket.getOutputStream(), true));
                        }
                    } catch (InterruptedException | IOException e) {
                        e.printStackTrace();
                    }

                }).start();



            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Błąd w startServer.");
        }
    }

    public void sendMessage(int header, String source, String destination, String message, PrintWriter out) {
        out.println(header + "/" + source + "/" + destination + "/" + message);
        System.out.println("Wysłano: " +header + "/" + source + "/" + destination + "/" + message);
    }

    String buildActiveFriends(String line) {
        String[] lineParts = line.split("/");
        String userFriends = sqlConnection.getFriends(lineParts[1]);
        String friendsTable[] = userFriends.split("/");
        StringBuilder friendsToSend = new StringBuilder();
        for(ClientThread c : clients) {
            for(String friend : friendsTable) {
                if(c.getNick().equals(friend)) {
                    friendsToSend.append(friend);
                    friendsToSend.append("/");
                }
            }
        }
        return friendsToSend.toString();
    }


    class ClientThread implements Runnable {
        Socket socket;
        //PrintWriter out;
        Scanner in;

        private String line;
        private String nick = "null";


        public ClientThread(Socket socket) {
            this.socket = socket;
        }



        @Override
        public void run() {
            System.out.println("Mamy nowego klienta.");


            try {

                in = new Scanner(socket.getInputStream());

                while(in.hasNextLine()) {
                    line = in.nextLine();
                    System.out.println("Odebrano: " + line);
                    analyze(line);
                }
            } catch (IOException | NullPointerException e) {
                e.printStackTrace();
            }
            System.out.println("Koniec wątku klienta.");
            try {


                clients.remove(this);

                for(ClientThread c : clients) {
                    sendMessage(8, c.nick, c.nick, buildActiveFriends(8+"/"+c.nick), new PrintWriter(c.socket.getOutputStream(), true));
                }



                socket.close();
                in.close();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public String getNick() {
            return nick;
        }


        private synchronized void analyze(String line) throws IOException, NullPointerException {
            switch (line.charAt(0)) {
                case '1':
                    sendUsersList(line);
                    break;
                case '2':
                    messageSendToBoth(line);
                    break;
                case '3':
                    sendInfoLabel(line);
                    break;
                case '4':
                    if(sqlConnection.checkLogin(line)) {
                        sendLoginSuccess(line);
                    } else {
                        sendLoginFailed();
                    }
                    break;
                case '5':
                    if(sqlConnection.checkRegister(line)) {
                        sendRegisterSuccess();
                    } else {
                        sendRegisterFailed();
                    }
                    break;
                case '6':
                    sendHistory(line);
                    break;
                case '7':
                    if(sqlConnection.addFriend(line)) {
                        sendAddFriendSuccess(line);
                    } else {
                        sendAddFriendFailed(line);
                    }
                    break;
                case '8':
                    sendActive(line);
                    break;
            }
        }


        private void sendActive(String line) throws IOException {
            //lista aktywnych
            sendMessage(8, nick, nick, buildActiveFriends(line), new PrintWriter(socket.getOutputStream(), true));
        }

        private void sendAddFriendFailed(String line) {
            System.out.println("Nie powiodło sie dodawanie friendsa.");
        }

        private void sendAddFriendSuccess(String line) throws IOException {
            sendUsersList(line);
        }

        private void sendRegisterFailed() throws IOException {
            sendMessage(5,nick,nick,"false", new PrintWriter(socket.getOutputStream(), true));
        }

        private void sendRegisterSuccess() throws IOException {
            sendMessage(5,nick,nick,"true", new PrintWriter(socket.getOutputStream(), true));
        }

        private void sendHistory(String line) throws IOException {
            String[] lineParts = line.split("/");
            ArrayList<String> filteredHistory = sqlConnection.getHistory(lineParts[1]);
            for (String s : filteredHistory) {
                sendMessage(6,nick,nick,s, new PrintWriter(socket.getOutputStream(), true));
            }

        }

        private void sendLoginFailed() throws IOException {
            sendMessage(4,nick,nick,"false", new PrintWriter(socket.getOutputStream(), true));
            socket.close();
        }

        private void sendLoginSuccess(String line) throws IOException {
            String[] lineParts = line.split("/");
            nick = lineParts[3];
            System.out.println("Ustawiam nick na:" + nick);
            sendMessage(4,nick,nick,"true", new PrintWriter(socket.getOutputStream(), true));
        }

        private void setNick(String line) {
            String[] partsOfMessage = line.split("/");
            this.nick = partsOfMessage[1];
            System.out.println("Ustawiam nick: " + partsOfMessage[1]);
        }

        private void sendInfoLabel(String line) throws IOException {
            try {
                String[] partsOfMessage = line.split("/");
                String typing = partsOfMessage[3];
                PrintWriter p2;
                //sendMessage(3,nick,nick,"LOL", new PrintWriter(socket.getOutputStream(), true));

                for (ClientThread c : clients) {


                    if (c.getNick().equals(partsOfMessage[2])) {
                        p2 = new PrintWriter(c.socket.getOutputStream(), true);
                        sendMessage(3,partsOfMessage[1],partsOfMessage[2],typing, p2);
                    }
                }
            }catch (NullPointerException e) {
                e.printStackTrace();
                System.out.println("Pusta linia.");
            }

        }

        private synchronized void messageSendToBoth(String line) throws IOException, NullPointerException {
            String[] partsOfMessage = line.split("/");
            PrintWriter p1;
            PrintWriter p2;
            for(ClientThread c : clients) {
                if(c.getNick().equals(partsOfMessage[2])) {
                    p1 = new PrintWriter(c.socket.getOutputStream(), true);
                    sendMessage(2,partsOfMessage[1],partsOfMessage[2],partsOfMessage[3], p1);
                }

                if(c.getNick().equals(partsOfMessage[1])) {
                    p2 = new PrintWriter(c.socket.getOutputStream(), true);
                    sendMessage(2,partsOfMessage[1],partsOfMessage[2],partsOfMessage[3], p2);
                }
            }

            sqlConnection.putHistory(partsOfMessage[1], partsOfMessage[1], partsOfMessage[2], partsOfMessage[3]);
            sqlConnection.putHistory(partsOfMessage[2], partsOfMessage[1], partsOfMessage[2], partsOfMessage[3]);
        }

        private synchronized void sendUsersList(String line) throws IOException {
            String[] lineParts = line.split("/");


            String userFriends = sqlConnection.getFriends(lineParts[1]);
            sendMessage(1,nick,nick,userFriends, new PrintWriter(socket.getOutputStream(), true));
        }
    }

}
