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

public class createDriverMockWhiteTest {
	
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
	// Sarrera: null, "Driver Gonzalez", "135"
	// Testak null itzuli behar du
	public void test1() {
		try {
			String email = null;
			String name = "Driver Gonzalez";
			String password = "135";
			
			// Konfiguratu mock-a null itzul dezan
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			
			System.out.println("Gidaria:" + (driver == null));
			assertNull(driver);
			
		} catch (UserAlreadyExistException e) {
			fail();
		} catch (Exception e) {
			fail();
		} finally {
			sut.close();
		}
	}
	
	@Test
	// Sarrera: "driver1@gmail.com", "Aitor Fernandez", "123"
	// Erabiltzailea jada datu basean dago, beraz, UserAlreadyExistException saltatu beharko luke.
	public void test2() {
		try {
			String email = "driver1@gmail.com";
			String name = "Aitor Fernandez";
			String password = "123";
			
			Driver existingDriver = new Driver(email, name, password);
			
			// Konfiguratu mock-a lehendik dagoen driverra itzul dezan
			Mockito.when(db.find(Driver.class, email)).thenReturn(existingDriver);
			
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			
			fail();
			
		} catch (UserAlreadyExistException e) {
			assertTrue(true);
		} catch (Exception e) {
			fail();
		} finally {
			sut.close();
		}
	}
	
	@Test
	// Sarrera: "example@gmail.com", "Driver Gonzalez", "135"
	// Gidaria datu basean sortu behar da 
	public void test3() {
		try {
			String email = "example@gmail.com";
			String name = "Driver Gonzalez";
			String password = "135";
			
			// Konfiguratu mock-a null itzul dezan (gidaria ez da existitzen)
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			
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
		} finally {
			sut.close();
		}
	}
}