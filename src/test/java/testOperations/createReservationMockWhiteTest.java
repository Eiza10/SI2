package testOperations;

import static org.junit.Assert.*;

import java.util.Date;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.Persistence;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import dataAccess.DataAccess;
import domain.Car;
import domain.Driver;
import domain.Reservation;
import domain.Ride;
import domain.Traveler;
import exceptions.NotEnoughAvailableSeatsException;
import exceptions.ReservationAlreadyExistException;

public class createReservationMockWhiteTest {
	static DataAccess sut;

	protected MockedStatic<Persistence> persistenceMock;

	@Mock
	protected EntityManagerFactory entityManagerFactory;
	@Mock
	protected EntityManager db;
	@Mock
	protected EntityTransaction et;

	@Before
	public void init() {
		MockitoAnnotations.openMocks(this);
		persistenceMock = Mockito.mockStatic(Persistence.class);
		persistenceMock.when(() -> Persistence.createEntityManagerFactory(Mockito.any()))
				.thenReturn(entityManagerFactory);

		Mockito.doReturn(db).when(entityManagerFactory).createEntityManager();
		Mockito.doReturn(et).when(db).getTransaction();
		sut = new DataAccess(db);
	}

	@After
	public void tearDown() {
		persistenceMock.close();
	}

	@Test
	// TRY-1(Catch): ride, traveler edo driver ez dira existitzen datu-basean
	// Sarrera: <1, 333, traveler@gmail.com>
	// Null itzuli behar du
	public void test1() {
	    try {
	        // Arrange
	        String email = "traveler@gmail.com";
	        int hm = 1;
	        Integer rideNumber = 333;

	        EntityTransaction mockTransaction = Mockito.mock(EntityTransaction.class);
	        Mockito.when(db.getTransaction()).thenReturn(mockTransaction);

	        Mockito.when(db.find(Ride.class, rideNumber)).thenReturn(null);
	        Mockito.when(db.find(Traveler.class, email)).thenReturn(null);

	        // Act
	        sut.open();
	        Reservation reservation = sut.createReservation(hm, rideNumber, email);

	        // Assert
	        assertNull(reservation);

	    } catch (Exception e) {
	        fail("Should not throw exception: " + e);
	    } finally {
	        sut.close();
	    }
	}

	
	@Test
	// TRY-1(try)-IF-2(T): r.getnPlaces() < hm
	// Sarrera: <2, 333, traveler@gmail.com>
	// NotEnoughAvailableSeatsException jaurti behar du
	public void test2() {
	    // Arrange
	    String email = "traveler@gmail.com";
	    int hm = 2;
	    Integer rideNumber = 333;

	    // Mock transaction
	    EntityTransaction mockTransaction = Mockito.mock(EntityTransaction.class);
	    Mockito.when(db.getTransaction()).thenReturn(mockTransaction);

	    // Create real domain objects
	    Driver driver = new Driver("driver@gmail.com", "Driver", "1234");

	    // Car with only 1 seat to trigger NotEnoughAvailableSeatsException
	    Car car = new Car("ABC-123", 1, driver, false);

	    String from = "Bilbao";
	    String to = "Donostia";
	    Date date = new Date();
	    float price = 10.0f;

	    // Create ride using the driver's method
	    Ride ride = driver.addRide(from, to, date, price, car);
	    ride.setRideNumber(rideNumber);

	    // Mock DB returns
	    Mockito.when(db.find(Ride.class, rideNumber)).thenReturn(ride);

	    Traveler traveler = new Traveler(email, "Traveler", "pass");
	    Mockito.when(db.find(Traveler.class, email)).thenReturn(traveler);

	    sut.open();

	    // Act & Assert
	    assertThrows(NotEnoughAvailableSeatsException.class, () -> {
	        sut.createReservation(hm, rideNumber, email);
	    });

	    sut.close();
	}
	
