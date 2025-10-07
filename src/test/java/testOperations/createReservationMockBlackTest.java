package testOperations;
import static org.junit.Assert.*;

import java.util.Date;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import dataAccess.DataAccess;
import domain.Car;
import domain.Driver;
import domain.Reservation;
import domain.Ride;
import domain.Traveler;
import exceptions.NotEnoughAvailableSeatsException;
import exceptions.ReservationAlreadyExistException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

public class createReservationMockBlackTest {

    private DataAccess sut;
    private EntityManager db;

    private Driver driver;
    private Traveler traveler;
    private Ride ride;
    private Car car;

    @Before
    public void setUp() {
        // Mock DB
        db = Mockito.mock(EntityManager.class);

        // Initialize SUT
        sut = new DataAccess(db);

        // Common domain objects
        driver = new Driver("driver@gmail.com", "Driver", "pass");
        traveler = new Traveler("traveler@gmail.com", "Traveler", "pass");
        car = new Car("ABC-123", 5, driver, false);

        // Ride with enough seats
        ride = driver.addRide("Bilbao", "Donostia", new Date(), 10.0f, car);
        ride.setRideNumber(333);

        // Mock transaction
        EntityTransaction mockTransaction = Mockito.mock(EntityTransaction.class);
        Mockito.when(db.getTransaction()).thenReturn(mockTransaction);
        
        // Mock transaction methods to do nothing (like white box test)
        Mockito.doNothing().when(mockTransaction).begin();
        Mockito.doNothing().when(mockTransaction).commit();
        Mockito.doNothing().when(db).persist(Mockito.any());
    }

    // -------- Test case 1: Ride or Traveler does not exist ----------
    @Test
    public void testCase1_NullRideOrTraveler() {
        // DB returns null for ride (use 333 to match other tests)
        Mockito.when(db.find(Ride.class, 333)).thenReturn(null);
        Mockito.when(db.find(Traveler.class, "traveler@gmail.com")).thenReturn(traveler);

        try {
            Reservation res = sut.createReservation(1, 333, "traveler@gmail.com");
            assertNull(res);
        } catch (Exception e) {
            fail("No exception expected: " + e);
        }

        // DB returns null for traveler
        Mockito.when(db.find(Ride.class, 333)).thenReturn(ride);
        Mockito.when(db.find(Traveler.class, "traveler@gmail.com")).thenReturn(null);

        try {
            Reservation res = sut.createReservation(1, 333, "traveler@gmail.com");
            assertNull(res);
        } catch (Exception e) {
            fail("No exception expected: " + e);
        }
    }

    // -------- Test case 2: Not enough available seats ----------
    @Test(expected = NotEnoughAvailableSeatsException.class)
    public void testCase2_NotEnoughSeats() throws Exception {
        // Create a ride with limited seats (only 1 seat available)
        Car smallCar = new Car("ABC-123-SMALL", 1, driver, false);
        Ride smallRide = driver.addRide("Bilbao", "Donostia", new Date(), 10.0f, smallCar);
        smallRide.setRideNumber(333);
        
        Mockito.when(db.find(Ride.class, 333)).thenReturn(smallRide);
        Mockito.when(db.find(Traveler.class, "traveler@gmail.com")).thenReturn(traveler);

        // Request more seats than total available (2 > 1)
        sut.createReservation(2, 333, "traveler@gmail.com"); // hm > total seats
    }

    // -------- Test case 3: Reservation already exists ----------
    @Test(expected = ReservationAlreadyExistException.class)
    public void testCase3_ReservationAlreadyExists() throws Exception {
        Mockito.when(db.find(Ride.class, 333)).thenReturn(ride);
        Mockito.when(db.find(Traveler.class, "traveler@gmail.com")).thenReturn(traveler);
        Mockito.when(db.find(Driver.class, driver.getEmail())).thenReturn(driver);

        // Pre-create a reservation to simulate existing reservation
        Reservation existingRes = traveler.makeReservation(ride, 1);
        ride.addReservation(existingRes);
        driver.addReservation(existingRes);

        sut.createReservation(1, 333, "traveler@gmail.com"); // triggers ReservationAlreadyExistException
    }

    // -------- Test case 4: Normal reservation ----------
    @Test
    public void testCase4_NormalReservation() {
        Mockito.when(db.find(Ride.class, 333)).thenReturn(ride);
        Mockito.when(db.find(Traveler.class, "traveler@gmail.com")).thenReturn(traveler);
        Mockito.when(db.find(Driver.class, driver.getEmail())).thenReturn(driver);

        // Don't call sut.open() since we're using a mock EntityManager
        Reservation reservation = null;
        try {
            reservation = sut.createReservation(1, 333, "traveler@gmail.com");
        } catch (Exception e) {
            fail("No exception expected: " + e);
        }
        
        assertNotNull("Reservation should not be null", reservation);
        assertEquals(traveler, reservation.getTraveler());
        assertEquals(ride, reservation.getRide());
        assertEquals(1, reservation.getHmTravelers());
    }
}
