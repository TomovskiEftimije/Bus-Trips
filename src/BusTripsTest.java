import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;


public class BusTripsTest {



    @Test
    public void testGetStopName_ValidStationId() throws IOException {
        String stationId = "7";
        String expectedStopName = "AL Masjid Al-nabawi (AL Baqe)";
        String actualStopName = BusTrips.getStopName(stationId);
        assertEquals(expectedStopName, actualStopName);
    }

    @Test
    public void testGetStopName_InvalidStationId() throws IOException {
        String stationId = "54321";
        String actualStopName = BusTrips.getStopName(stationId);
        assertNull(actualStopName);
    }

    @Test
    public void testGetBusLines_ValidStopTimes() throws IOException {
        Map<String, List<Date>> stopTimes = new HashMap<>();
        String routeId1 = "101";
        String routeId2 = "103";
        String tripId1 = "NORMAL_03_101_Return_22:10";
        String tripId2 = "NORMAL_03_103_Go_07:20";
        stopTimes.put(tripId1, new ArrayList<>(Arrays.asList(new Date(), new Date())));
        stopTimes.put(tripId2, new ArrayList<>(Arrays.asList(new Date(), new Date())));

        Map<String, String> busLines = BusTrips.getBusLines(stopTimes);
        assertEquals(2, busLines.size());
        assertTrue(busLines.containsKey(tripId1));
        assertEquals(routeId1, busLines.get(tripId1));
        assertTrue(busLines.containsKey(tripId2));
        assertEquals(routeId2, busLines.get(tripId2));
    }

    @Test
    public void testGetBusLines_EmptyStopTimes() throws IOException {
        Map<String, List<Date>> stopTimes = new HashMap<>();
        Map<String, String> busLines = BusTrips.getBusLines(stopTimes);
        assertTrue(busLines.isEmpty());
    }

    @Test
    public void testGetStopTimes_ValidStationIdAndTimeFormat() throws IOException, ParseException {
        Map<String, List<Date>> stopTimes = BusTrips.getStopTimes("5", "relative");
        assertEquals(48, stopTimes.size());
    }

    @Test
    public void testGetStopTimes_InvalidStationId() throws IOException, ParseException {
        String stationId = "54321";
        String timeFormat = "relative";
        Map<String, List<Date>> stopTimes = BusTrips.getStopTimes(stationId, timeFormat);
        assertTrue(stopTimes.isEmpty());
    }

    @Test
    public void testProcessCommand_Exit() {
        ByteArrayInputStream in = new ByteArrayInputStream("exit\n".getBytes());
        System.setIn(in);
        assertDoesNotThrow(() -> BusTrips.main(new String[]{}));
    }

    @Test
    public void testGetServiceId_ValidTripId() throws IOException {
        String tripId = "NORMAL_03_101_Return_22:10";
        String expectedServiceId = "1";
        String actualServiceId = BusTrips.getServiceId(tripId);
        assertEquals(expectedServiceId, actualServiceId);
    }

    @Test
    public void testGetServiceId_InvalidTripId() throws IOException {
        String tripId = "invalidTrip";
        String actualServiceId = BusTrips.getServiceId(tripId);
        assertNull(actualServiceId);
    }

    @Test
    public void testGetStopId_ValidStationId() throws IOException {
        String stationId = "7";
        String expectedStopId = "7";
        String actualStopId = BusTrips.getStopId(stationId);
        assertEquals(expectedStopId, actualStopId);
    }

    @Test
    public void testGetStopId_InvalidStationId() throws IOException {
        String stationId = "54321";
        String actualStopId = BusTrips.getStopId(stationId);
        assertNull(actualStopId);
    }
}
