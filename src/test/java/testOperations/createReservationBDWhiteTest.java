package testOperations;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dataAccess.DataAccess;
import domain.Reservation;
import domain.Ride;
import exceptions.CarAlreadyExistsException;
import exceptions.NotEnoughAvailableSeatsException;
import exceptions.ReservationAlreadyExistException;
import exceptions.UserAlreadyExistException;

public class createReservationBDWhiteTest {
    
    // sut: system under test
    static DataAccess sut = new DataAccess();
    
    // additional operations needed to execute the test
    static TestDataAccess testDA = new TestDataAccess();
    
    private Ride ride;

    @Before
    public void setUp() {
        // Clean up any existing test data first
        cleanupTestData();
        
        // Set up test data
        sut.open();
        
        try {
            // Create driver
            sut.createDriver("driver@gmail.com", "Driver", "pass");
            
            // Create traveler  
            sut.createTraveler("traveler@gmail.com", "Traveler", "pass");
            
        } catch (UserAlreadyExistException e) {
            // If users exist, that's fine for some tests
            System.out.println("Users already exist, continuing with test");
        } catch (Exception e) {
            fail("Setup failed: " + e.getMessage());
        }
        
        sut.close();
    }
    
    @After
    public void tearDown() {
        cleanupTestData();
    }
    
    private void cleanupTestData() {
        // Clean up test data
        testDA.open();
        
        try {
            if (testDA.existDriver("driver@gmail.com")) {
                testDA.removeDriver("driver@gmail.com");
            }
        } catch (Exception e) {
            System.out.println("Error cleaning up driver: " + e.getMessage());
        }
        
        testDA.close();
    }

    @Test
    // TRY-1(Catch): ride or traveler does not exist in database
    // Input: <1, 333, traveler@gmail.com> (non-existent ride)
    // Should return null
    public void test1() {
        sut.open();
        
        try {
            // Use non-existent ride number (match mock test's 333)
            Reservation reservation = sut.createReservation(1, 333, "traveler@gmail.com");
            
            // Should return null due to NPE handling when ride is not found
            assertNull("Reservation should be null when ride doesn't exist", reservation);
            
        } catch (Exception e) {
            fail("Should not throw exception, should return null: " + e);
        } finally {
            sut.close();
        }
    }

    @Test
    // TRY-1(try)-IF-2(T): r.getnPlaces() < hm
    // Input: <2, rideNumber, traveler@gmail.com> where ride has only 1 seat
    // Should throw NotEnoughAvailableSeatsException
    public void test2() {
        sut.open();
        
        try {
            // Add a car with 1 seat to the driver (use unique car plate) to match mock test
            String carPlate = "CAR-002-" + System.currentTimeMillis();
            try {
                sut.addCarToDriver("driver@gmail.com", carPlate, 1, false);
            } catch (CarAlreadyExistsException e) {
                // If car already exists, use a different plate
                carPlate = "CAR-002-ALT-" + System.currentTimeMillis();
                sut.addCarToDriver("driver@gmail.com", carPlate, 1, false);
            }
            
            // Create a ride with limited seats (1 seat) and future date
            Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // tomorrow
            ride = sut.createRide("Bilbao", "Donostia", futureDate, 10.0f, "driver@gmail.com", carPlate);
            
            if (ride == null) {
                fail("Failed to create ride - createRide returned null");
            }
            
            System.out.println("Created ride with number: " + ride.getRideNumber());
            
            // Request more seats than available (2 > 1) to match mock test hm=2
            assertThrows(NotEnoughAvailableSeatsException.class, () -> {
                sut.createReservation(2, ride.getRideNumber(), "traveler@gmail.com");
            });
            
        } catch (Exception e) {
            fail("Unexpected exception during setup: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
        } finally {
            sut.close();
        }
    }

    @Test
    // TRY-1(try)-IF-2(F)-IF-3(T): reservation already exists in database
    // Input: <1, rideNumber, traveler@gmail.com> with existing reservation
    // Should throw ReservationAlreadyExistException
    public void test3() {
        sut.open();
        
        try {
            // Add a car with 5 seats to the driver (use unique car plate)
            String carPlate = "CAR-003-" + System.currentTimeMillis();
            try {
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            } catch (CarAlreadyExistsException e) {
                // If car already exists, use a different plate
                carPlate = "CAR-003-ALT-" + System.currentTimeMillis();
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            }
            
            // Create a ride with enough seats and future date
            Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // tomorrow
            ride = sut.createRide("Bilbao", "Donostia", futureDate, 10.0f, "driver@gmail.com", carPlate);
            
            // Create first reservation
            Reservation firstRes = sut.createReservation(1, ride.getRideNumber(), "traveler@gmail.com");
            assertNotNull("First reservation should be created", firstRes);
            
            // Try to create the same reservation again
            assertThrows(ReservationAlreadyExistException.class, () -> {
                sut.createReservation(1, ride.getRideNumber(), "traveler@gmail.com");
            });
            
        } catch (ReservationAlreadyExistException e) {
            fail("First reservation should succeed: " + e);
        } catch (Exception e) {
            fail("Unexpected exception: " + e);
        } finally {
            sut.close();
        }
    }

    @Test
    // TRY-1(try)-IF-2(F)-IF-3(F): normal reservation creation
    // Input: <1, rideNumber, traveler@gmail.com> with no existing reservation
    // Should return valid Reservation object
    public void test4() {
        sut.open();
        
        Reservation reservation = null;
        try {
            // Add a car with 5 seats to the driver (use unique car plate)
            String carPlate = "CAR-004-" + System.currentTimeMillis();
            try {
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            } catch (CarAlreadyExistsException e) {
                // If car already exists, use a different plate
                carPlate = "CAR-004-ALT-" + System.currentTimeMillis();
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            }
            
            // Create a ride with enough seats and future date
            Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000); // tomorrow
            ride = sut.createRide("Bilbao", "Donostia", futureDate, 10.0f, "driver@gmail.com", carPlate);
            
            // Create reservation
            reservation = sut.createReservation(1, ride.getRideNumber(), "traveler@gmail.com");
            
        } catch (Exception e) {
            fail("No exception expected: " + e);
        } finally {
            sut.close();
        }
        
        // Verify reservation was created correctly
        assertNotNull("Reservation should not be null", reservation);
        assertEquals("Traveler email should match", "traveler@gmail.com", reservation.getTraveler().getEmail());
        assertEquals("Ride should match", ride.getRideNumber(), reservation.getRide().getRideNumber());
        assertEquals("Number of travelers should be 1", 1, reservation.getHmTravelers());
    }
}
