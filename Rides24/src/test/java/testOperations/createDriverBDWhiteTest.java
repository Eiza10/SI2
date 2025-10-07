package testOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import dataAccess.DataAccess;
import domain.Driver;
import exceptions.UserAlreadyExistException;
import testOperations.TestDataAccess;

public class createDriverBDWhiteTest {

	// sut: system under test
	static DataAccess sut = new DataAccess();
	
	static TestDataAccess testDA = new TestDataAccess();
	
	@SuppressWarnings("unused")
	private Driver driver;
	
	@Test
	// Sarrera: null, "Driver Gonzalez", "135"
	// Testak null itzuli behar du
	public void test1() {
	    Driver driver = null;
	    String email = null;
	    String name = "Driver Gonzalez";
	    String password = "135";
	    
	    try {
	        sut.open();
	        driver = sut.createDriver(email, name, password);
	        sut.close();

	        assertNull(driver);
	        
	    } catch (Exception e) {
	    	e.printStackTrace();
	        fail();
	    } finally {
	        // Datubasetik ezabatu sortuz gero
	        testDA.open();
	        if (driver != null && driver.getEmail() != null) {
	            if (testDA.existDriver(driver.getEmail())) {
	                testDA.removeDriver(driver.getEmail());
	            }
	        }
	        testDA.close();
	    }
	}
	
	@Test
	// Sarrera: "driver1@gmail.com", "Aitor Fernandez", "123"
	// Erabiltzailea jada datu basean dago, beraz, UserAlreadyExistException saltatu beharko luke.
	public void test2() {
		String email = "driver1@gmail.com";
		String name = "Aitor Fernandez";
		String password = "123";
		
		boolean driverCreated = false;
		
		try {
			// Giadria sortu test aurretik (jada ez balego)
			testDA.open();
			if (!testDA.existDriver(email)) {
				testDA.createDriver(email, name, password);
				driverCreated = true;
			}
			testDA.close();
			
			sut.open();
			sut.createDriver(email, name, password);
			sut.close();
			
			fail();
			
		} catch (UserAlreadyExistException e) {
			sut.close();
			assertTrue(true);
		} catch (Exception e) {
			sut.close();
			fail();
		} finally {
			// Sortutako gidaria ezabatu (sortu bada)
			testDA.open();
			if (driverCreated) {
				testDA.removeDriver(email);
			}
			testDA.close();
		}
	}
	
	@Test
	// Sarrera: "example@gmail.com", "Driver Gonzalez", "135"
	// Gidaria datu basean sortu behar da 
	public void test3() {
		String email = "example@gmail.com";
		String name = "Driver Gonzalez";
		String password = "135";
		
		Driver driver = null;
		
		try {

			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			assertNotNull(driver);
			assertEquals(email, driver.getEmail());
			assertEquals(name, driver.getName());
			assertEquals(password, driver.getPassword());
			
			// Gidaria ondo sortu dela checkeatu
			testDA.open();
			boolean exist = testDA.existDriver(email);
			assertTrue(exist);
			testDA.close();
			
		} catch (UserAlreadyExistException e) {
			fail();
		} catch (Exception e) {
			fail();
		} finally {
			// Sortutako gidaria ezabatu
			testDA.open();
			testDA.removeDriver(email);
			testDA.close();
		}
	}
	
}