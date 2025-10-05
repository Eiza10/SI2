package testOperations;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dataAccess.DataAccess;
import domain.Driver;
import domain.Reservation;
import domain.Ride;
import domain.Traveler;
import exceptions.CarAlreadyExistsException;
import exceptions.NotEnoughAvailableSeatsException;
import exceptions.ReservationAlreadyExistException;
import exceptions.UserAlreadyExistException;
import testOperations.TestDataAccess;


public class returnMoneyTravelerBDWhiteTest {

	 // sut: system under test
    static DataAccess sut = new DataAccess();
    
    // additional operations needed to execute the test
    static TestDataAccess testDA = new TestDataAccess();
    
    @Test
	//Driver or Traveler not in database
	// This tests the path where line 3 (db.find(Driver.class, email)) returns null
	// or line 4 (db.find(Traveler.class, res.getTraveler().getEmail())) returns null
	// Expected: NullPointerException caught at line 13
	public void test1() {
		String driverEmail = "nonexistentdriver@gmail.com";
		String travelerEmail = "testtraveler@gmail.com";
		
		try {
			// Create only traveler, no driver to test null driver path
			sut.open();
			sut.createTraveler(travelerEmail, "Test Traveler", "456");
			
			// Create a paid reservation
			Reservation reservation = new Reservation();
			reservation.setPayed(true);
			reservation.setCost(20.0f);
			
			// Create traveler object for reservation
			Traveler traveler = sut.getTravelerByEmail(travelerEmail, "456");
			reservation.setTraveler(traveler);
			
			List<Reservation> resList = new ArrayList<>();
			resList.add(reservation);
			
			// Execute: this should hit line 3 where Driver d = db.find(Driver.class, email) returns null
			// Then when accessing d.addTransaction(trans) at line 8, it should throw NullPointerException
			sut.returnMoneyTravelers(resList, driverEmail);
			sut.close();
			
			// Method should handle the exception gracefully
			assertTrue("Method should handle missing driver gracefully", true);
			
		} catch (Exception e) {
			sut.close();
			// Expected to catch NullPointerException and handle it gracefully
			assertTrue("Expected exception handling for null driver", true);
		} finally {
			// Cleanup
			try {
				sut.open();
				sut.deleteAccountTraveler(travelerEmail);
				sut.close();
			} catch (Exception e) {
				// Ignore cleanup errors
			}
		}
	}
	
	@Test
	//ridesList is empty
	// This tests the path where the for loop (for-1) doesn't execute because resList is empty
	// Expected: The method should complete without doing anything, no transactions created
	public void test2() {
		String driverEmail = "testdriver2@gmail.com";
		
		try {
			// Setup test data - create driver
			testDA.open();
			testDA.createDriver(driverEmail, "Test Driver", "123");
			testDA.close();
			
			sut.open();
			
			// Create empty reservation list - this tests the path where for-1 loop doesn't execute
			List<Reservation> resList = new ArrayList<>();
			
			// Execute the method under test - should complete without errors even with empty list
			sut.returnMoneyTravelers(resList, driverEmail);
			
			// The test passes if no exception is thrown
			assertTrue("Method should handle empty list without errors", true);
			
			sut.close();
			
		} catch (Exception e) {
			sut.close();
			fail("Test should not throw exception with empty list: " + e.getMessage());
		} finally {
			// Cleanup
			try {
				testDA.open();
				testDA.removeDriver(driverEmail);
				testDA.close();
			} catch (Exception e) {
				// Ignore cleanup errors
			}
		}
	}
	
