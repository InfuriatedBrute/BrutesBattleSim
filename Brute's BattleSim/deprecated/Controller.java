//package deprecated;
//
//import java.io.*;
//import java.nio.file.*;
//import java.util.*;
//
//import types.UnitType;
//
//class Controller {
//	private String username;
//	
//	private Controller(String username) {
//		if(username == null) {
//			this.username = UUID.randomUUID().toString();
//		}else {
//			this.username = username;
//		}
//	}
//	
//	private List<String> getOrdersFor(UnitType type) {
//		Path path = Paths.get("ControllerOrders\\" + username + "\\"+ type + ".txt");
//		try {
//			if(!Files.exists(path))Files.write(path, new ArrayList<String>());
//			return Files.readAllLines(path);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
//	
//	private void setOrdersFor(UnitType type, List<String> orders){
//		Path path = Paths.get("ControllerOrders\\" + username + "\\"+ type + ".txt");
//		try {
//			Files.write(path, orders);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//	
//	private List<String> getFormationOrdersFor(UnitType type) {
//		Path path = Paths.get("ControllerFormationOrders\\" + username + "\\"+ type + ".txt");
//		try {
//			if(!Files.exists(path))Files.write(path, new ArrayList<String>());
//			return Files.readAllLines(path);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		return null;
//	}
//	
//	private void setFormationOrdersFor(UnitType type, List<String> orders){
//		Path path = Paths.get("ControllerFormationOrders\\" + username + "\\"+ type + ".txt");
//		try {
//			Files.write(path, orders);
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//	}
//}