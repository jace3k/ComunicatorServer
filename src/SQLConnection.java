import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

public class SQLConnection {

    public static final String DRIVER = "org.sqlite.JDBC";
    public static final String DB_URL = "jdbc:sqlite:history.db";

    private java.sql.Connection conn;
    private java.sql.Statement stat;


    public SQLConnection() {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            System.err.println("Brak sterownika JDBC");
            e.printStackTrace();
        }
        try {
            conn = DriverManager.getConnection(DB_URL);
            stat = conn.createStatement();
        } catch (SQLException e) {
            System.err.println("Problem z otwarciem polaczenia");
            e.printStackTrace();
        }

        try {
            stat.execute("create table if not exists users (login VARCHAR (20), password VARCHAR (20), status INT )");
            stat.execute("create table if not exists history (login VARCHAR (20), source VARCHAR(20), destination VARCHAR(20), message VARCHAR )");
            stat.execute("create table if not exists friends (login VARCHAR(20), friend VARCHAR(20))");

        } catch (SQLException ex) {
            ex.printStackTrace();
            System.err.println("Blad przy tworzeniu tabeli");
        }
    }

    public boolean checkLogin(String line) {
        String[] lineParts = line.split("/");
        try {
            ResultSet result = stat.executeQuery("SELECT * FROM users WHERE login='"+lineParts[3]+"' AND password='"+lineParts[4]+"'");
            if(result.next()) {
                System.out.println("Udało się zalogować.");
                return true;
            } else return false;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkRegister(String line) {
        String lineParts[] = line.split("/");

        try {
            ResultSet result = stat.executeQuery("SELECT * FROM users WHERE login='"+lineParts[3]+"'");
            if(result.next()) {
                System.out.println("Użytkownik już istnieje.");
                return false;
            } else {
                putUser(lineParts[3], lineParts[4], "1");
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void putUser(String login, String password, String status) {
        try {
            PreparedStatement prepStmt = conn.prepareStatement("insert into users values (?, ?, ?);");
            prepStmt.setString(1, login);
            prepStmt.setString(2, password);
            prepStmt.setString(3, status);
            prepStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Blad przy wstawianiu wpisu");
        }
    }

    public void putHistory(String user, String source, String destination, String msg) {
        try {
            PreparedStatement prepStmt = conn.prepareStatement("insert into history values (?, ?, ?, ?);");
            prepStmt.setString(1, user);
            prepStmt.setString(2, source);
            prepStmt.setString(3, destination);
            prepStmt.setString(4, msg);
            prepStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Blad przy wstawianiu wpisu");
        }
    }

    public ArrayList<String> getHistory(String user) {
        ArrayList<String> filteredHistory = new ArrayList<>();
        int counter = 0;
        try {
            ResultSet result = stat.executeQuery("SELECT * FROM history WHERE login='"+user+"'");
            while(result.next()) {
                String message = result.getString("source") + "/" + result.getString("destination") + "/" + result.getString("message");
                filteredHistory.add(message);
                counter++;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        System.out.println("Dodano " + counter + " wpisów z hostorii.");
        return filteredHistory;
    }

    public String getFriends(String user) {
        StringBuilder friends = new StringBuilder();
        try {
            ResultSet result = stat.executeQuery("SELECT * FROM friends WHERE login='"+user+"'");
            while(result.next()) {
                friends.append(result.getString("friend"));
                friends.append("/");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return friends.toString();
    }

    public boolean addFriend(String line) {
        String[] lineParts = line.split("/");
        // lineParts[3] - friend do dodania
        // lineParts[1] - nick usera
        boolean userNotExists = false;
        boolean userAlreadyAdded = false;




        try {
            ResultSet result = stat.executeQuery("SELECT * FROM users WHERE login='"+lineParts[3]+"'" );
            if(!result.next()) {
                userNotExists = true;
                System.out.println("Dodawanie friendsa: Nie ma takiego użytkownika.");
            }
            result = stat.executeQuery("SELECT * FROM friends WHERE login='"+lineParts[1]+"' AND friend='"+lineParts[3]+"'");
            if(result.next()) {
                userAlreadyAdded = true;
                System.out.println("Dodawanie friendsa: Użytkownik już dodany.");
            }

            if(userAlreadyAdded || userNotExists) {
                return false;
            } else {
                PreparedStatement prepStmt = conn.prepareStatement("insert into friends values (?, ?);");
                prepStmt.setString(1, lineParts[1]);
                prepStmt.setString(2, lineParts[3]);
                prepStmt.execute();
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Blad przy wstawianiu wpisu");
            return false;
        }
    }
}
