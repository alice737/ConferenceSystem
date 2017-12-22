package model;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.io.IOException;
import java.sql.*;
import java.util.Optional;

public class Server {

    private static final String DB_DRIVER = "oracle.jdbc.driver.OracleDriver";
    private static final String DB_CONNECTION = "jdbc:oracle:thin:@localhost:1521:orcl";
    private static final String DB_USER = "TO_PROJEKT";
    private static final String DB_PASSWORD = "projekt";

    private static final String TABELA_UZYTKOWNIK = "UZYTKOWNIK";
    private static final String TABELA_UCZESTNIK = "UCZESTNIK";
    private static final String TABELA_PRELEGENT = "PRELEGENT";
    private static final String TABELA_ORGANIZATOR = "ORGANIZATOR";
    private static final String KOLUMNA_ID_UZYTKOWNIK = "ID_UZYTKOWNIK";
    private static final String KOLUMNA_IMIE = "IMIE";
    private static final String KOLUMNA_NAZWISKO = "NAZWISKO";
    private static final String KOLUMNA_LOGIN = "LOGIN";
    private static final String KOLUMNA_HASLO = "HASLO";
    private static final String KOLUMNA_EMAIL = "EMAIL";
    private static final String KOLUMNA_MIEJSCOWOSC = "MIEJSCOWOSC";


    private static final String NOWY_UZYTKOWNIK = "INSERT INTO " + TABELA_UZYTKOWNIK
            + "(ID_UZYTKOWNIK, LOGIN, HASLO, IMIE, NAZWISKO, EMAIL, MIEJSCOWOSC) VALUES"
            + "(?, ?, ?, ?, ?, ?, ?)";

    private static final String NOWY_UCZESTNIK = "INSERT INTO " + TABELA_UCZESTNIK
            + "(ID_UZYTKOWNIK) VALUES"
            + "(?)";

    private static final String NOWY_ORGANIZATOR = "INSERT INTO " + TABELA_ORGANIZATOR
            + "(ID_UZYTKOWNIK) VALUES"
            + "(?)";

    private static final String NOWY_PRELEGENT = "INSERT INTO " + TABELA_PRELEGENT
            + "(ID_UZYTKOWNIK) VALUES"
            + "(?)";
    private static final String ZNAJDZ_IMIE = "SELECT " + KOLUMNA_IMIE + " FROM " + "UZYTKOWNIK WHERE " + KOLUMNA_LOGIN + "= ?";
    private static final String ZNAJDZ_NAZWISKO = "SELECT " + KOLUMNA_NAZWISKO + " FROM " + "UZYTKOWNIK WHERE " + KOLUMNA_LOGIN + "= ?";
    private static final String ZNAJDZ_LOGIN = "SELECT " + KOLUMNA_LOGIN + " FROM " + "UZYTKOWNIK WHERE " + KOLUMNA_LOGIN + "= ?";
    private static final String ZNAJDZ_HASLO = "SELECT " + KOLUMNA_HASLO + " FROM " + "UZYTKOWNIK WHERE " + KOLUMNA_LOGIN + "= ?";
    private static final String ZNAJDZ_MIEJSCOWOSC = "SELECT " + KOLUMNA_MIEJSCOWOSC + " FROM " + "UZYTKOWNIK WHERE " + KOLUMNA_LOGIN + "= ?";
    private static final String ZNAJDZ_EMAIL = "SELECT " + KOLUMNA_EMAIL + " FROM " + "UZYTKOWNIK WHERE " + KOLUMNA_LOGIN + "= ?";


    private Connection connection;

    private String login1;

    public void setLogin(String login) {
        this.login1 = login;
    }

    public String getLogin1() {
        return login1;
    }

    /***
     * lazy initialization - instance of this class won't be created, until
     * the first time other class calls the getInstance method
     */
    private static Server instance = new Server();

    /***
     *  private constructor, cannot be used outside this class
     */
    private Server() {
    }

    /***
     *
     * @return singleton - there is only one instance of this object
     */
    public static Server getInstance() {
        return instance;
    }

    public boolean open() {
        try {
            connection = DriverManager.getConnection(DB_CONNECTION, DB_USER, DB_PASSWORD);
            return true;
        } catch (SQLException e) {
            System.out.println("Couldn't connect to database: " + e.getMessage());
            return false;
        }
    }

