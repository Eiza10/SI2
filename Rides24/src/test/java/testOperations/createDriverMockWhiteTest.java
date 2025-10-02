package testOperations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import dataAccess.DataAccess;
import domain.Driver;
import exceptions.UserAlreadyExistException;

public class createDriverMockWhiteTest {

    @Mock
    private EntityManager db;
    
    @Mock
    private EntityTransaction transaction;
    
    @InjectMocks
    private DataAccess dataAccess;
    
    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        // Configurar el comportamiento básico del EntityManager
        when(db.getTransaction()).thenReturn(transaction);
    }
    
    /**
     * Test TRY-1(T): Driver no existe en DB (null)
     * Camino: 1 -> try-1(T) -> 17-18
     * Resultado esperado: Se crea el driver correctamente
     */
    @Test
    public void testCreateDriver_DriverDoesNotExist_Success() {
        // Arrange
        String email = "driver1@gmail.com";
        String name = "Driver Gonzalez";
        String password = "135";
        
        // El driver no existe (devuelve null)
        when(db.find(Driver.class, email)).thenReturn(null);
        
        // Act
        Driver result = null;
        try {
            result = dataAccess.createDriver(email, name, password);
        } catch (UserAlreadyExistException e) {
            fail("No debería lanzar excepción cuando el driver no existe");
        }
        
        // Assert
        assertNotNull("El driver creado no debe ser null", result);
        assertEquals(email, result.getEmail());
        assertEquals(name, result.getName());
        
        // Verificar que se llamó a persist y commit
        ArgumentCaptor<Driver> driverCaptor = ArgumentCaptor.forClass(Driver.class);
        verify(db).persist(driverCaptor.capture());
        verify(transaction, times(1)).commit();
        verify(transaction, times(1)).begin();
        
        Driver persistedDriver = driverCaptor.getValue();
        assertEquals(email, persistedDriver.getEmail());
        assertEquals(name, persistedDriver.getName());
    }
    
    /**
     * Test TRY-1(F), IF1(T): Driver existe en DB
     * Camino: 1 -> try-1(F) -> 3-5 (lanza excepción)
     * Resultado esperado: Se lanza UserAlreadyExistException
     */
    @Test(expected = UserAlreadyExistException.class)
    public void testCreateDriver_DriverAlreadyExists_ThrowsException() throws UserAlreadyExistException {
        // Arrange
        String email = "driver1@gmail.com";
        String name = "Aitor Fernandez";
        String password = "123";
        
        // El driver ya existe
        Driver existingDriver = new Driver(email, "Existing Name", "oldpass");
        when(db.find(Driver.class, email)).thenReturn(existingDriver);
        
        // Act
        dataAccess.createDriver(email, name, password);
        
        // Assert (la excepción se verifica con expected en @Test)
        // Verificar que se hizo commit antes de lanzar la excepción
        verify(transaction, times(1)).commit();
        verify(db, never()).persist(any(Driver.class));
    }
    
    /**
     * Test TRY-1(F), IF1(F): Driver existe y db.find devuelve algo distinto a null
     * Camino: 1 -> try-1(F) -> if-1(F) -> 7-8 (similar al anterior pero verificando el camino)
     * Resultado esperado: Se lanza UserAlreadyExistException
     */
    @Test
    public void testCreateDriver_DriverExistsVerifyPath_ThrowsException() {
        // Arrange
        String email = "example@gmail.com";
        String name = "Driver Gonzalez";
        String password = "135";
        
        Driver existingDriver = new Driver(email, name, password);
        when(db.find(Driver.class, email)).thenReturn(existingDriver);
        
        // Act & Assert
        try {
            dataAccess.createDriver(email, name, password);
            fail("Debería haber lanzado UserAlreadyExistException");
        } catch (UserAlreadyExistException e) {
            // Excepción esperada
            assertNotNull("El mensaje de la excepción no debe ser null", e.getMessage());
        }
        
        // Verify
        verify(transaction, times(1)).begin();
        verify(transaction, times(1)).commit();
        verify(db, times(1)).find(Driver.class, email);
        verify(db, never()).persist(any(Driver.class));
    }
    
    /**
     * Test adicional: NullPointerException en el catch
     * Camino: 1 -> try-1(exception) -> 10-14 -> END
     * Resultado esperado: Retorna null y hace commit
     */
    @Test
    public void testCreateDriver_NullPointerException_ReturnsNull() {
        // Arrange
        String email = "test@gmail.com";
        String name = "Test Driver";
        String password = "pass";
        
        // Simular NullPointerException
        when(db.find(Driver.class, email)).thenThrow(new NullPointerException());
        
        // Act
        Driver result = null;
        try {
            result = dataAccess.createDriver(email, name, password);
        } catch (UserAlreadyExistException e) {
            fail("No debería lanzar UserAlreadyExistException");
        }
        
        // Assert
        assertNull("El resultado debe ser null cuando hay NullPointerException", result);
        verify(transaction, times(1)).commit();
        verify(db, never()).persist(any(Driver.class));
    }
}