package testOperations;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import dataAccess.DataAccess;
import domain.Driver;
import domain.Reservation;
import domain.Traveler;
import domain.Transaction;

public class returnMoneyTravelerMockWhite {

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

    /**
     * Caso 1: Lista vacía → no persiste nada
     */
    @Test
    public void testEmptyList() {
        try {
            List<Reservation> resList = new ArrayList<>();

            sut.open();
            sut.returnMoneyTravelers(resList, "driver@test.com");
            sut.close();

            // Verificamos que no se haya llamado persist
            Mockito.verify(db, Mockito.never()).persist(Mockito.any());

        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Caso 2: Reserva no pagada → no pasa por if
     */
    @Test
    public void testReservationNotPaid() {
        try {
            Reservation res = Mockito.mock(Reservation.class);
            Mockito.when(res.isPayed()).thenReturn(false);

            List<Reservation> resList = List.of(res);

            sut.open();
            sut.returnMoneyTravelers(resList, "driver@test.com");
            sut.close();

            Mockito.verify(db, Mockito.never()).persist(Mockito.any());

        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Caso 3: Reserva pagada → traveler recibe dinero, driver pierde dinero
     */
    @Test
    public void testReservationPaid() {
        try {
            // Mocks
            Reservation res = Mockito.mock(Reservation.class);
            Traveler t = Mockito.mock(Traveler.class);
            Driver d = Mockito.mock(Driver.class);

            Mockito.when(res.isPayed()).thenReturn(true);
            Mockito.when(res.getCost()).thenReturn((float)100.0);
            Mockito.when(res.getTraveler()).thenReturn(t);
            Mockito.when(res.getDriver()).thenReturn(d);

            Mockito.when(t.getEmail()).thenReturn("trav@test.com");
            Mockito.when(t.getMoney()).thenReturn((float)200.0);
            Mockito.when(d.getMoney()).thenReturn((float)500.0);

            Mockito.when(db.find(Traveler.class, "trav@test.com")).thenReturn(t);
            Mockito.when(db.find(Driver.class, "driver@test.com")).thenReturn(d);

            List<Reservation> resList = List.of(res);

            sut.open();
            sut.returnMoneyTravelers(resList, "driver@test.com");
            sut.close();

            // Verificaciones
            Mockito.verify(t).setMoney((float)300.0); // 200 + 100
            Mockito.verify(d).setMoney((float)400.0); // 500 - 100

            Mockito.verify(db, Mockito.times(1)).persist(t);
            Mockito.verify(db, Mockito.times(1)).persist(d);
            Mockito.verify(db, Mockito.times(1)).persist(Mockito.any(Transaction.class));

        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Caso 4: Varias reservas (algunas pagadas, otras no)
     */
    @Test
    public void testMultipleReservations() {
        try {
            Reservation res1 = Mockito.mock(Reservation.class);
            Reservation res2 = Mockito.mock(Reservation.class);

            Traveler t = Mockito.mock(Traveler.class);
            Driver d = Mockito.mock(Driver.class);

            // res1 pagada
            Mockito.when(res1.isPayed()).thenReturn(true);
            Mockito.when(res1.getCost()).thenReturn((float) 100.0);
            Mockito.when(res1.getTraveler()).thenReturn(t);
            Mockito.when(res1.getDriver()).thenReturn(d);
            Mockito.when(t.getEmail()).thenReturn("trav@test.com");
            Mockito.when(t.getMoney()).thenReturn((float)200.0);
            Mockito.when(d.getMoney()).thenReturn((float)500.0);

            // res2 no pagada
            Mockito.when(res2.isPayed()).thenReturn(false);

            Mockito.when(db.find(Traveler.class, "trav@test.com")).thenReturn(t);
            Mockito.when(db.find(Driver.class, "driver@test.com")).thenReturn(d);

            List<Reservation> resList = List.of(res1, res2);

            sut.open();
            sut.returnMoneyTravelers(resList, "driver@test.com");
            sut.close();

            // Verificamos que solo res1 procesada
            Mockito.verify(t).setMoney((float)300.0);
            Mockito.verify(d).setMoney((float)400.0);
            Mockito.verify(db, Mockito.times(1)).persist(Mockito.any(Transaction.class));

        } catch (Exception e) {
            fail();
        }
    }

    /**
     * Caso 5: NullPointerException en DB → commit en catch
     */
    @Test
    public void testNullPointerCommit() {
        try {
            List<Reservation> resList = new ArrayList<>();

            Mockito.when(db.find(Driver.class, "driver@test.com"))
                .thenThrow(new NullPointerException());

            sut.open();
            sut.returnMoneyTravelers(resList, "driver@test.com");
            sut.close();

            Mockito.verify(et, Mockito.times(1)).commit();

        } catch (Exception e) {
            fail();
        }
    }
}