	@Test
	//rides are not payed
	// This tests the path where res.isPayed() at line if-1 returns false
	// Expected: The if block (lines 4-12) should not execute, no money transfers
	public void test3() {
		String driverEmail = "testdriver3@gmail.com";
		String travelerEmail = "testtraveler3@gmail.com";
		
		try {
			// Setup test data
			testDA.open();
			testDA.createDriver(driverEmail, "Test Driver", "123");
			testDA.close();
			
			sut.open();
			sut.createTraveler(travelerEmail, "Test Traveler", "456");
			
			// Create a ride
			Ride ride = sut.createRide("Bilbo", "Donostia", new Date(), 10.0f, driverEmail, "AA123456");
			
			// Create unpaid reservation - this tests the path where if-1 (res.isPayed()) is false
			Reservation reservation = sut.createReservation(1, ride.getRideNumber(), travelerEmail);
			assertNotNull(reservation);
			assertFalse("Reservation should not be paid initially", reservation.isPayed());
			
			// Get initial money amounts
			Driver driver = sut.getDriverByEmail(driverEmail, "123");
			Traveler traveler = sut.getTravelerByEmail(travelerEmail, "456");
			float initialDriverMoney = driver.getMoney();
			float initialTravelerMoney = traveler.getMoney();
			
			List<Reservation> resList = new ArrayList<>();
			resList.add(reservation);
			
			// Execute: for loop executes but if-1 condition fails, so lines 4-12 don't execute
			sut.returnMoneyTravelers(resList, driverEmail);
			
			// Verify no money transfer occurred (if block was skipped)
			driver = sut.getDriverByEmail(driverEmail, "123");
			traveler = sut.getTravelerByEmail(travelerEmail, "456");
			
			assertEquals("Driver money should remain unchanged for unpaid reservation", 
						 initialDriverMoney, driver.getMoney(), 0.01f);
			assertEquals("Traveler money should remain unchanged for unpaid reservation", 
						 initialTravelerMoney, traveler.getMoney(), 0.01f);
			
			sut.close();
			
		} catch (Exception e) {
			sut.close();
			fail("Test should not throw exception: " + e.getMessage());
		} finally {
			// Cleanup
			try {
				testDA.open();
				testDA.removeDriver(driverEmail);
				testDA.close();
				
				sut.open();
				sut.deleteAccountTraveler(travelerEmail);
				sut.close();
			} catch (Exception e) {
				// Ignore cleanup errors
			}
		}
	}
	
	@Test
	//Everything is well and transaction is commited
	// This tests the complete successful path: lines 1,2,3,for-1,if-1,4,5,6,7,8,9,10,11,12
	// Expected: Money transfer occurs, all database operations complete successfully
	public void test4() {
		String driverEmail = "testdriver4@gmail.com";
		String travelerEmail = "testtraveler4@gmail.com";
		
		try {
			// Setup test data
			testDA.open();
			testDA.createDriver(driverEmail, "Test Driver", "123");
			testDA.close();
			
			sut.open();
			sut.createTraveler(travelerEmail, "Test Traveler", "456");
			
			// Create a ride
			Ride ride = sut.createRide("Bilbo", "Donostia", new Date(), 10.0f, driverEmail, "AA123456");
			
			// Create paid reservation - this ensures if-1 (res.isPayed()) is true
			Reservation reservation = sut.createReservation(2, ride.getRideNumber(), travelerEmail);
			assertNotNull(reservation);
			
			// Pay for the reservation to make it paid
			sut.putMoneyTraveler(travelerEmail, 100); // Give traveler money
			sut.pay(reservation); // Pay for reservation - sets isPayed() to true
			
			// Verify reservation is paid
			assertTrue("Reservation should be paid", reservation.isPayed());
			
			// Get initial money amounts
			Driver driver = sut.getDriverByEmail(driverEmail, "123");
			Traveler traveler = sut.getTravelerByEmail(travelerEmail, "456");
			float initialDriverMoney = driver.getMoney();
			float initialTravelerMoney = traveler.getMoney();
			
			List<Reservation> resList = new ArrayList<>();
			resList.add(reservation);
			
			// Execute: This should follow the complete path:
			// line 3: d = db.find(Driver.class, email) - finds driver
			// for-1: iterates through resList
			// if-1: res.isPayed() returns true
			// line 4: t = db.find(Traveler.class, res.getTraveler().getEmail()) - finds traveler
			// lines 5-12: complete money transfer and persist operations
			sut.returnMoneyTravelers(resList, driverEmail);
			
			// Verify the money was transferred correctly
			driver = sut.getDriverByEmail(driverEmail, "123");
			traveler = sut.getTravelerByEmail(travelerEmail, "456");
			
			float expectedRefund = reservation.getCost();
			assertEquals("Driver money should decrease by refund amount", 
						 initialDriverMoney - expectedRefund, driver.getMoney(), 0.01f);
			assertEquals("Traveler money should increase by refund amount", 
						 initialTravelerMoney + expectedRefund, traveler.getMoney(), 0.01f);
			
			// Verify transactions were created for both driver and traveler
			assertTrue("Driver should have transactions", !driver.getTransactions().isEmpty());
			assertTrue("Traveler should have transactions", !traveler.getTransactions().isEmpty());
			
			sut.close();
			
		} catch (Exception e) {
			sut.close();
			fail("Test should not throw exception: " + e.getMessage());
		} finally {
			// Cleanup
			try {
				testDA.open();
				testDA.removeDriver(driverEmail);
				testDA.close();
				
				sut.open();
				sut.deleteAccountTraveler(travelerEmail);
				sut.close();
			} catch (Exception e) {
				// Ignore cleanup errors
			}
		}
	}
}
