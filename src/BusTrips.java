import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class BusTrips {

    private static final String STOPS_FILE = "gtfs/stops.txt";
    private static final String TRIPS_FILE = "gtfs/trips.txt";
    private static final String STOP_TIMES_FILE = "gtfs/stop_times.txt";

    private static final String CALENDAR_FILE = "gtfs/calendar.txt";
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("HH:mm:ss");

    public static void main(String[] args) {
        System.out.println("BusTrips app!");

        try (Scanner scanner = new Scanner(System.in)) {
            while (true) {
                System.out.println("Usage: busTrips <station_id> <num_of_buses> <relative|absolute>");
                System.out.println("Enter command (or 'exit' to quit):");

                String command = scanner.nextLine();

                if (command.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting application...");
                    break;
                }

                processCommand(command);
            }

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void processCommand(String command) {
        if (!command.startsWith("busTrips")) {
            System.out.println("Invalid command. Usage: busTrips <station_id> <num_of_buses> <relative|absolute>");
            return;
        }

        try {
            String[] parts = command.split(" ");
            if (parts.length != 4) {
                System.out.println("Invalid command. Usage: busTrips <station_id> <num_of_buses> <relative|absolute>");
                return;
            }

            String stationId = parts[1].trim();
            try {
                Integer.parseInt(stationId);
            } catch (NumberFormatException e) {
                System.out.println("Invalid station ID. Please enter a valid integer.");
                return;
            }

            int numOfBuses;
            try {
                numOfBuses = Integer.parseInt(parts[2]);
                if (numOfBuses <= 0) {
                    System.out.println("Number of buses must be a positive integer.");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number of buses. Please enter a valid integer.");
                return;
            }

            String timeFormat = parts[3].trim().toLowerCase();
            if (!timeFormat.equals("relative") && !timeFormat.equals("absolute")) {
                System.out.println("Invalid time format. Use 'relative' or 'absolute'.");
                return;
            }

            if (numOfBuses > 3 && timeFormat.equals("relative")) {
                System.out.println("You can display a maximum of 3 buses for relative time.");
                return;
            }

            if (numOfBuses > 5 && timeFormat.equals("absolute")) {
                System.out.println("You can display a maximum of 5 buses for the absolute time.");
                return;
            }

            String stopName = getStopName(stationId);
            if (stopName == null) {
                System.out.println("No stop found for the given station ID.");
                return;
            }

            String stopId = getStopId(stationId);
            if (stopId == null) {
                System.out.println("No stop found for the given station ID.");
                return;
            }

            Map<String, List<Date>> stopTimes = getStopTimes(stopId, timeFormat);
            if (stopTimes.isEmpty()) {
                System.out.println("No upcoming buses found for the given station ID.");
                return;
            }

            Map<String, String> busLines = getBusLines(stopTimes);
            if (busLines.isEmpty()) {
                System.out.println("No bus lines found for the upcoming buses.");
                return;
            }

            System.out.println("Bus stop: " + stopName);
            Map<String, List<Date>> futureBusesByRoute = new HashMap<>();
            for (Map.Entry<String, List<Date>> entry : stopTimes.entrySet()) {
                String tripId = entry.getKey();
                String routeId = busLines.get(tripId);
                List<Date> arrivalTimes = entry.getValue();
                List<Date> futureArrivalTimes = new ArrayList<>();
                Calendar currentCalendar = Calendar.getInstance();
                for (Date arrivalTime : arrivalTimes) {
                    Calendar arrivalCalendar = Calendar.getInstance();
                    arrivalCalendar.setTime(arrivalTime);
                    if (arrivalCalendar.get(Calendar.HOUR_OF_DAY) > currentCalendar.get(Calendar.HOUR_OF_DAY) ||
                        (arrivalCalendar.get(Calendar.HOUR_OF_DAY) == currentCalendar.get(Calendar.HOUR_OF_DAY) &&
                            arrivalCalendar.get(Calendar.MINUTE) > currentCalendar.get(Calendar.MINUTE))) {
                        futureArrivalTimes.add(arrivalTime);
                    }
                }
                if (!futureArrivalTimes.isEmpty()) {
                    futureBusesByRoute.putIfAbsent(routeId, new ArrayList<>());
                    futureBusesByRoute.get(routeId).addAll(futureArrivalTimes);
                }
            }

            for (Map.Entry<String, List<Date>> entry : futureBusesByRoute.entrySet()) {
                String routeId = entry.getKey();
                List<Date> arrivalTimes = entry.getValue();
                Collections.sort(arrivalTimes);
                System.out.print(routeId + ": ");
                int count = 0;
                StringBuilder sb = new StringBuilder();
                for (Date arrivalTime : arrivalTimes) {
                    if (count >= numOfBuses) {
                        break;
                    }
                    if (timeFormat.equals("absolute")) {
                        String formattedTime = TIME_FORMAT.format(arrivalTime);
                        sb.append(formattedTime.substring(0, formattedTime.lastIndexOf(":")));
                    } else {
                        Calendar arrivalCalendar = Calendar.getInstance();
                        arrivalCalendar.setTime(arrivalTime);
                        long currentTimeMillis = System.currentTimeMillis();
                        Calendar currentCalendar = Calendar.getInstance();
                        currentCalendar.setTimeInMillis(currentTimeMillis);
                        long diffMinutes = (arrivalCalendar.get(Calendar.HOUR_OF_DAY) - currentCalendar.get(Calendar.HOUR_OF_DAY)) * 60 +
                            (arrivalCalendar.get(Calendar.MINUTE) - currentCalendar.get(Calendar.MINUTE));
                        sb.append(diffMinutes).append("min");
                    }
                    if (count < numOfBuses - 1) {
                        sb.append(", ");
                    }
                    count++;
                }
                System.out.println(sb.toString());
            }

        } catch (Exception e) {
            System.err.println("An error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getStopName(String stationId) throws IOException {
        try (FileReader reader = new FileReader(STOPS_FILE);
             CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
            for (CSVRecord record : csvParser) {
                if (record.get("\uFEFFstop_id").trim().equalsIgnoreCase(stationId)) {
                    return record.get("stop_name").trim();
                }
            }
        }
        return null;
    }

    public static Map<String, String> getBusLines(Map<String, List<Date>> stopTimes) throws IOException {
        Map<String, String> busLines = new HashMap<>();
        try (FileReader reader = new FileReader(TRIPS_FILE);
             CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
            for (CSVRecord record : csvParser) {
                String tripId = record.get("trip_id").trim();
                if (stopTimes.containsKey(tripId)) {
                    String routeId = record.get("\uFEFFroute_id").trim();
                    busLines.put(tripId, routeId);
                }
            }
        }
        return busLines;
    }

    public static Map<String, List<Date>> getStopTimes(String stationId, String timeFormat) throws IOException, ParseException {
        Map<String, List<Date>> stopTimes = new HashMap<>();
        Map<String, Boolean> serviceAvailability = getServiceAvailability();
        try (FileReader reader = new FileReader(STOP_TIMES_FILE);
             CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
            for (CSVRecord record : csvParser) {
                String stopId = record.get("stop_id").trim();
                if (stopId.equalsIgnoreCase(stationId)) {
                    String tripId = record.get("\uFEFFtrip_id").trim();
                    Date arrivalTime = TIME_FORMAT.parse(record.get("arrival_time").trim());
                    String serviceId = getServiceId(tripId);
                    if (serviceAvailability.containsKey(serviceId) && serviceAvailability.get(serviceId)) {
                        if (!stopTimes.containsKey(tripId)) {
                            stopTimes.put(tripId, new ArrayList<>());
                        }
                        stopTimes.get(tripId).add(arrivalTime);
                    }
                }
            }
        }
        return stopTimes;
    }

    public static String getServiceId(String tripId) throws IOException {
        try (FileReader reader = new FileReader(TRIPS_FILE);
             CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
            for (CSVRecord record : csvParser) {
                if (record.get("trip_id").trim().equalsIgnoreCase(tripId)) {
                    return record.get("service_id").trim();
                }
            }
        }
        return null;
    }

    public static Map<String, Boolean> getServiceAvailability() throws IOException {
        Map<String, Boolean> serviceAvailability = new HashMap<>();
        try (FileReader reader = new FileReader(CALENDAR_FILE);
             CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
            for (CSVRecord record : csvParser) {
                String serviceId = record.get("\uFEFFservice_id").trim();
                int monday = Integer.parseInt(record.get("monday").trim());
                int tuesday = Integer.parseInt(record.get("tuesday").trim());
                int wednesday = Integer.parseInt(record.get("wednesday").trim());
                int thursday = Integer.parseInt(record.get("thursday").trim());
                int friday = Integer.parseInt(record.get("friday").trim());
                int saturday = Integer.parseInt(record.get("saturday").trim());
                int sunday = Integer.parseInt(record.get("sunday").trim());
                boolean isServiceAvailable = monday == 1 || tuesday == 1 || wednesday == 1 || thursday == 1 || friday == 1 || saturday == 1 || sunday == 1;
                serviceAvailability.put(serviceId, isServiceAvailable);
            }
        }
        return serviceAvailability;
    }

    public static String getStopId(String stationId) throws IOException {
        try (FileReader reader = new FileReader(STOPS_FILE);
             CSVParser csvParser = CSVFormat.DEFAULT.withHeader().parse(reader)) {
            for (CSVRecord record : csvParser) {
                if (record.get("\uFEFFstop_id").trim().equalsIgnoreCase(stationId)) {
                    return record.get("\uFEFFstop_id").trim();
                }
            }
        }
        return null;
    }

}