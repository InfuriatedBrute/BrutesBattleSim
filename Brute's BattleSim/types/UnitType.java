package types;

import java.util.*;
import java.io.*;
import java.nio.file.*;

/** I.e. Elf Archer */
public class UnitType {
	private Path path;

	public UnitType(String name) {
		path = Paths.get("textfiles\\UnitTypes\\" + name + ".txt");
	}

	public int get(Stat x) {
		return Integer.parseInt(getByCode(x.name()));
	}

	public String get(Attribute x) {
		String s = getByCode(x.name());
		if(x.equals(Attribute.ICON) && s.equals("~")) throw new RuntimeException("~ is a forbidden unit character.");
		return s;
	}

	private String getByCode(String name) {
		String s = name + ": ";
		try {
			if (!Files.exists(path))
				Files.write(path, new ArrayList<String>()); // make a blank file if none exists
			for (String line : Files.readAllLines(path)) {
				if (line.indexOf(s)==0)
					return line.substring(s.length());
			}
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void set(String statOrAttributeName, String value) {
		try {
			String code = statOrAttributeName + ": ";
			if (!Files.exists(path))
				Files.write(path, new ArrayList<String>()); // make a blank file if none exists
			List<String> lines = Files.readAllLines(path);
			boolean statcodeFound = false;
			for (int i = 0; i < lines.size() && !statcodeFound; i++) {
				if (lines.get(i).contains(code)) {
					lines.set(i, code + value);
					statcodeFound = true;
				}
			}
			if (!statcodeFound)
				lines.add(code + value);
			Files.write(path, lines);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public String toString() {
		return(get(Attribute.ICON));
	}

}