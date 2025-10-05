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
	// TRY-1(T): El email es null, el name es "Driver Gonzalez", el password es "135"
	// El test debe devolver null
	public void test1() {
		try {
			// Definir parámetros
			String email = null;
			String name = "Driver Gonzalez";
			String password = "135";
			
			// Configurar el mock para que devuelva null (no debería llegar a ejecutarse)
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			// Invocar al System Under Test (sut)
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			
			// Verificar los resultados
			System.out.println("Gidaria:" + (driver == null));
			assertNull(driver);
			
		} catch (UserAlreadyExistException e) {
			// Si el programa llega a este punto, falla
			fail();
		} catch (Exception e) {
			// Cualquier otra excepción también falla el test
			fail();
		} finally {
			sut.close();
		}
	}
	
	@Test
	// TRY-1(F), IF1(T): El driver ya existe en la DB
	// El email es "driver1@gmail.com", name es "Aitor Fernandez", password es "123"
	// Debe lanzar UserAlreadyExistException
	public void test2() {
		try {
			// Definir parámetros
			String email = "driver1@gmail.com";
			String name = "Aitor Fernandez";
			String password = "123";
			
			// Crear un driver existente
			Driver existingDriver = new Driver(email, name, password);
			
			// Configurar el mock para que devuelva el driver existente
			Mockito.when(db.find(Driver.class, email)).thenReturn(existingDriver);
			
			// Invocar al System Under Test (sut)
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			
			// Si llegamos aquí sin excepción, el test falla
			fail();
			
		} catch (UserAlreadyExistException e) {
			// Verificar que se lanzó la excepción esperada
			assertTrue(true);
		} catch (Exception e) {
			// Cualquier otra excepción falla el test
			fail();
		} finally {
			sut.close();
		}
	}
	
	@Test
	// TRY-1(F), IF1(F): El driver NO existe en la DB
	// El email es "example@gmail.com", name es "Driver Gonzalez", password es "135"
	// El driver debe ser creado correctamente
	public void test3() {
		try {
			// Definir parámetros
			String email = "example@gmail.com";
			String name = "Driver Gonzalez";
			String password = "135";
			
			// Configurar el mock para que devuelva null (driver no existe)
			Mockito.when(db.find(Driver.class, email)).thenReturn(null);
			
			// Invocar al System Under Test (sut)
			sut.open();
			Driver driver = sut.createDriver(email, name, password);
			
			// Verificar los resultados
			assertNotNull(driver);
			assertEquals(email, driver.getEmail());
			assertEquals(name, driver.getName());
			assertEquals(password, driver.getPassword());
			
			// Verificar que se llamó a persist con el driver
			Mockito.verify(db).persist(Mockito.any(Driver.class));
			
		} catch (UserAlreadyExistException e) {
			// Si el programa llega a este punto, falla
			fail();
		} catch (Exception e) {
			// Cualquier otra excepción falla el test
			fail();
		} finally {
			sut.close();
		}
	}
}