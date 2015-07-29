package com.billybyte.batchscheduler;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import com.billybyte.commonstaticmethods.Utils;

public class BatchRunDirect {

	/**
	 * 
	 * @param args
	 * @param String batchRunListXmlPath = args[0];

	 */
	public static void main(String[] args) {
		String batchRunListXmlPath = args[0];
		@SuppressWarnings("unchecked")
		List<BatchRunParams> launchList = (List<BatchRunParams>)Utils.getXmlData(List.class, null, batchRunListXmlPath);
		for(int i = 0;i<launchList.size();i++){
			BatchRunParams brp=launchList.get(i);
			
		    Class<?> cls = brp.getClassOfMain();
		    Method meth=null;
			try {
				meth = cls.getMethod("main", String[].class);
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
			if(meth==null)return;
		    String[] params = brp.getCmdLineArgs(); // init params accordingly
		    try {
				meth.invoke(null, (Object) params);
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (InvocationTargetException e) {
				e.printStackTrace();
			} // static method doesn't have an instance

		}
	}
}
