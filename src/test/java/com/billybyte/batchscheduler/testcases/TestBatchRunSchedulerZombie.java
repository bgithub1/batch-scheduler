package com.billybyte.batchscheduler.testcases;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.billybyte.batchscheduler.BatchRunScheduler;
import com.billybyte.batchscheduler.BatchSchedulerParams;
import com.billybyte.commonstaticmethods.LoggingUtils;
import com.billybyte.commonstaticmethods.Utils;
import com.thoughtworks.xstream.XStream;

import junit.framework.TestCase;

public class TestBatchRunSchedulerZombie extends TestCase	{
	public void test1(){
//		BatchRunParams brp = 
//				new BatchRunParams(TestBatchRunSchedulerZombie.class, new String[]{"hi","5000"}, 
//						new String[]{"-Xmx1500m","-Xms300m"}, null, null, "tester", true, -1l, "06:30:00", 5000l);
//		
//		XStream xs = new XStream();
//		String xml = xs.toXML(brp);
//		Utils.prt(xml);
		new LoggingUtils("testlog4j.properties", TestBatchRunScheduler.class);
		List<BatchSchedulerParams> launchList = Utils.getXmlData(List.class, this.getClass(), 
					"batchRunSchedulerTestZombie.xml");
		BatchRunScheduler pLauncher = 
				new BatchRunScheduler(launchList);
		try {
			pLauncher.launchList();
			Thread.sleep(10*1000);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
