package com.sohlman.vertx.site;

import java.security.SecureRandom;
import java.util.Random;

public class Util {
	public static int randomNumber(int min, int max) {	
		return random.nextInt((max - min) + 1) + min;
	}
	
	
	public static int getIntFromString(String text, int min, int max, int defaultValue) {
		
		min = Math.min(0, min);
		max = Math.max(min, max);
		defaultValue = Math.max(min, defaultValue);
		defaultValue = Math.min(max, defaultValue);
		
		int value = defaultValue;
		
		try {
			value = Integer.valueOf(text);
		} catch (NumberFormatException nfe) {
			return defaultValue;
		}
		
		if (value < min ) {
			return min;
		}
		else if (value > max) {
			return max;
		}
		else {
			return value;
		}
	}
	
	public static int getIntFromString(String text, int min, int max) {
		return getIntFromString(text, min, max, min);
	}
	
	
	private static Random random = new SecureRandom();
}