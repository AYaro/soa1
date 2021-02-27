import models.Flat;
import models.House;


import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

public class Store {

    static final String DB_URL = "jdbc:postgresql://pg:5432/studs";
    static final String USER = "s242297";
    static final String PASS = "dyv175";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("PostgreSQL JDBC Driver is not found. Include it in your library path ");
            e.printStackTrace();
            return null;
        }

        System.out.println("PostgreSQL JDBC Driver successfully loaded");
        Connection connection;
        try {
            connection = DriverManager.getConnection(DB_URL, USER, PASS);
        } catch (SQLException e) {
            System.err.println("Connection Failed");
            e.printStackTrace();
            return null;
        }

        if (connection != null) {
            System.out.println("You successfully connected to database now");
            DatabaseMetaData meta = connection.getMetaData();
            ResultSet res = meta.getTables(null, null, "flat", null);
            if (!res.next()) createTable(connection);
        } else {
            System.err.println("Failed to make connection to database");
        }
        return connection;
    }


    public static void createTable(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        String sql = "CREATE TABLE flat " +
                "(id                    SERIAL                      PRIMARY KEY," +
                " name                  TEXT                        NOT NULL, " +
                " coordinate_x          REAL                        NOT NULL, " +
                " coordinate_y          REAL                        NOT NULL, " +
                " creation_date         TIMESTAMP WITH TIME ZONE    NOT NULL, " +
                " area                  INT," +
                " number_of_rooms       INT," +
                " height                INT," +
                " furnish               VARCHAR(12)                 NOT NULL," +
                " transport             VARCHAR(12)                 NOT NULL," +
                " house_name            VARCHAR(200)," +
                " year                  INT," +
                " number_of_lifts       INT);";
        statement.executeUpdate(sql);
        statement.close();
    }



    public static Flat getFlatById(Connection connection, long id) throws SQLException {
        PreparedStatement select = connection.prepareStatement("SELECT * FROM flat WHERE id = ?;");
        select.setLong(1, id);
        ResultSet rs = select.executeQuery();
        Flat flat = null;
        if (rs.next()) {
            flat = makeFlatFromResultSet(rs);
        }
        select.close();
        rs.close();
        return flat;
    }


    private static Flat makeFlatFromResultSet(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String name = rs.getString("name");
        Long x = rs.getLong ("coordinate_x");
        Double y = rs.getDouble("coordinate_y");
        java.time.LocalDateTime creationDate = rs.getTimestamp("creation_date").toLocalDateTime();
        Integer area = rs.getInt("area");
        int numberOfRooms = rs.getInt("number_of_rooms");
        Integer height = rs.getInt("height");
        String furnish = rs.getString("furnish");
        String transport = rs.getString("transport");
        String houseName = rs.getString("house_name");
        Integer year = rs.getInt("year");
        int numberOfLifts = rs.getInt("number_of_lifts");
        return new Flat(id, name, x, y, creationDate, area, numberOfRooms,
                height, furnish, transport, houseName, year, numberOfLifts);
    }

    private static ArrayList<Flat> collectFlatsFromResultSet(ResultSet rs) throws SQLException {
        ArrayList<Flat> flats = new ArrayList<>();
        while (rs.next()) {
            flats.add(makeFlatFromResultSet(rs));
        }
        return flats;
    }

    public static ArrayList<Flat> getAllFlats(Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery( "SELECT * FROM flat ORDER BY id;");
        ArrayList<Flat> flats = collectFlatsFromResultSet(rs);
        rs.close();
        connection.close();
        return flats;
    }

    public static ArrayList<Flat> getAllFlatsWithLimit(Connection connection, int limit, int offset) throws SQLException {
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery( "SELECT * FROM flat ORDER BY id;");
        ArrayList<Flat> flats = collectFlatsFromResultSet(rs);
        rs.close();
        connection.close();
        return flats;
    }

    public static void deleteFlat(Connection connection, long id) throws SQLException {
        PreparedStatement delete = connection.prepareStatement("DELETE FROM flat WHERE id = ?;");
        delete.setLong(1, id);
        delete.executeUpdate();
        delete.close();
        connection.close();
    }

