package testOperations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
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
import org.mockito.MockitoAnnotations;

import dataAccess.DataAccess;
import domain.Driver;
import domain.Reservation;
import domain.Traveler;
import domain.Transaction;

public class returnMoneyTravelerMockBlackTest {

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
        persistenceMock = mockStatic(Persistence.class);
        persistenceMock.when(() -> Persistence.createEntityManagerFactory(any()))
            .thenReturn(entityManagerFactory);

        when(entityManagerFactory.createEntityManager()).thenReturn(db);
        when(db.getTransaction()).thenReturn(et);

        sut = new DataAccess(db);
    }

    @After
    public void tearDown() {
        persistenceMock.close();
    }

    
    @Test
    //resList is not null, email is not null, Driver and Traveler are both in database, resList.size is >0
    public void test1() {
        String driverEmail = "driver1@gmail.com";
        String travelerEmail = "traveler1@gmail.com";
        
        // Create domain objects
        Driver driver = new Driver(driverEmail, "Driver Name", "123");
        driver.setMoney(100.0f);
        
        Traveler traveler = new Traveler(travelerEmail, "Traveler Name", "456");
        traveler.setMoney(50.0f);
        
        Reservation reservation = new Reservation();
        reservation.setPayed(true);
        reservation.setCost(20.0f);
        reservation.setTraveler(traveler);
        reservation.setDriver(driver);
        
        List<Reservation> resList = new ArrayList<>();
        resList.add(reservation);
        
        try {
            // Configure mocks
            when(db.find(Driver.class, driverEmail)).thenReturn(driver);
            when(db.find(Traveler.class, travelerEmail)).thenReturn(traveler);
            
            // Invoke the method under test
            sut.open();
            sut.returnMoneyTravelers(resList, driverEmail);
            sut.close();
            
            // Verify that traveler received money back
            assertEquals(70.0f, traveler.getMoney(), 0.01f);
            // Verify that driver lost money
            assertEquals(80.0f, driver.getMoney(), 0.01f);
            
            // Verify persistence operations
            verify(db, times(1)).persist(traveler);
            verify(db, times(1)).persist(driver);
            verify(db, times(1)).persist(any(Transaction.class));
            
        } catch (Exception e) {
            fail("Test should not throw exception: " + e.getMessage());
        }
    }

   
    @Test
    //resList is null
    public void test2() {
        String driverEmail = "driver1@gmail.com";
        List<Reservation> resList = null;
        
        try {
            // Invoke the method under test with null resList
            sut.open();
            sut.returnMoneyTravelers(resList, driverEmail);
            sut.close();
            
            // The method should handle null gracefully and not throw exception
            // Verify that no persistence operations were called
            verify(db, never()).persist(any());
            
        } catch (NullPointerException e) {
            // Expected behavior - the method catches NullPointerException
            verify(et, times(1)).commit();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

    
    @Test
    //email is null
    public void test3() {
        String driverEmail = null;
        String travelerEmail = "traveler1@gmail.com";
        
        Traveler traveler = new Traveler(travelerEmail, "Traveler Name", "456");
        traveler.setMoney(50.0f);
        
        Reservation reservation = new Reservation();
        reservation.setPayed(true);
        reservation.setCost(20.0f);
        reservation.setTraveler(traveler);
        
        List<Reservation> resList = new ArrayList<>();
        resList.add(reservation);
        
        try {
            // Configure mocks - driver email is null so find returns null
            when(db.find(Driver.class, driverEmail)).thenReturn(null);
            
            // Invoke the method under test with null email
            sut.open();
            sut.returnMoneyTravelers(resList, driverEmail);
            sut.close();
            
            // The method should handle null email gracefully
            // Verify that no persistence operations were called due to null driver
            verify(db, never()).persist(any());
            
        } catch (NullPointerException e) {
            // Expected behavior - the method catches NullPointerException
            verify(et, times(1)).commit();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
    @Test
    //Driver not in database
    public void test4() {
        String driverEmail = "nonexistent@gmail.com";
        String travelerEmail = "traveler1@gmail.com";
        
        Traveler traveler = new Traveler(travelerEmail, "Traveler Name", "456");
        traveler.setMoney(50.0f);
        
        Reservation reservation = new Reservation();
        reservation.setPayed(true);
        reservation.setCost(20.0f);
        reservation.setTraveler(traveler);
        
        List<Reservation> resList = new ArrayList<>();
        resList.add(reservation);
        
        try {
            // Configure mocks - driver not found in database
            when(db.find(Driver.class, driverEmail)).thenReturn(null);
            
            // Invoke the method under test
            sut.open();
            sut.returnMoneyTravelers(resList, driverEmail);
            sut.close();
            
            // The method should handle missing driver gracefully
            // Since driver is null, the loop will try to access null.addTransaction() 
            // which should cause NullPointerException to be caught
            verify(db, never()).persist(any());
            
        } catch (NullPointerException e) {
            // Expected behavior - the method catches NullPointerException
            verify(et, times(1)).commit();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
    @Test
    //Traveler not in database
    public void test5() {
        String driverEmail = "driver1@gmail.com";
        String travelerEmail = "nonexistent@gmail.com";
        
        Driver driver = new Driver(driverEmail, "Driver Name", "123");
        driver.setMoney(100.0f);
        
        // Create a traveler object but don't put it in the database
        Traveler traveler = new Traveler(travelerEmail, "Traveler Name", "456");
        traveler.setMoney(50.0f);
        
        Reservation reservation = new Reservation();
        reservation.setPayed(true);
        reservation.setCost(20.0f);
        reservation.setTraveler(traveler);
        reservation.setDriver(driver);
        
        List<Reservation> resList = new ArrayList<>();
        resList.add(reservation);
        
        try {
            // Configure mocks - driver exists, but traveler not found in database
            when(db.find(Driver.class, driverEmail)).thenReturn(driver);
            when(db.find(Traveler.class, travelerEmail)).thenReturn(null);
            
            // Invoke the method under test
            sut.open();
            sut.returnMoneyTravelers(resList, driverEmail);
            sut.close();
            
            // The method should handle missing traveler gracefully
            // Since traveler is null from database, it should cause NullPointerException
            verify(db, never()).persist(any());
            
        } catch (NullPointerException e) {
            // Expected behavior - the method catches NullPointerException
            verify(et, times(1)).commit();
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }
    
    @Test
    //resList size = 0
    public void test6() {
        String driverEmail = "driver1@gmail.com";
        
        Driver driver = new Driver(driverEmail, "Driver Name", "123");
        driver.setMoney(100.0f);
        
        // Create empty reservation list
        List<Reservation> resList = new ArrayList<>();
        
        try {
            // Configure mocks
            when(db.find(Driver.class, driverEmail)).thenReturn(driver);
            
            // Invoke the method under test with empty list
            sut.open();
            sut.returnMoneyTravelers(resList, driverEmail);
            sut.close();
            
            // With empty list, the for loop should not execute
            // No persistence operations should occur
            verify(db, never()).persist(any());
            
            // Driver money should remain unchanged
            assertEquals(100.0f, driver.getMoney(), 0.01f);
            
        } catch (Exception e) {
            fail("Test should not throw exception with empty list: " + e.getMessage());
        }
    }
}
