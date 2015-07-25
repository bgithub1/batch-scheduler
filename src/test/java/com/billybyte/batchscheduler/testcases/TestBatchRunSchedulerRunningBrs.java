package com.billybyte.batchscheduler.testcases;

import java.io.IOException;
import java.util.List;

import com.billybyte.batchscheduler.BatchRunScheduler;
import com.billybyte.batchscheduler.BatchSchedulerParams;
import com.billybyte.commonstaticmethods.LoggingUtils;
import com.billybyte.commonstaticmethods.Utils;

import junit.framework.TestCase;

public class TestBatchRunSchedulerRunningBrs extends TestCase	{
	public void test1(){
		new LoggingUtils("testlog4j.properties", this.getClass());
		List<BatchSchedulerParams> launchList = Utils.getXmlData(List.class, this.getClass(), 
					"batchRunSchedulerTestRunningBrs.xml");
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