//    java.time.LocalDate creationDate = rs.getTimestamp("creation_date").toLocalDateTime().toLocalDate();
//    Integer area = rs.getInt("area");
//    int numberOfRooms = rs.getInt("number_of_rooms");
//    Integer height = rs.getInt("height");
//    String furnish = rs.getString("furnish");
//    String transport = rs.getString("transport");
//    String houseName = rs.getString("house_name");
//    Integer year = rs.getInt("year");
//    int numberOfLifts = rs.getInt("num_of_lifts");

    public static Flat addFlat(Connection connection, Flat flat) throws SQLException {
        PreparedStatement add = connection.prepareStatement
                ("INSERT INTO flat (name, coordinate_x, coordinate_y," +
                        "area, number_of_rooms, height, furnish, transport," +
                        "house_name, year, number_of_lifts, creation_date)" +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);");
        add.setString(1, flat.getName());
        add.setFloat(2, flat.getCoordinates().getX());
        add.setDouble(3, flat.getCoordinates().getY());
        add.setInt(4, flat.getArea());
        add.setInt(5, flat.getNumberOfRooms());
        add.setInt(6, flat.getHeight());
        add.setString(7, flat.getFurnish().toString());
        add.setString(8, flat.getTransport().toString());

        House house = flat.getHouse();
        add.setString(9, house.getName());
        add.setInt(10, house.getYear());
        add.setInt(11, house.getNumberOfLifts());

        add.setTimestamp(12, java.sql.Timestamp.valueOf(flat.getCreationDate()));
        add.executeUpdate();

        PreparedStatement select = connection.prepareStatement("SELECT id FROM flat WHERE creation_date = ?;");
        select.setTimestamp(1, java.sql.Timestamp.valueOf(flat.getCreationDate()));
        ResultSet rs = select.executeQuery();
        rs.next();
        flat.setId(rs.getLong("id"));
        select.close();
        rs.close();
        connection.close();

        System.out.println("got res id");

        return flat;
    }

    public static Flat updateFlat(Connection connection, Flat flat) throws SQLException {
        System.out.println("updaying");
        PreparedStatement update = connection.prepareStatement
                ("UPDATE flat SET name = ?, coordinate_x = ?, coordinate_y = ?, " +
                        "area = ?, number_of_rooms = ?, height = ?, furnish = ?, transport = ?, " +
                        "house_name = ?, year = ?, number_of_lifts = ? WHERE id = ?;");

        update.setString(1, flat.getName());
        update.setLong(2, flat.getCoordinates().getX());
        update.setDouble(3, flat.getCoordinates().getY());
        update.setInt(4, flat.getArea());
        update.setInt(5, flat.getNumberOfRooms());
        update.setInt(6, flat.getHeight());
        System.out.println(flat.getArea());
        System.out.println(flat.getHeight());
        System.out.println(flat.getFurnish().getName());
        update.setString(7, flat.getFurnish().getName());
        update.setString(8, flat.getTransport().getName());

        House house = flat.getHouse();
        update.setString(9, house.getName());
        update.setInt(10, house.getYear());
        update.setInt(11, house.getNumberOfLifts());

        update.setLong(12, flat.getId());

        System.out.println(update.toString());
        update.executeUpdate();
        update.close();

        PreparedStatement select = connection.prepareStatement("SELECT creation_date FROM flat WHERE id = ?;");
        select.setLong(1, flat.getId());
        ResultSet rs = select.executeQuery();
        rs.next();

        LocalDateTime creationDate = rs.getTimestamp(1).toLocalDateTime();
        flat.setCreationDate(creationDate);

        select.close();
        rs.close();
        connection.close();

        return flat;
    }

    private static final Map<String, String> FILTER_FIELDS = new HashMap<>();

    static {
        FILTER_FIELDS.put("id", "id");
        FILTER_FIELDS.put("name", "name");
        FILTER_FIELDS.put("coordinateX", "coordinate_x");
        FILTER_FIELDS.put("coordinateY", "coordinate_y");
        FILTER_FIELDS.put("creationDate", "creation_date");
        FILTER_FIELDS.put("area", "area");
        FILTER_FIELDS.put("numberOfRooms", "number_of_rooms");
        FILTER_FIELDS.put("height", "height");
        FILTER_FIELDS.put("furnish", "furnish");
        FILTER_FIELDS.put("transport", "transport");
        FILTER_FIELDS.put("houseName", "house_name");
        FILTER_FIELDS.put("year", "year");
        FILTER_FIELDS.put("numberOfLifts", "number_of_lifts");
    }

    public static ArrayList<Flat> getFlats(Connection connection, String[] filterFields, String[] filterValues,
                                             String[] sortFields, int offset, int limit) throws SQLException {
        List<String> filterFieldsList = Arrays.asList(filterFields);
        StringBuilder selectBuilder = new StringBuilder("SELECT * FROM flat ");
        if (!filterFieldsList.isEmpty()) {
            selectBuilder.append(" WHERE ");
            for (String field : filterFieldsList) {
                if (FILTER_FIELDS.get(field) != null) {
                    selectBuilder.append(FILTER_FIELDS.get(field));
                    selectBuilder.append(" = ? and ");
                }
            }
            selectBuilder.replace(selectBuilder.lastIndexOf("and"), selectBuilder.lastIndexOf("and") + 3, "");
        }

        if (sortFields.length != 0) {
            selectBuilder.append(" ORDER BY ");
            for (String field : sortFields) {
                selectBuilder.append(FILTER_FIELDS.get(field));
                selectBuilder.append(", ");
            }
            selectBuilder.deleteCharAt(selectBuilder.lastIndexOf(","));
        }

        if (limit != -1) {
            selectBuilder.append(" LIMIT ");
            selectBuilder.append(limit);
        }

        if (offset != -1) {
            selectBuilder.append(" OFFSET ");
            selectBuilder.append(offset);
        }

        selectBuilder.append(";");
        PreparedStatement preparedStatement = connection.prepareStatement(selectBuilder.toString());

        if (filterFieldsList.contains("id")) {
            preparedStatement.setLong(filterFieldsList.indexOf("id") + 1,
                    Long.parseLong(filterValues[filterFieldsList.indexOf("id")]));
        }
        if (filterFieldsList.contains("name")) {
            preparedStatement.setString(filterFieldsList.indexOf("name") + 1,
                    filterValues[filterFieldsList.indexOf("name")]);
        }
        if (filterFieldsList.contains("coordinateX")) {
            preparedStatement.setDouble(filterFieldsList.indexOf("coordinateX") + 1,
                    Double.parseDouble(filterValues[filterFieldsList.indexOf("coordinateX")]));
        }
        if (filterFieldsList.contains("coordinateY")) {
            preparedStatement.setDouble(filterFieldsList.indexOf("coordinateY") + 1,
                    Double.parseDouble(filterValues[filterFieldsList.indexOf("coordinateY")]));
        }
        if (filterFieldsList.contains("creationDate")) {
            preparedStatement.setTimestamp(filterFieldsList.indexOf("creationDate") + 1,
                    java.sql.Timestamp.valueOf(filterValues[filterFieldsList.indexOf("creationDate")]));
        }
        if (filterFieldsList.contains("area")) {
            preparedStatement.setInt(filterFieldsList.indexOf("area") + 1,
                    Integer.parseInt(filterValues[filterFieldsList.indexOf("area")]));
        }
        if (filterFieldsList.contains("numberOfRooms")) {
            preparedStatement.setInt(filterFieldsList.indexOf("numberOfRooms") + 1,
                    Integer.parseInt(filterValues[filterFieldsList.indexOf("numberOfRooms")]));
        }
        if (filterFieldsList.contains("height")) {
            System.out.println("got heigth");
            preparedStatement.setLong(filterFieldsList.indexOf("height") + 1,
                    Long.parseLong(filterValues[filterFieldsList.indexOf("height")]));
        }
        if (filterFieldsList.contains("furnish")) {
            preparedStatement.setString(filterFieldsList.indexOf("furnish") + 1,
                    filterValues[filterFieldsList.indexOf("furnish")]);
        }
        if (filterFieldsList.contains("transport")) {
            preparedStatement.setString(filterFieldsList.indexOf("transport") + 1,
                    filterValues[filterFieldsList.indexOf("transport")]);
        }
        if (filterFieldsList.contains("houseName")) {
            preparedStatement.setString(filterFieldsList.indexOf("houseName") + 1,
                    filterValues[filterFieldsList.indexOf("houseName")]);
        }
        if (filterFieldsList.contains("year")) {
            preparedStatement.setInt(filterFieldsList.indexOf("year") + 1,
                    Integer.parseInt(filterValues[filterFieldsList.indexOf("year")]));
        }
        if (filterFieldsList.contains("numOfLifts")) {
            preparedStatement.setString(filterFieldsList.indexOf("numOfLifts") + 1,
                    filterValues[filterFieldsList.indexOf("numOfLifts")]);
        }

        System.out.println("SQL:"+ selectBuilder.toString());

        ResultSet rs = preparedStatement.executeQuery();
        ArrayList<Flat> flats = collectFlatsFromResultSet(rs);
        rs.close();
        connection.close();
        return flats;
    }

}
