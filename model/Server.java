package model;


import java.sql.*;

public class Server {

    private static final String DB_CONNECTION = "jdbc:oracle:thin:@localhost:1521:orcl";
    private static final String DB_USER = "TO_PROJEKT";
    private static final String DB_PASSWORD = "projekt";

    private static final String TABELA_UZYTKOWNIK = "UZYTKOWNIK";
    private static final String TABELA_UCZESTNIK = "UCZESTNIK";
    private static final String TABELA_PRELEGENT = "PRELEGENT";
    private static final String TABELA_ORGANIZATOR = "ORGANIZATOR";
    private static final String KOLUMNA_ID_UZYTKOWNIK = "ID_UZYTKOWNIK";
    private static final String KOLUMNA_ID_UCZESTNIK = "ID_UCZESTNIK";
    private static final String KOLUMNA_ID_ORGANIZATOR = "ID_ORGANIZATOR";
    private static final String KOLUMNA_ID_PRELEGENT = "ID_PRELEGENT";
    private static final String KOLUMNA_LOGIN_UZYTKOWNIK = "LOGIN";
    private static final String KOLUMNA_HASLO_UZYTKOWNIKA = "HASLO";


    private static final String NOWY_UZYTKOWNIK = "INSERT INTO " + TABELA_UZYTKOWNIK
            + "(ID_UZYTKOWNIK, LOGIN, HASLO, IMIE, NAZWISKO, EMAIL, MIEJSCOWOSC) VALUES"
            + "(?, ?, ?, ?, ?, ?, ?)";

    private static final String NOWY_UCZESTNIK = "INSERT INTO " + TABELA_UCZESTNIK
            + "(ID_UCZESTNIK) VALUES"
            + "(?)";

    private static final String NOWY_ORGANIZATOR = "INSERT INTO " + TABELA_ORGANIZATOR
            + "(ID_ORGANIZATOR) VALUES"
            + "(?)";

    private static final String NOWY_PRELEGENT = "INSERT INTO " + TABELA_PRELEGENT
            + "(ID_PRELEGENT) VALUES"
            + "(?)";

    private static final String ZNAJDZ_UZYTKOWNIKA = "SELECT COUNT(1) FROM "
            + TABELA_UZYTKOWNIK + " WHERE " + KOLUMNA_LOGIN_UZYTKOWNIK + " = ?";

    private static final String SPRAWDZ_HASLO = "SELECT " + KOLUMNA_HASLO_UZYTKOWNIKA + " FROM "
            + TABELA_UZYTKOWNIK + " WHERE " + KOLUMNA_LOGIN_UZYTKOWNIK + " = ?";

    private Connection connection;

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

    /**
     * close connection to db
     */
    public void close() {

        try {
            if (connection != null) {
                connection.close();
            }
        } catch (SQLException e) {
            System.out.println("Couldn't close connection: " + e.getMessage());
        }
    }

    /**
     * @param table from db
     * @return number of rows in table
     */
    private int getCount(String table) {

        String sql = "SELECT COUNT(*) AS count FROM " + table;

        try (Statement statement = connection.createStatement();
             ResultSet results = statement.executeQuery(sql)) {

            int count = 0;
            while (results.next()) {
                count = results.getInt(1);
            }
            return count;
        } catch (SQLException e) {
            System.out.println("Query failed: " + e.getMessage());
            return -1;
        }
    }

    /**
     * @param table  from db
     * @param column ID column
     * @return new value used as an ID of new record in db
     */
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

    /**
     * @param login check if this login already is used in db
     * @return true if login is free
     */
    private boolean isLoginFree(String login) {

        try {
            PreparedStatement statement = connection.prepareStatement(ZNAJDZ_UZYTKOWNIKA);
            statement.setString(1, login);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            return resultSet.getInt(1) != 1;

        } catch (SQLException e) {
            System.out.println("isLoginFree error: " + e.getMessage());
            return false;
        }
    }

    /**
     * create new user of declared type if given login is free
     */

    public boolean insertUser(String login, String haslo,
                              String imie, String nazwisko,
                              String email, String miejscowosc, TypUzytkownika typ) throws SQLException {

        if (isLoginFree(login)) {
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
                return true;

            } catch (SQLException e) {
                System.out.println("inserUser: nie dodano użytkownika: " + e.getMessage());
                return false;
            }
        }
        System.out.println("insertUser: nie dodano uzytkownika");
        return false;

    }

    /**
     * @return true if user with given password exists in db
     */
    public boolean logIn(String login, String haslo) {

        try {
            PreparedStatement statement = connection.prepareStatement(SPRAWDZ_HASLO);
            statement.setString(1, login);
            ResultSet resultSet = statement.executeQuery();
            resultSet.next();
            if (resultSet.getString(1).equals(haslo)) {
                System.out.println("Logowanie: haslo poprawne");
                return true;
            }
            System.out.println("Logowanie: niepoprawne hasło.");
            return false;
        } catch (SQLException e) {
            System.out.println("Logowanie error: " + e.getMessage());
            return false;
        }
    }

    //TODO usuwanie uztkownika z tabeli uzytkownik i tabeli uczestnik/prganizator/prelegent

    //TODO edycja danych: miejscowosc, haslo, email


}