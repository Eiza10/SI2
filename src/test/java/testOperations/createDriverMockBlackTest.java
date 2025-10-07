package testOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import exceptions.UserAlreadyExistException;

public class createDriverMockBlackTest {
	
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
	// Sarrera: "proba@gmail.com", "Proba Izena", "987"
	// Gidaria arazorik gabe sortu beharko litzateke
	public void test1() {
		String email = "proba@gmail.com";
		String name = "Proba Izena";
		String password = "987";
		
		try {
			// Konfiguratu mock-a null itzul dezan (Gidaria ez da existitzen)
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			sut.close();
			
			assertNotNull(driver);
			assertEquals(email, driver.getEmail());
			assertEquals(name, driver.getName());
			assertEquals(password, driver.getPassword());
			
			// Gidariaren sorrera bermatu
			Mockito.verify(db).persist(Mockito.any(Driver.class));
			
		} catch (UserAlreadyExistException e) {
			fail();
		} catch (Exception e) {
			fail();
		}
	}
	
	@Test
	// Sarrera: "driver1@gmail.com", "Aitor Fernandez", "123"
	// Erabiltzailea jada datu basean dago, beraz, UserAlreadyExistException saltatu beharko luke.
	public void test2() {
		String email = "driver1@gmail.com";
		String name = "Aitor Fernandez";
		String password = "123";
		
		try {
			Driver existingDriver = new Driver(email, name, password);
			
			// Konfiguratu mock-a lehendik dagoen driverra itzul dezan
			Mockito.when(db.find(Driver.class, email)).thenReturn(existingDriver);
			
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			sut.close();
			
			fail();
			
		} catch (UserAlreadyExistException e) {
			sut.close();
			assertTrue(true);
		} catch (Exception e) {
			sut.close();
			fail();
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
			// Konfiguratu mock-a null itzuli dezan
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			assertNull(driver);
			
		} catch (UserAlreadyExistException e) {
			fail();
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
			// Konfiguratu mock-a null itzuli dezan
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			assertNull(driver);
			
		} catch (UserAlreadyExistException e) {
			fail();
		} catch (Exception e) {
			fail();
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
			// Konfiguratu mock-a null itzuli dezan
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			assertNull(driver);
			
		} catch (UserAlreadyExistException e) {
			fail();
		} catch (Exception e) {
			fail();
		}
	}
}