	@Test
	// TRY-1(try)-IF-2(F)-IF-3(T): reservation jada existitzen da datu-basean
	// Sarrera: <1, 333, traveler@gmail.com>
	// ReservationAlreadyExistException jaurti behar du
	public void test3() {
	    // Arrange
	    String email = "traveler@gmail.com";
	    int hm = 1;
	    Integer rideNumber = 333;

	    // Mock transaction
	    EntityTransaction mockTransaction = Mockito.mock(EntityTransaction.class);
	    Mockito.when(db.getTransaction()).thenReturn(mockTransaction);

	    // Create domain objects
	    Driver driver = new Driver("driver@gmail.com", "Driver", "pass");
	    Car car = new Car("ABC-123", 5, driver, false); // enough seats → IF-2(F)
	    String from = "Bilbao";
	    String to = "Donostia";
	    Date date = new Date();
	    float price = 10.0f;

	    Ride ride = driver.addRide(from, to, date, price, car);
	    ride.setRideNumber(rideNumber);

	    Traveler traveler = new Traveler(email, "Traveler", "pass");

	    // Create a reservation so doesReservationExist is true
	    Reservation existingRes = traveler.makeReservation(ride, hm);
	    ride.addReservation(existingRes);
	    driver.addReservation(existingRes);

	    // Mock DB
	    Mockito.when(db.find(Ride.class, rideNumber)).thenReturn(ride);
	    Mockito.when(db.find(Traveler.class, email)).thenReturn(traveler);
	    Mockito.when(db.find(Driver.class, driver.getEmail())).thenReturn(driver);

	    sut.open();
	    try {
	        sut.createReservation(hm, rideNumber, email);
	        fail("Expected ReservationAlreadyExistException to be thrown");
	    } catch (ReservationAlreadyExistException e) {
	        // Exception thrown as expected — test passes
	    } catch (NotEnoughAvailableSeatsException e) {
			fail();
		} finally {
	        sut.close();
	    }
	}
	
	@Test
	// TRY-1(try)-IF-2(F)-IF-3(F): reservation ez da existitzen datu-basean
	// Sarrera: <1, 333, traveler@gmail.com>
	// Reservation itzuli behar du
	public void test4() {
	    // Arrange
	    String email = "traveler@gmail.com";
	    int hm = 1;
	    Integer rideNumber = 333;

	    // Mock transaction
	    EntityTransaction mockTransaction = Mockito.mock(EntityTransaction.class);
	    Mockito.when(db.getTransaction()).thenReturn(mockTransaction);

	    // Real domain objects
	    Driver driver = new Driver("driver@gmail.com", "Driver", "pass");
	    Car car = new Car("ABC-123", 5, driver, false); // enough seats
	    String from = "Bilbao";
	    String to = "Donostia";
	    Date date = new Date();
	    float price = 10.0f;

	    Ride ride = driver.addRide(from, to, date, price, car);
	    ride.setRideNumber(rideNumber);

	    Traveler traveler = new Traveler(email, "Traveler", "pass");

	    // Ensure ride has NO existing reservations → IF-3(F)
	    // (No need to add any reservations)

	    // Mock DB lookups to return real objects
	    Mockito.when(db.find(Ride.class, rideNumber)).thenReturn(ride);
	    Mockito.when(db.find(Traveler.class, email)).thenReturn(traveler);
	    Mockito.when(db.find(Driver.class, driver.getEmail())).thenReturn(driver);

	    sut.open();

	    // Act
	    Reservation reservation = null;
	    try {
	        reservation = sut.createReservation(hm, rideNumber, email);
	    } catch (Exception e) {
	        fail("No exception expected: " + e);
	    } finally {
	        sut.close();
	    }

	    // Assert
	    assertNotNull(reservation);
	    assertEquals(traveler, reservation.getTraveler());
	    assertEquals(ride, reservation.getRide());
	    assertEquals(hm, reservation.getHmTravelers());
	}


}
