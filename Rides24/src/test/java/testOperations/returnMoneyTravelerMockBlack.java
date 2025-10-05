package testOperations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

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
import org.mockito.MockitoAnnotations;

import dataAccess.DataAccess;
import domain.Driver;
import domain.Reservation;
import domain.Traveler;
import domain.Transaction;

public class returnMoneyTravelerMockBlack {

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
        persistenceMock = mockStatic(Persistence.class);
        persistenceMock.when(() -> Persistence.createEntityManagerFactory(any()))
            .thenReturn(entityManagerFactory);

        when(entityManagerFactory.createEntityManager()).thenReturn(db);
        when(db.getTransaction()).thenReturn(et);

        sut = new DataAccess(db);
    }

    @After
    public void tearDown() {
        persistenceMock.close();
    }

    /**
     * Caso BB1: Entrada = lista vacía → no hay efectos
     */
    @Test
    public void testEmptyReservationList() {
        List<Reservation> resList = new ArrayList<>();

        sut.open();
        sut.returnMoneyTravelers(resList, "driver@test.com");
        sut.close();

        verify(db, never()).persist(any());
    }

    /**
     * Caso BB2: Reserva no pagada → no se modifica nada
     */
    @Test
    public void testReservationNotPayed() {
        Reservation res = mock(Reservation.class);
        when(res.isPayed()).thenReturn(false);

        List<Reservation> resList = List.of(res);

        sut.open();
        sut.returnMoneyTravelers(resList, "driver@test.com");
        sut.close();

        verify(db, never()).persist(any());
    }

    /**
     * Caso BB3: Reserva pagada → traveler recibe + dinero, driver - dinero
     */
    @Test
    public void testReservationPayed() {
        Reservation res = mock(Reservation.class);
        Traveler t = mock(Traveler.class);
        Driver d = mock(Driver.class);

        when(res.isPayed()).thenReturn(true);
        when(res.getCost()).thenReturn((float)50.0);
        when(res.getTraveler()).thenReturn(t);
        when(res.getDriver()).thenReturn(d);

        when(t.getEmail()).thenReturn("trav@test.com");
        when(t.getMoney()).thenReturn((float)100.0);
        when(d.getMoney()).thenReturn((float)200.0);

        when(db.find(Traveler.class, "trav@test.com")).thenReturn(t);
        when(db.find(Driver.class, "driver@test.com")).thenReturn(d);

        List<Reservation> resList = List.of(res);

        sut.open();
        sut.returnMoneyTravelers(resList, "driver@test.com");
        sut.close();

        // Resultados esperados (vista externa)
        verify(t).setMoney((float)150.0);
        verify(d).setMoney((float)150.0);
        verify(db, times(1)).persist(t);
        verify(db, times(1)).persist(d);
        verify(db, times(1)).persist(any(Transaction.class));
    }

    /**
     * Caso BB4: Varias reservas, solo algunas pagadas → solo esas afectan
     */
    @Test
    public void testMixedReservations() {
        Reservation res1 = mock(Reservation.class);
        Reservation res2 = mock(Reservation.class);
        Traveler t = mock(Traveler.class);
        Driver d = mock(Driver.class);

        when(res1.isPayed()).thenReturn(true);
        when(res1.getCost()).thenReturn((float)30.0);
        when(res1.getTraveler()).thenReturn(t);
        when(res1.getDriver()).thenReturn(d);

        when(res2.isPayed()).thenReturn(false);

        when(t.getEmail()).thenReturn("trav@test.com");
        when(t.getMoney()).thenReturn((float)200.0);
        when(d.getMoney()).thenReturn((float)500.0);

        when(db.find(Traveler.class, "trav@test.com")).thenReturn(t);
        when(db.find(Driver.class, "driver@test.com")).thenReturn(d);

        List<Reservation> resList = List.of(res1, res2);

        sut.open();
        sut.returnMoneyTravelers(resList, "driver@test.com");
        sut.close();

        // Solo res1 produce efectos
        verify(t).setMoney((float)230.0);
        verify(d).setMoney((float)470.0);
        verify(db, times(1)).persist(any(Transaction.class));
    }

    /**
     * Caso BB5: DB lanza NullPointer → commit()
     */
    @Test
    public void testDbThrowsNullPointer() {
        when(db.find(Driver.class, "driver@test.com")).thenThrow(new NullPointerException());

        sut.open();
        sut.returnMoneyTravelers(new ArrayList<>(), "driver@test.com");
        sut.close();

        verify(et, times(1)).commit();
    }
}
