package testOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import domain.Driver;
import domain.Reservation;
import domain.Ride;
import domain.Traveler;
import domain.Transaction;
import exceptions.RideAlreadyExistException;
import exceptions.RideMustBeLaterThanTodayException;

public class returnMoneyTravelerMockWhiteTest {
	
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
	//Driver not in database - TRY(T): Testing the path where line 3 db.find(Driver.class, email) returns null
	//This causes NullPointerException at line 8 when trying to access d.addTransaction(trans)
	//Expected: catch(NullPointerException e) at line 13, then db.getTransaction().commit()
	public void test1() {
		String driverEmail = "nonexistent@gmail.com";
		String travelerEmail = "traveler@gmail.com";
		
		try {
			// Create test objects
			Traveler traveler = new Traveler(travelerEmail, "Traveler Name", "456");
			traveler.setMoney(50.0f);
			
			Reservation reservation = new Reservation();
			reservation.setPayed(true);
			reservation.setCost(20.0f);
			reservation.setTraveler(traveler);
			
			List<Reservation> resList = new ArrayList<>();
			resList.add(reservation);
			
			// Configure mocks: Driver not found (line 3 returns null)
			when(db.find(Driver.class, driverEmail)).thenReturn(null);
			when(db.find(Traveler.class, travelerEmail)).thenReturn(traveler);
			
			// Invoke the method under test
			sut.open();
			sut.returnMoneyTravelers(resList, driverEmail);
			sut.close();
			
			// Verify that the exception path was taken (line 13)
			verify(et, times(1)).commit();
			
		} catch (Exception e) {
			sut.close();
			fail("Should not throw exception, should handle internally: " + e.getMessage());
		}
	}
	
	@Test
	//ridesList is empty - Testing the path where FOR-1 loop doesn't execute
	//Expected: Lines 1,2,3 execute, but for loop body never executes
	//No persist operations should be called
	public void test2() {
		String driverEmail = "driver@gmail.com";
		
		try {
			// Create test objects
			Driver driver = new Driver(driverEmail, "Driver Name", "123");
			driver.setMoney(100.0f);
			
			// Empty reservation list - for loop won't execute
			List<Reservation> resList = new ArrayList<>();
			
			// Configure mocks
			when(db.find(Driver.class, driverEmail)).thenReturn(driver);
			
			// Invoke the method under test
			sut.open();
			sut.returnMoneyTravelers(resList, driverEmail);
			sut.close();
			
			// Verify no persist operations were called (for loop didn't execute)
			verify(db, never()).persist(any());
			verify(et, never()).commit(); // Exception path not taken
			
		} catch (Exception e) {
			sut.close();
			fail("Should not throw exception with empty list: " + e.getMessage());
		}
	}
	
	@Test
	//rides are not payed - Testing FOR-1(T), IF-1(F): Loop executes but if condition is false
	//Expected: Lines 1,2,3,for-1 execute, but if-1 condition fails, so lines 4-12 don't execute
	//No persist operations should be called for the reservation processing
	public void test3() {
		String driverEmail = "driver@gmail.com";
		String travelerEmail = "traveler@gmail.com";
		
		try {
			// Create test objects
			Driver driver = new Driver(driverEmail, "Driver Name", "123");
			driver.setMoney(100.0f);
			
			Traveler traveler = new Traveler(travelerEmail, "Traveler Name", "456");
			traveler.setMoney(50.0f);
			
			// Create unpaid reservation - if-1 condition will be false
			Reservation reservation = new Reservation();
			reservation.setPayed(false); // This makes if-1 condition false
			reservation.setCost(20.0f);
			reservation.setTraveler(traveler);
			
			List<Reservation> resList = new ArrayList<>();
			resList.add(reservation);
			
			// Configure mocks
			when(db.find(Driver.class, driverEmail)).thenReturn(driver);
			
			// Invoke the method under test
			sut.open();
			sut.returnMoneyTravelers(resList, driverEmail);
			sut.close();
			
			// Verify no persist operations were called (if condition was false)
			verify(db, never()).persist(any());
			verify(et, never()).commit(); // Exception path not taken
			
			// Verify money amounts didn't change (if block was skipped)
			assertEquals(100.0f, driver.getMoney(), 0.01f);
			assertEquals(50.0f, traveler.getMoney(), 0.01f);
			
		} catch (Exception e) {
			sut.close();
			fail("Should not throw exception: " + e.getMessage());
		}
	}
	
	@Test
	//Everything is well and transaction is commited - Testing FOR-1(T), IF-1(T): Complete successful path
	//Expected: Lines 1,2,3,for-1,if-1,4,5,6,7,8,9,10,11,12 all execute successfully
	//Money transfer occurs and all persist operations are called
	public void test4() {
		String driverEmail = "driver@gmail.com";
		String travelerEmail = "traveler@gmail.com";
		
		try {
			// Create test objects
			Driver driver = new Driver(driverEmail, "Driver Name", "123");
			driver.setMoney(100.0f);
			
			Traveler traveler = new Traveler(travelerEmail, "Traveler Name", "456");
			traveler.setMoney(50.0f);
			
			// Create paid reservation - if-1 condition will be true
			Reservation reservation = new Reservation();
			reservation.setPayed(true); // This makes if-1 condition true
			reservation.setCost(20.0f);
			reservation.setTraveler(traveler);
			reservation.setDriver(driver);
			
			List<Reservation> resList = new ArrayList<>();
			resList.add(reservation);
			
			// Configure mocks - both driver and traveler found
			when(db.find(Driver.class, driverEmail)).thenReturn(driver);
			when(db.find(Traveler.class, travelerEmail)).thenReturn(traveler);
			
			// Invoke the method under test
			sut.open();
			sut.returnMoneyTravelers(resList, driverEmail);
			sut.close();
			
			// Verify money transfer occurred (lines 5 and 9)
			assertEquals("Traveler should receive refund", 70.0f, traveler.getMoney(), 0.01f);
			assertEquals("Driver should lose money", 80.0f, driver.getMoney(), 0.01f);
			
			// Verify all persist operations were called (lines 10, 11, 12)
			verify(db, times(1)).persist(traveler); // line 10
			verify(db, times(1)).persist(driver);   // line 11
			verify(db, times(1)).persist(any(Transaction.class)); // line 12
			
			// Verify transaction was added to both (lines 7 and 8)
			assertTrue("Driver should have transaction", !driver.getTransactions().isEmpty());
			assertTrue("Traveler should have transaction", !traveler.getTransactions().isEmpty());
			
			// Verify exception path was not taken
			verify(et, never()).commit();
			
		} catch (Exception e) {
			sut.close();
			fail("Should not throw exception: " + e.getMessage());
		}
	}
	
	
	
}
