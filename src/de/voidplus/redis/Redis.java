package de.voidplus.redis;

import processing.core.*;

public class Redis {
	
	private PApplet parent;
	public final static String VERSION = "1.0";
	
	public Redis(PApplet parent) {
		this.parent = parent;
	}	

	public static String version() {
		return VERSION;
	}

}