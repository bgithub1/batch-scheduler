package com.billybyte.batchscheduler.testcases;

import java.util.Calendar;

public class SayHelloTester {
	public static void main(String[] args) {
		String whatToSay = args[0];
		Long sleepTime = new Long(args[1]);
		Calendar c = Calendar.getInstance();
		System.out.println(whatToSay + " before sleep - " + c.getTime().toString());
		try {
			Thread.sleep(sleepTime);
		} catch (InterruptedException e) {
			e.printStackTrace();
			return;
		}
		 c = Calendar.getInstance();
		System.out.println("Finished sleep - exiting " + SayHelloTester.class.getCanonicalName() + " - " + c.getTime().toString());
	}
}
