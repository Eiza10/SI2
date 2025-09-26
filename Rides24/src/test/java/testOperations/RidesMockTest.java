package testOperations;

import java.util.*;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.mockito.Mockito;

import businessLogic.BLFacade;
import configuration.UtilDate;
import domain.Car;
import domain.Driver;
import domain.Ride;
import gui.MainGUI;

public class RidesMockTest {
	static BLFacade appFacadeInterface = Mockito.mock(BLFacade.class);
	public static void main(String args[]) {
	List<String> departingList = new
	ArrayList<String>(Arrays.asList("Bilbo","Donostia","Gasteiz"));
	List<String> arrivalList = new
	ArrayList<String>(Arrays.asList("Madrid","Barcelona"));
	Mockito.when(appFacadeInterface.getDepartCities()).thenReturn(departingList);

	Mockito.when(appFacadeInterface.getDestinationCities("Donostia")).thenReturn(arrivalList
	);

	 List<Date> resultDates = new ArrayList<Date>();
	 Calendar today = Calendar.getInstance();

	 int month=today.get(Calendar.MONTH);
	 int year=today.get(Calendar.YEAR);
	 if (month==12) { month=1; year+=1;}

	 resultDates.add(UtilDate.newDate(year,month, 23));
	 resultDates.add(UtilDate.newDate(year,month, 26));
	 Driver driver=new Driver("driver3@gmail.com","Test Driver", "123");

	 
	 List<Ride> resultRides = new ArrayList<Ride>();
	 resultRides.add(new Ride(4,"Donostia", "Madrid", UtilDate.newDate(2025, 8, 23),
	5, new Driver("Jon", "jon@gmail.com", "pass"), new Car("AA123456", 4, driver, true)));

	 Mockito.when(
	 appFacadeInterface.getThisMonthDatesWithRides(Mockito.eq("Donostia"),
	Mockito.eq("Madrid"),Mockito.any(Date.class))).thenReturn(resultDates);

	 Mockito.when(
	 appFacadeInterface.getRides("Donostia", "Madrid", UtilDate.newDate(year,
	month, 23))).thenReturn(resultRides);
	 
	MainGUI sut = new MainGUI();
	sut.setBussinessLogic(appFacadeInterface);
	try {
	UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
	} catch (ClassNotFoundException | InstantiationException |
	IllegalAccessException
	| UnsupportedLookAndFeelException e) {
	// TODO Auto-generated catch block
	e.printStackTrace();
	}
	sut.setVisible(true);
	}
	}
