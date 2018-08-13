package main;

import java.awt.Font;

public class Constants {
	public static final int MAX_SEARCH_DISTANCE = 100;
	public static final double STUN_THRESHOLD = 1.10;
	public static final int MAP_WIDTH = 50;
	public static final int MAP_HEIGHT = 10;
	public static final int START_AREA_WIDTH = 6;
	public static final int START_AREA_HEIGHT = 6;
	public static final int TURN_LIMIT = 1000;
	public static final boolean START_AREA_CAN_BE_CENTERED = START_AREA_HEIGHT % 2 == MAP_HEIGHT % 2;
	public static final double NO_ACTION_STUN_VALUE = -0.10;
	public static final int BASE_MOVE_TIME = 10;
	public static final char BLANK_SPACE_CHAR = '_';
	public static final String FONT = "Comic Sans";
}