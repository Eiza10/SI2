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
	// sut.createDriver: El Driver("proba@gmail.com") NO existe en la DB.
	// El Driver debe ser creado en la DB correctamente.
	// Prueba casos válidos: email, name y password no nulos
	public void test1() {
		String email = "proba@gmail.com";
		String name = "Proba Izena";
		String password = "987";
		
		try {
			// Configurar el mock para que devuelva null (driver no existe)
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			// Invocar al System Under Test (sut)
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			sut.close();
			
			// Verificar los resultados
			assertNotNull(driver);
			assertEquals(email, driver.getEmail());
			assertEquals(name, driver.getName());
			assertEquals(password, driver.getPassword());
			
			// Verificar que se llamó a persist
			Mockito.verify(db).persist(Mockito.any(Driver.class));
			
		} catch (UserAlreadyExistException e) {
			// Si el programa llega a este punto, falla
			fail();
		} catch (Exception e) {
			// Cualquier otra excepción también falla el test
			fail();
		}
	}
	
	@Test
	// sut.createDriver: El Driver("driver1@gmail.com") YA existe en la DB.
	// La excepción UserAlreadyExistException debe ser lanzada.
	public void test2() {
		String email = "driver1@gmail.com";
		String name = "Aitor Fernandez";
		String password = "123";
		
		try {
			// Crear un driver existente
			Driver existingDriver = new Driver(email, name, password);
			
			// Configurar el mock para que devuelva el driver existente
			Mockito.when(db.find(Driver.class, email)).thenReturn(existingDriver);
			
			// Invocar al System Under Test (sut)
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			sut.close();
			
			// Si llegamos aquí sin excepción, el test falla
			fail();
			
		} catch (UserAlreadyExistException e) {
			// Verificar que se lanzó la excepción esperada
			sut.close();
			assertTrue(true);
		} catch (Exception e) {
			// Cualquier otra excepción falla el test
			sut.close();
			fail();
		}
	}
	
	@Test
	// sut.createDriver: El email es null, name es "Proba Izena", password es "987"
	// El test debe devolver null
	public void test3() {
		String email = null;
		String name = "Proba Izena";
		String password = "987";
		
		Driver driver = null;
		
		try {
			// Configurar el mock
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			// Invocar al System Under Test (sut)
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			// Verificar los resultados
			assertNull(driver);
			
		} catch (UserAlreadyExistException e) {
			// Si el programa llega a este punto, falla
			fail();
		} catch (Exception e) {
			// Cualquier otra excepción también falla el test
			fail();
		}
	}
	
	@Test
	// sut.createDriver: El email es "proba@gmail.com", name es null, password es "987"
	// El test debe devolver null
	public void test4() {
		String email = "proba@gmail.com";
		String name = null;
		String password = "987";
		
		Driver driver = null;
		
		try {
			// Configurar el mock
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			// Invocar al System Under Test (sut)
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			// Verificar los resultados
			assertNull(driver);
			
		} catch (UserAlreadyExistException e) {
			// Si el programa llega a este punto, falla
			fail();
		} catch (Exception e) {
			// Cualquier otra excepción también falla el test
			fail();
		}
	}
	
	@Test
	// sut.createDriver: El email es "proba@gmail.com", name es "Proba Izena", password es null
	// El test debe devolver null
	public void test5() {
		String email = "proba@gmail.com";
		String name = "Proba Izena";
		String password = null;
		
		Driver driver = null;
		
		try {
			// Configurar el mock
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			// Invocar al System Under Test (sut)
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			// Verificar los resultados
			assertNull(driver);
			
		} catch (UserAlreadyExistException e) {
			// Si el programa llega a este punto, falla
			fail();
		} catch (Exception e) {
			// Cualquier otra excepción también falla el test
			fail();
		}
	}
}