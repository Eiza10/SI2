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

public class createReservationBDBlackTest {
    
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

    // -------- Test case 1: Ride or Traveler does not exist ----------
    @Test
    public void testCase1_NullRideOrTraveler() {
        sut.open();
        
        try {
            // Test with non-existent ride number (use 333 to match others)
            Reservation res1 = sut.createReservation(1, 333, "traveler@gmail.com");
            assertNull("Reservation should be null when ride doesn't exist", res1);
            
            // Test with non-existent traveler email  
            // First create a valid ride
            String carPlate = "CAR-001-" + System.currentTimeMillis();
            try {
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            } catch (CarAlreadyExistsException e) {
                carPlate = "CAR-001-ALT-" + System.currentTimeMillis();
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            }
            
            Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            ride = sut.createRide("Bilbao", "Donostia", futureDate, 10.0f, "driver@gmail.com", carPlate);
            
            Reservation res2 = sut.createReservation(1, ride.getRideNumber(), "nonexistent@gmail.com");
            assertNull("Reservation should be null when traveler doesn't exist", res2);
            
        } catch (Exception e) {
            fail("No exception expected: " + e);
        } finally {
            sut.close();
        }
    }

    // -------- Test case 2: Not enough available seats ----------
    @Test(expected = NotEnoughAvailableSeatsException.class)
    public void testCase2_NotEnoughSeats() throws Exception {
        sut.open();
        
        try {
            // Create a car with limited seats (1 seat) to match other tests
            String carPlate = "CAR-002-" + System.currentTimeMillis();
            try {
                sut.addCarToDriver("driver@gmail.com", carPlate, 1, false);
            } catch (CarAlreadyExistsException e) {
                carPlate = "CAR-002-ALT-" + System.currentTimeMillis();
                sut.addCarToDriver("driver@gmail.com", carPlate, 1, false);
            }
            
            // Create a ride with 1 seat
            Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            ride = sut.createRide("Bilbao", "Donostia", futureDate, 10.0f, "driver@gmail.com", carPlate);
            
            // Try to reserve more seats than available (2 > 1)
            sut.createReservation(2, ride.getRideNumber(), "traveler@gmail.com");
            
        } finally {
            sut.close();
        }
    }

    // -------- Test case 3: Reservation already exists ----------
    @Test(expected = ReservationAlreadyExistException.class)
    public void testCase3_ReservationAlreadyExists() throws Exception {
        sut.open();
        
        try {
            // Create a car with enough seats
            String carPlate = "CAR-003-" + System.currentTimeMillis();
            try {
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            } catch (CarAlreadyExistsException e) {
                carPlate = "CAR-003-ALT-" + System.currentTimeMillis();
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            }
            
            // Create a ride
            Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            ride = sut.createRide("Bilbao", "Donostia", futureDate, 10.0f, "driver@gmail.com", carPlate);
            
            // Create first reservation - should succeed
            Reservation firstRes = sut.createReservation(1, ride.getRideNumber(), "traveler@gmail.com");
            assertNotNull("First reservation should be created", firstRes);
            
            // Try to create the same reservation again - should throw exception
            sut.createReservation(1, ride.getRideNumber(), "traveler@gmail.com");
            
        } finally {
            sut.close();
        }
    }

    // -------- Test case 4: Normal reservation ----------
    @Test
    public void testCase4_NormalReservation() {
        sut.open();
        
        Reservation reservation = null;
        try {
            // Create a car with enough seats
            String carPlate = "CAR-004-" + System.currentTimeMillis();
            try {
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            } catch (CarAlreadyExistsException e) {
                carPlate = "CAR-004-ALT-" + System.currentTimeMillis();
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            }
            
            // Create a ride
            Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            ride = sut.createRide("Bilbao", "Donostia", futureDate, 10.0f, "driver@gmail.com", carPlate);
            
            // Create reservation
            reservation = sut.createReservation(1, ride.getRideNumber(), "traveler@gmail.com");
            
        } catch (Exception e) {
            fail("No exception expected: " + e);
        } finally {
            sut.close();
        }
        
        // Verify reservation was created correctly (black box validation)
        assertNotNull("Reservation should not be null", reservation);
        assertNotNull("Reservation should have a traveler", reservation.getTraveler());
        assertNotNull("Reservation should have a ride", reservation.getRide());
        assertEquals("Traveler email should match", "traveler@gmail.com", reservation.getTraveler().getEmail());
        assertEquals("Ride should match", ride.getRideNumber(), reservation.getRide().getRideNumber());
        assertEquals("Number of travelers should be 1", 1, reservation.getHmTravelers());
        assertTrue("Reservation should have a positive reservation code", reservation.getReservationCode() > 0);
    }

    // -------- Test case 5: Invalid parameters ----------
    @Test
    public void testCase5_InvalidParameters() {
        sut.open();
        
        try {
            // Create a valid ride first
            String carPlate = "CAR-005-" + System.currentTimeMillis();
            try {
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            } catch (CarAlreadyExistsException e) {
                carPlate = "CAR-005-ALT-" + System.currentTimeMillis();
                sut.addCarToDriver("driver@gmail.com", carPlate, 5, false);
            }
            
            Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            ride = sut.createRide("Bilbao", "Donostia", futureDate, 10.0f, "driver@gmail.com", carPlate);
            
            // Test with null email - this should return null or throw exception
            Reservation res3 = sut.createReservation(1, ride.getRideNumber(), null);
            assertNull("Reservation should be null when email is null", res3);
            
        } catch (Exception e) {
            // Some invalid parameters might throw exceptions, which is acceptable
            System.out.println("Exception with invalid parameters: " + e.getMessage());
        } finally {
            sut.close();
        }
    }

    // -------- Test case 6: Edge case - Maximum seats ----------
    @Test
    public void testCase6_MaximumSeats() {
        sut.open();
        
        try {
            // Create a car with exactly 1 seat
            String carPlate = "CAR-006-" + System.currentTimeMillis();
            try {
                sut.addCarToDriver("driver@gmail.com", carPlate, 1, false);
            } catch (CarAlreadyExistsException e) {
                carPlate = "CAR-006-ALT-" + System.currentTimeMillis();
                sut.addCarToDriver("driver@gmail.com", carPlate, 1, false);
            }
            
            Date futureDate = new Date(System.currentTimeMillis() + 24 * 60 * 60 * 1000);
            ride = sut.createRide("Bilbao", "Donostia", futureDate, 10.0f, "driver@gmail.com", carPlate);
            
            // Reserve exactly the maximum available seats (1 seat)
            Reservation reservation = sut.createReservation(1, ride.getRideNumber(), "traveler@gmail.com");
            assertNotNull("Reservation should be created when requesting exactly available seats", reservation);
            assertEquals("Should reserve exactly 1 seat", 1, reservation.getHmTravelers());
            
        } catch (Exception e) {
            fail("No exception expected for valid edge case: " + e);
        } finally {
            sut.close();
        }
    }
}
