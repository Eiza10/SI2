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

public class createDriverBDBlackTest {

	// sut: system under test
	static DataAccess sut = new DataAccess();
	
	static TestDataAccess testDA = new TestDataAccess();
	
	@SuppressWarnings("unused")
	private Driver driver;
	
	@Test
	// Sarrera: "proba@gmail.com", "Proba Izena", "987"
	// Gidaria arazorik gabe sortu beharko litzateke
	public void test1() {
		String email = "proba@gmail.com";
		String name = "Proba Izena";
		String password = "987";
		
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
			if (testDA.existDriver(email)) {
				testDA.removeDriver(email);
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
			if (driverCreated && testDA.existDriver(email)) {
				testDA.removeDriver(email);
			}
			testDA.close();
		}
	}
	
	@Test
	// Sarrera: null, "Proba Izena", "987"
	// Testak null itzuli behar du
	public void test3() {
		String email = null;
		String name = "Proba Izena";
		String password = "987";
		
		Driver driver = null;
		try {
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			assertNull(driver);
			
		} catch (Exception e) {
			fail();
		} 
	}
	
	@Test
	// Sarrera: "proba@gmail.com", null, "987"
	// Testak null itzuli behar du
	public void test4() {
		String email = "proba@gmail.com";
		String name = null;
		String password = "987";
		
		Driver driver = null;
		
		try {
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			assertNull(driver);
			
		} catch (Exception e) {
			fail();
		} finally {
			// Datu basea garbitu gidaria sortu bada
			testDA.open();
			if (testDA.existDriver(email)) {
				testDA.removeDriver(email);
			}
			testDA.close();
		}
	}
	
	@Test
	// Sarrera: "proba@gmail.com", "Proba Izena", null
	// Testak null itzuli behar du
	public void test5() {
		String email = "proba@gmail.com";
		String name = "Proba Izena";
		String password = null;
		
		Driver driver = null;
		
		try {
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();

			assertNull(driver);
			
		} catch (Exception e) {
			fail();
		} finally {
			// Datu basea garbitu gidaria sortu bada
			testDA.open();
			if (testDA.existDriver(email)) {
				testDA.removeDriver(email);
			}
			testDA.close();
		}
	}
}