    public void close() {
        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Couldn't close connection: " + e.getMessage());
        }
    }

    public int getCount(String table) {
        String sql = "SELECT COUNT(*) AS count FROM " + table;

        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {

            int count = 0;
            while (results.next()) {
                count = results.getInt(1);
            }

//            System.out.format("Count = %d\n", count);
            return count;
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
            return -1;
        }
    }

    private int getId(String table, String column) {
        String sql = "SELECT MAX(" + column + ") AS max FROM " + table;
        try (Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            int maxId = 0;
            if (result.next()) {
                maxId = result.getInt(1);
            }
            return maxId;
        } catch (SQLException e) {
            System.out.println("getID() error: " + e.getMessage());
            return -1;
        }
    }

    public void insertUser(String login, String haslo,
                           String imie, String nazwisko,
                           String email, String miejscowosc, TypUzytkownika typ) throws SQLException {

        try {
            PreparedStatement statement = connection.prepareStatement(NOWY_UZYTKOWNIK);

            int id = getId(TABELA_UZYTKOWNIK, KOLUMNA_ID_UZYTKOWNIK) + 1;

            statement.setInt(1, id);
            statement.setString(2, login);
            statement.setString(3, haslo);
            statement.setString(4, imie);
            statement.setString(5, nazwisko);
            statement.setString(6, email);
            statement.setString(7, miejscowosc);

            statement.executeUpdate();

            if (typ == TypUzytkownika.UCZESTNIK) {
                statement = connection.prepareStatement(NOWY_UCZESTNIK);
                statement.setInt(1, id);
            }
            if (typ == TypUzytkownika.ORGANIZATOR) {
                statement = connection.prepareStatement(NOWY_ORGANIZATOR);
                statement.setInt(1, id);
            }
            if (typ == TypUzytkownika.PRELEGENT) {
                statement = connection.prepareStatement(NOWY_PRELEGENT);
                statement.setInt(1, id);
            }

            statement.executeUpdate();
            System.out.println("Uzytkownik dodany.");

        } catch (SQLException e) {
            System.out.println("nie dodano uĹĽytkownika: " + e.getMessage());
        }

    }

    public int checkLoginPass(String login, String haslo) {

        int znaleziono = 2;

        try (Statement stmt = connection.createStatement();
             Statement stmt2 = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT LOGIN FROM UZYTKOWNIK");
             ResultSet rs2 = stmt2.executeQuery("SELECT HASLO FROM UZYTKOWNIK")) {

            while (rs.next() && rs2.next()) {

                if (login.equals(rs.getString(1)) && (haslo.equals(rs2.getString(1)))) {
                    znaleziono = 1;
                    setLogin(rs.getString(1));

                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return znaleziono;
    }

    public String userName() {
        String wartosc = "null";
        String LOG = getLogin1();

        PreparedStatement st1;
        try {
            st1 = connection.prepareStatement(ZNAJDZ_IMIE);
            // st1.setString(1,name);
            st1.setString(1, LOG);
            ResultSet rs = st1.executeQuery();

            while (rs.next()) {

                wartosc = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wartosc;

    }

    public String userSurname() {
        String wartosc = "null";
        String LOG = getLogin1();

        PreparedStatement st1;
        try {
            st1 = connection.prepareStatement(ZNAJDZ_NAZWISKO);
            // st1.setString(1,name);
            st1.setString(1, LOG);
            ResultSet rs = st1.executeQuery();

            while (rs.next()) {

                wartosc = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wartosc;

    }

    public String userCountry() {
        String wartosc = "null";
        String LOG = getLogin1();
        String name = "IMIE";
        PreparedStatement st1;
        try {
            st1 = connection.prepareStatement(ZNAJDZ_MIEJSCOWOSC);
            // st1.setString(1,name);
            st1.setString(1, LOG);
            ResultSet rs = st1.executeQuery();

            while (rs.next()) {

                wartosc = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wartosc;

    }

    public String userPass() {
        String wartosc = "null";
        String LOG = getLogin1();
        String name = "IMIE";
        PreparedStatement st1;
        try {
            st1 = connection.prepareStatement(ZNAJDZ_HASLO);
            // st1.setString(1,name);
            st1.setString(1, LOG);
            ResultSet rs = st1.executeQuery();

            while (rs.next()) {

                wartosc = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wartosc;

    }

    public String userEmail() {
        String wartosc = "null";
        String LOG = getLogin1();
        String name = "IMIE";
        PreparedStatement st1;
        try {
            st1 = connection.prepareStatement(ZNAJDZ_EMAIL);
            // st1.setString(1,name);
            st1.setString(1, LOG);
            ResultSet rs = st1.executeQuery();

            while (rs.next()) {

                wartosc = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wartosc;

    }

    public String userLogin() {
        String wartosc = "null";
        String LOG = getLogin1();
        String name = "IMIE";
        PreparedStatement st1;
        try {
            st1 = connection.prepareStatement(ZNAJDZ_LOGIN);
            // st1.setString(1,name);
            st1.setString(1, LOG);
            ResultSet rs = st1.executeQuery();

            while (rs.next()) {

                wartosc = rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return wartosc;

    }


}

