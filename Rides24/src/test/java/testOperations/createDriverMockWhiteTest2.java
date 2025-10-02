package testOperations;

import static org.junit.Assert.*;
import org.junit.Test;
import org.mockito.Mockito;

import dataAccess.DataAccess;
import domain.Driver;
import exceptions.UserAlreadyExistException;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

// ============================================================================
// 1. WHITE BOX TESTING WITH MOCK
// Tests based on flow diagram paths and code structure
// ============================================================================
public class createDriverMockWhiteTest2 {

    /**
     * WHITE BOX PATH: TRY-1(F), IF-1(F)
     * Flow: 1 → 3-5 → 10-14 → END
     * Test successful driver creation when driver doesn't exist
     */
    @Test
    public void testPath_Try1False_If1False_DriverCreated() {
        System.out.println("TEST: White Box Mock - Path TRY-1(F), IF-1(F)");
        
        // Mock setup
        EntityManager mockDb = Mockito.mock(EntityManager.class);
        EntityTransaction mockTransaction = Mockito.mock(EntityTransaction.class);
        
        Mockito.when(mockDb.getTransaction()).thenReturn(mockTransaction);
        
        // Configure: driver does NOT exist (IF-1 will be FALSE)
        String email = "newdriver@gmail.com";
        Mockito.when(mockDb.find(Driver.class, email)).thenReturn(null);
        
        // Create DataAccess with mocked EntityManager
        DataAccess sut = new DataAccess(mockDb);
        
        // Execute
        Driver result = null;
        try {
            result = sut.createDriver(email, "New Driver", "pass123");
        } catch (UserAlreadyExistException e) {
            fail("Should not throw exception on this path");
        }
        
        // Verify: Lines 10-14 executed
        assertNotNull("Driver should be created", result);
        assertEquals("Email should match", email, result.getEmail());
        
        // Verify mock interactions (proves path was followed)
        Mockito.verify(mockTransaction, Mockito.times(1)).begin();
        Mockito.verify(mockDb, Mockito.times(1)).find(Driver.class, email);
        Mockito.verify(mockDb, Mockito.times(1)).persist(Mockito.any(Driver.class));
        Mockito.verify(mockTransaction, Mockito.times(1)).commit();
        
        System.out.println("✓ Path executed correctly: Lines 1→3-5→10-14→END");
    }
    
    /**
     * WHITE BOX PATH: TRY-1(F), IF-1(T)
     * Flow: 1 → 3-5 → 7-8 → END
     * Test exception thrown when driver already exists
     */
    @Test
    public void testPath_Try1False_If1True_ExceptionThrown() {
        System.out.println("TEST: White Box Mock - Path TRY-1(F), IF-1(T)");
        
        // Mock setup
        EntityManager mockDb = Mockito.mock(EntityManager.class);
        EntityTransaction mockTransaction = Mockito.mock(EntityTransaction.class);
        
        Mockito.when(mockDb.getTransaction()).thenReturn(mockTransaction);
        
        // Configure: driver EXISTS (IF-1 will be TRUE)
        String email = "driver1@gmail.com";
        Driver existingDriver = new Driver(email, "Existing Driver", "123");
        Mockito.when(mockDb.find(Driver.class, email)).thenReturn(existingDriver);
        
        DataAccess sut = new DataAccess(mockDb);
        
        // Execute and verify exception
        boolean exceptionThrown = false;
        try {
            sut.createDriver(email, "Another Name", "456");
        } catch (UserAlreadyExistException e) {
            exceptionThrown = true;
        }
        
        assertTrue("UserAlreadyExistException should be thrown", exceptionThrown);
        
        // Verify: Lines 7-8 executed, lines 10-14 NOT executed
        Mockito.verify(mockDb, Mockito.never()).persist(Mockito.any(Driver.class));
        Mockito.verify(mockTransaction, Mockito.times(1)).commit(); // Commit before exception
        
        System.out.println("✓ Path executed correctly: Lines 1→3-5→7-8→END");
    }
    
    /**
     * WHITE BOX PATH: TRY-1(T)
     * Flow: 1 → 3-5 → NPE → 17-18 → END
     * Test catch block when NullPointerException occurs
     */
    @Test
    public void testPath_Try1True_CatchBlockExecuted() {
        System.out.println("TEST: White Box Mock - Path TRY-1(T) - Catch Block");
        
        // Mock setup
        EntityManager mockDb = Mockito.mock(EntityManager.class);
        EntityTransaction mockTransaction = Mockito.mock(EntityTransaction.class);
        
        Mockito.when(mockDb.getTransaction()).thenReturn(mockTransaction);
        
        // Configure: throw NullPointerException (TRY-1 TRUE)
        String email = "test@gmail.com";
        Mockito.when(mockDb.find(Driver.class, email))
               .thenThrow(new NullPointerException("DB error"));
        
        DataAccess sut = new DataAccess(mockDb);
        
        // Execute
        Driver result = null;
        try {
            result = sut.createDriver(email, "Test Driver", "pass");
        } catch (UserAlreadyExistException e) {
            fail("Should not throw UserAlreadyExistException on NPE");
        }
        
        // Verify: Lines 17-18 executed (catch block)
        assertNull("Driver should be null when NPE occurs", result);
        
        // Verify: Commit called in catch block
        Mockito.verify(mockTransaction, Mockito.times(1)).commit();
        Mockito.verify(mockDb, Mockito.never()).persist(Mockito.any(Driver.class));
        
        System.out.println("✓ Path executed correctly: Lines 1→NPE→17-18→END");
    }
}

