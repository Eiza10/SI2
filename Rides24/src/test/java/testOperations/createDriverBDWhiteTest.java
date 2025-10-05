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
	
	// additional operations needed to execute the test
	static TestDataAccess testDA = new TestDataAccess();
	
	@SuppressWarnings("unused")
	private Driver driver;
	
	@Test
	// sut.createDriver: El email es null, el name es "Driver Gonzalez", el password es "135"
	// El test debe devolver null. Si se devuelve una excepción, el método createDriver no está bien implementado.
	public void test1() {
		Driver driver = null;
		try {
			// Definir parámetros
			String email = null;
			String name = "Driver Gonzalez";
			String password = "135";
			
			// Invocar al System Under Test (sut)
			sut.open();
			driver = sut.createDriver(email, name, password);
			
			// Verificar los resultados
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
	// sut.createDriver: El Driver con email "driver1@gmail.com" YA existe en la DB.
	// La excepción UserAlreadyExistException debe ser lanzada.
	public void test2() {
		String email = "driver1@gmail.com";
		String name = "Aitor Fernandez";
		String password = "123";
		
		boolean driverCreated = false;
		
		try {
			// Crear el driver en la DB antes del test
			testDA.open();
			if (!testDA.existDriver(email)) {
				testDA.createDriver(email, name, password);
				driverCreated = true;
			}
			testDA.close();
			
			// Invocar al System Under Test (sut)
			sut.open();
			sut.createDriver(email, name, password);
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
		} finally {
			// Eliminar los objetos creados en la base de datos
			testDA.open();
			if (driverCreated) {
				testDA.removeDriver(email);
			}
			testDA.close();
		}
	}
	
	@Test
	// sut.createDriver: El Driver con email "example@gmail.com" NO existe en la DB.
	// El Driver debe ser creado en la DB correctamente.
	// El test asume que "example@gmail.com" no existe en la DB antes del test.
	public void test3() {
		String email = "example@gmail.com";
		String name = "Driver Gonzalez";
		String password = "135";
		
		Driver driver = null;
		
		try {
			// Invocar al System Under Test (sut)
			sut.open();
			driver = sut.createDriver(email, name, password);
			sut.close();
			
			// Verificar los resultados
			assertNotNull(driver);
			assertEquals(email, driver.getEmail());
			assertEquals(name, driver.getName());
			assertEquals(password, driver.getPassword());
			
			// Verificar que el driver está en la DB
			testDA.open();
			boolean exist = testDA.existDriver(email);
			assertTrue(exist);
			testDA.close();
			
		} catch (UserAlreadyExistException e) {
			// Si el programa llega a este punto, falla
			fail();
		} catch (Exception e) {
			// Cualquier otra excepción también falla el test
			fail();
		} finally {
			// Eliminar los objetos creados en la base de datos
			testDA.open();
			testDA.removeDriver(email);
			testDA.close();
		}
	}
	
}