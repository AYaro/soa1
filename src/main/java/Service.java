import models.Flat;
import models.Furnish;
import models.Transport;

import java.sql.SQLException;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;


public class Service {
    public static Flat makeFlatFromParams(Map<String, String[]> parameters)
            throws ParseException, NumberFormatException {
        String name = parameters.get("name")[0];
        Long x = Long.parseLong(parameters.get("coordinateX")[0]);
        Double y = Double.parseDouble(parameters.get("coordinateY")[0]);
        LocalDateTime creationDate = LocalDateTime.now();
        Integer area = Integer.parseInt(parameters.get("area")[0]);
        int numberOfRooms = Integer.parseInt(parameters.get("numberOfRooms")[0]);
        Integer height = Integer.parseInt(parameters.get("height")[0]);
        String furnishType = parameters.get("furnish")[0];
        String transportType = parameters.get("transport")[0];
        String houseName = parameters.get("houseName")[0];
        Integer year = Integer.parseInt(parameters.get("year")[0]);
        int numberOfLifts = Integer.parseInt(parameters.get("numberOfLifts")[0]);
        if (area <= 0 || year <= 0 || numberOfRooms <= 0 || height <= 0 || numberOfLifts <= 0 || x <= -484){
            throw new NumberFormatException();
        }
        return new Flat(name, x, y, creationDate, area, numberOfRooms,
                height, furnishType, transportType, houseName, year, numberOfLifts);
    }

    public static Flat updateFlatFromParams(Map<String, String[]> parameters, Flat flat)
            throws ParseException, NumberFormatException {
        if (parameters.get("name") != null) {
            flat.setName(parameters.get("name")[0]);
        }
        if (parameters.get("coordinateX") != null) {
            flat.setCoordinateX(Long.parseLong(parameters.get("coordinateX")[0]));
        }
        if (parameters.get("coordinateY") != null) {
            flat.setCoordinateY(parameters.get("coordinateY")[0].isEmpty() ? 0
                    : Double.parseDouble(parameters.get("coordinateY")[0]));
        }
        if (parameters.get("area") != null) {
            flat.setArea(Integer.parseInt(parameters.get("area")[0]));
        }
        if (parameters.get("numberOfRooms") != null) {
            flat.setNumberOfRooms(Integer.parseInt(parameters.get("numberOfRooms")[0]));
        }
        if (parameters.get("height") != null) {
            flat.setHeight(Integer.parseInt(parameters.get("height")[0]));
        }
        if (parameters.get("furnishType") != null) {
            flat.setFurnish(Furnish.getByName(parameters.get("furnishType")[0]));
        }
        if (parameters.get("transportType") != null) {
            flat.setTransport(Transport.getByName(parameters.get("transportType")[0]));
        }
        if (parameters.get("houseName") != null) {
            flat.setHouseName(parameters.get("houseName")[0]);
        }
        if (parameters.get("year") != null) {
            flat.setHouseYear(Integer.parseInt(parameters.get("year")[0]));
        }
        if (parameters.get("numberOfLifts") != null) {
            flat.setHouseYear(Integer.parseInt(parameters.get("numberOfLifts")[0]));
        }
        return flat;
    }


    public static Flat addFlat(Flat flat) throws SQLException {
        Store.addFlat(Objects.requireNonNull(Store.getConnection()), flat);
        return flat;
    }

    public static int countFlatsByHouse(String name, Long year, int numberOfLifts) throws SQLException {
        return Store.CountHouse(Store.getConnection(), name, year, numberOfLifts);
    }

    public static int countFlatsByTransport(String transport) throws SQLException {
        return Store.CountWhereMore(Store.getConnection(), transport);
    }

    public static void deleteOneByRoom(int numberOfRooms) throws SQLException {
        Store.DeleteOneByRoom(Store.getConnection(), numberOfRooms);
    }

    public static Flat getFlatById(long id) throws SQLException {
        return Store.getFlatById(Objects.requireNonNull(Store.getConnection()), id);
    }

    public static Flat updateFlat(long id, Flat flat) throws SQLException {
        flat.setId(id);
        flat = Store.updateFlat(Objects.requireNonNull(Store.getConnection()), flat);
        return flat;
    }

    public static void deleteFlat(long id) throws SQLException {
        Store.deleteFlat(Objects.requireNonNull(Store.getConnection()), id);
    }

    public static ArrayList<Flat> getAllFlat() throws SQLException {
        return Store.getAllFlats(Objects.requireNonNull(Store.getConnection()));
    }


    public static ArrayList<Flat> getFlat(String[] filterFields, String[] filterValues, String[] sortFields,
                                               int limit, int offset) throws SQLException {
        return Store.getFlats(Store.getConnection(), filterFields, filterValues, sortFields, offset, limit);
    }
}



