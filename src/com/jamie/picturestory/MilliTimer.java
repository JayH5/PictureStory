package com.jamie.picturestory;

/**
* Reduces typing when using System.currentTimeMillis() to time operations.
* v1.2
* Jamie Hewland
* 2012
*/
public class MilliTimer {
	private long startTime;
	
	MilliTimer() {
		restart();
	}
	public void restart() {
		startTime = System.currentTimeMillis();
	}
	public long getTime() {
		return (System.currentTimeMillis() - startTime);
	}
	public String toString() {
		return "" + getTime();
	}
}
