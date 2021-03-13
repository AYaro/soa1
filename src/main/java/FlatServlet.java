

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import models.Flat;
import models.Furnish;
import models.Transport;
import serializers.LocalDateTimeDeserializer;
import serializers.LocalDateTimeSerializer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

import java.lang.reflect.Type;
import java.sql.SQLException;;
import java.text.ParseException;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class FlatServlet extends HttpServlet {
    private static final String SERVLET_PATH_FLATS = "/flats";
    private static final String SERVLET_PATH_COUNT_TRANSPORT = "/flats/countByTransport";
    private static final String SERVLET_PATH_COUNT_HOUSE = "/flats/countByHouse";
    private static final String SERVLET_PATH_DELETE_BY_ROOM = "/flats/deleteByRoom";
    private static final ArrayList<String> FLAT_FIELDS_GET =
            new ArrayList<>(Arrays.asList( "name", "coordinateX", "coordinateY", "numberOfRooms", "area", "height",
                    "furnish", "transport", "houseName", "numberOfLifts", "year","id","creationDate"));
    private static final ArrayList<String> FLAT_FIELDS_POST =
            new ArrayList<>(Arrays.asList( "name", "coordinateX", "coordinateY", "numberOfRooms", "area", "height",
                    "furnish", "transport", "houseName", "numberOfLifts", "year"));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer());
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();

        try {
            if (path.equals(SERVLET_PATH_FLATS)) {
                if (request.getParameterMap().size() == 0) {
                    ArrayList<Flat> flats = Service.getAllFlat();
                    if (flats.isEmpty()) {
                        writer.print("[]");
                        return;
                    }
                    String jsonString = gson.toJson(flats);
                    writer.print(jsonString);
                    return;
                } else {
                    if (checkParametersForFilterSort(request.getParameterMap())) {
                        int count = 0;
                        String offsetStr = request.getParameter("offset");
                        String limitStr = request.getParameter("limit");
                        int offset = (offsetStr == null || offsetStr.isEmpty()) ? -1 : Integer.parseInt(offsetStr);
                        int limit = (limitStr == null || limitStr.isEmpty()) ? -1 : Integer.parseInt(limitStr);
                        if (offset != -1) count++;
                        if (limit != -1) count++;
                        if (request.getParameter("sort") != null) count++;
                        String[] filterFields = new String[request.getParameterMap().size() - count];
                        String[] filterValues = new String[request.getParameterMap().size() - count];
                        int i = 0;
                        for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                            if (!entry.getKey().equals("offset") && !entry.getKey().equals("limit") && !entry.getKey().equals("sort")) {
                                filterFields[i] = entry.getKey();
                                filterValues[i] = entry.getValue()[0];
                                i++;
                            }
                        }

                        String sortFieldsStr = request.getParameter("sort");
                        String[] sortFields = (sortFieldsStr == null || sortFieldsStr.isEmpty()) ? new String[]{} :
                                sortFieldsStr.split(",");
                        ArrayList<Flat> flatsResult = Service.getFlat(filterFields, filterValues,
                                sortFields, limit, offset);
                        if (flatsResult.isEmpty()) {
                            writer.append("[]");
                            return;
                        } else {
                            writer.append(gson.toJson(flatsResult));
                            return;
                        }
                    } else {
                        response.sendError(422);
                        return;
                    }
                }
            } else if (checkUrl(path)) {
                if (request.getParameterMap().size() == 0) {
                    long id = Long.parseLong(path.substring(path.lastIndexOf(SERVLET_PATH_FLATS)
                            + SERVLET_PATH_FLATS.length() + 1));
                    Flat flat = Service.getFlatById(id);
                    if (flat == null) {
                        response.sendError(404);
                        return;
                    }
                    writer.append(gson.toJson(flat));
                    return;
                } else {
                    response.sendError(400);
                    return;
                }
            } else if (path.equals(SERVLET_PATH_COUNT_HOUSE)){
                if (request.getParameterMap().size() != 3) {
                    response.sendError(422);
                    return;
                }
                Long year = Long.parseLong(request.getParameter("year"));
                int numberOfLifts = Integer.parseInt(request.getParameter("numberOfLifts"));
                String name = request.getParameter("name");
                int count = Service.countFlatsByHouse(name, year, numberOfLifts);
                writer.append("{\"count\":" + count + "}");
            } else if (path.equals(SERVLET_PATH_COUNT_TRANSPORT)){
                if (request.getParameterMap().size() != 1) {
                    response.sendError(422);
                    return;
                }
                String transport = request.getParameter("transport");
                int count = Service.countFlatsByTransport(transport);
                writer.append("{\"count\":" + count + "}");
            } else if (path.equals(SERVLET_PATH_DELETE_BY_ROOM)) {
                response.sendError(405);
                return;
            }
            response.sendError(404);
        } catch (NumberFormatException e) {
            System.out.println(e.getMessage());
            response.sendError(422, e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            response.sendError(500, e.getMessage());
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        response.setContentType("application/json;charset=UTF-8");
        PrintWriter writer = response.getWriter();
        Gson gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").create();
        try {
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            Map<String, String> paramMap = gson.fromJson(body, type);
            if (path.equals(SERVLET_PATH_DELETE_BY_ROOM)){
                if (paramMap.size() != 1) {
                    response.sendError(422);
                }
                int numberOfRooms = Integer.parseInt(paramMap.get("numberOfRooms"));
                Service.deleteOneByRoom(numberOfRooms);
                writer.append("OK");
                return;
            } else if (path.equals(SERVLET_PATH_FLATS)) {
                Flat flat = Service.makeFlatFromParams(paramMap);
                flat = Service.addFlat(flat);
                String jsonString = gson.toJson(flat);
                writer.append(jsonString);
                return;
            } else if (path.equals(SERVLET_PATH_COUNT_HOUSE) || path.equals(SERVLET_PATH_COUNT_TRANSPORT))  {
                response.sendError(405);
                return;
            }
            response.sendError(404);
        } catch (NumberFormatException | ParseException | JsonSyntaxException e) {
            System.out.println(e.getMessage());
            response.sendError(422, e.getMessage());
        } catch (Exception e) {
            System.out.println(e.getMessage());
            response.sendError(500, e.getMessage());
        }
    }


    private static boolean checkParams(Map<String, String[]> params, Boolean isGet) {
        try {
            for (Map.Entry<String, String[]> param : params.entrySet()) {
                System.out.println(param.getKey());
                System.out.println(param.getValue()[0]);
                switch (param.getKey()) {
                    case "offset":
                    case "limit":
                    case "sort":
                        if (!isGet) return false;
                        break;
                    case "transport":
                        if (Transport.getByName(param.getValue()[0]) == null) return false;
                        break;
                    case "furnish":
                        if (Furnish.getByName(param.getValue()[0]) == null) return false;
                        break;
                    case "id":
                    case "numberOfRooms":
                    case "numberOfLifts":
                    case "area":
                        int anumber = Integer.parseInt(param.getValue()[0]);
                        if (anumber < 0) return false;
                        break;
                    case "year":
                    case "height":
                        long hnumber = Long.parseLong(param.getValue()[0]);
                        if (hnumber < 0) return false;
                        break;
                    case "salary":
                        double dnumber1 = Double.parseDouble(param.getValue()[0]);
                        if (dnumber1 < 0) return false;
                        break;
                    case "coordinateX":
                        long dnumber = Long.parseLong(param.getValue()[0]);
                        if ( dnumber > -484) return false;
                        break;
                    case "coordinateY":
                    case "creationDate":
                        break;
                    case "name":
                        break;
                    case "houseName":
                        if (param.getValue()[0] == null ||param.getValue()[0] == "") return false;
                        break;
                    default:
                        return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();

        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeSerializer());
        gsonBuilder.registerTypeAdapter(LocalDateTime.class, new LocalDateTimeDeserializer());
        Gson gson = gsonBuilder.setPrettyPrinting().create();
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
        Map<String, String> paramMap = gson.fromJson(body, type);

        if (checkUrl(path)) {
            response.setContentType("application/json;charset=UTF-8");
            PrintWriter writer = response.getWriter();

            try {
                if (hasRedundantParameters(paramMap.keySet()) ||
                        !hasAllRequiredParameters(paramMap.keySet()) ||
                        !validateFields(paramMap)) {
                    response.sendError(422);
                } else {
                    long id = Long.parseLong(path.substring(path.lastIndexOf(SERVLET_PATH_FLATS)
                            + SERVLET_PATH_FLATS.length() + 1));
                    Flat flat = Service.getFlatById(id);
                    if (flat != null) {
                        flat = Service.updateFlatFromParams(paramMap, flat);
                        flat = Service.updateFlat(id, flat);
                        if (flat.getCreationDate() != null) {
                            writer.append(gson.toJson(flat));
                        }
                    } else {
                        writer.append("[]");
                        return;
                    }
                }
            } catch (NumberFormatException | ParseException e) {
                System.out.println(e.getMessage());
                response.sendError(422, e.getMessage());
            } catch (Exception e) {
                System.out.println(e.getMessage());
                response.sendError(500, e.getMessage());
            }
        } else {
            response.sendError(400);
        }
    }

    private static boolean checkUrl(String url){
        Pattern p = Pattern.compile("^" + SERVLET_PATH_FLATS + "/[0-9]*$");
        Matcher m = p.matcher(url);
        return m.matches();
    }

    private static boolean validateFields(Map<String, String> params){
        try {
            boolean res = Long.parseLong(params.get("coordinateX")) > -484 &&
                    (params.get("height") == null || Long.parseLong(params.get("height")) > 0) &&
                    Integer.parseInt(params.get("numberOfRooms")) > 0 &&
                    Integer.parseInt(params.get("area")) > 0 &&
                    params.get("name") != null && !params.get("name").isEmpty() &&
                    Furnish.getByName(params.get("furnish")) != null &&
                    Transport.getByName(params.get("transport")) != null &&
                    (params.get("year")) != null;
                    Double.parseDouble(params.get("coordinateY"));
            return res;
        } catch (Exception e) {
            System.out.println(e);
            return false;
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo();
        if (checkUrl(path)) {
            if (request.getParameterMap().size() == 0) {
                try {
                    long id = Long.parseLong(path.substring(path.lastIndexOf(SERVLET_PATH_FLATS)
                            + SERVLET_PATH_FLATS.length() + 1));
                    if (Service.getFlatById(id) == null) response.sendError(404);
                    Service.deleteFlat(id);
                } catch (NumberFormatException e) {
                    System.out.println(e.getMessage());
                    response.sendError(422, e.getMessage());
                } catch (SQLException e) {
                    System.out.println(e.getMessage());
                    response.sendError(500, e.getMessage());
                }
            } else if (path.equals(SERVLET_PATH_COUNT_HOUSE) || path.equals(SERVLET_PATH_COUNT_TRANSPORT) || path.equals(SERVLET_PATH_DELETE_BY_ROOM))  {
                response.sendError(405);
            } else {
                response.sendError(400);
            }
        } else {
            response.sendError(400);
        }
    }

    private static boolean hasRedundantParameters(Set<String> params) {
        return params.stream().anyMatch(x -> FLAT_FIELDS_GET.stream()
                .noneMatch(x::equals));
    }

    private static boolean hasAllRequiredParameters(Set<String> params) {
        return FLAT_FIELDS_POST.stream().filter(params::contains).count() == FLAT_FIELDS_POST.size();
    }

    private static boolean hasRedundantFields(String fields) {
        return Arrays.stream(fields.split(","))
                .anyMatch(x -> FLAT_FIELDS_GET.stream()
                        .noneMatch(x::equals)) && !fields.isEmpty();
    }

    private static boolean checkParametersForFilterSort(Map<String, String[]> params) {
        try {
            return (params.get("offset") == null || Integer.parseInt(params.get("offset")[0]) >= 0) &&
                    (params.get("limit") == null || Integer.parseInt(params.get("limit")[0]) >= 0) &&
                    (params.get("sort") == null || !hasRedundantFields(params.get("sort")[0])) &
                    (checkParams(params, true));
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

