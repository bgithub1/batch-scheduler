package com.billybyte.batchscheduler;

import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.ui.RedirectedConsoleForJavaProcess;
import com.billybyte.ui.RedirectedConsoleForJavaProcess.ConsoleType;
/**
 * This will get a spring beans file.  
 * You should put an init-method="myMethodThatLaunchsMyApp"  attribute in 
 *   the last bean that you create, so that something actually gets launched
 *   as opposed to just a bunch of objects being instantiated
 * @author bperlman1
 *
 */
public class BeansLaunch {
	/**
	 * 
	 * @param args
	 * 	0 = springBeansXmlFileOrPath
	 *  1 = (optional) name of a class that is in the same package as the 
	 *          springBeansXmlFileOrPath file, if you want to access that file
	 *          as a resource.
	 */
	public static void main(String[] args) {
		String springBeansXmlFileOrPath = args[0];
		String classNameOfClassInResourcePkg = args.length>1?args[1]:null;
		
		int index = 2;
		boolean redirectConsole =
				args.length<=index ? true : 
					new Boolean(args[index]);
		index+=1;
		try {
			if(redirectConsole){
				int xLoc = args.length>index ? new Integer(args[index]) : 10;
				index +=1;
				int yLoc = args.length>index ? new Integer(args[index]) : 10;
				index +=1;
				int width = args.length>index ? new Integer(args[index]) : 800;
				index +=1;
				int len = args.length>index ? new Integer(args[index]) : 400;
				
				new RedirectedConsoleForJavaProcess(width, len, xLoc, yLoc, 
						"system.out", ConsoleType.SYSTEM_OUT);
				new RedirectedConsoleForJavaProcess(width, len, xLoc+width, yLoc, 
						"system.err", ConsoleType.SYSTEM_ERR);
			}
		} catch (Exception e) {
		}

		Utils.prt("Launching spring beans file: "+springBeansXmlFileOrPath);
		
		if(classNameOfClassInResourcePkg==null){
			Utils.springGetAllBeans(springBeansXmlFileOrPath);
		}else{
			Utils.prt("file is resource in package of class: "+springBeansXmlFileOrPath);
			Utils.springGetAllBeans(springBeansXmlFileOrPath, classNameOfClassInResourcePkg);
		}

	}



}
