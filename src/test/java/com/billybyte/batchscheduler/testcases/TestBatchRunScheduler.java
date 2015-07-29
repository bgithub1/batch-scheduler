package com.billybyte.batchscheduler.testcases;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.billybyte.batchscheduler.BatchRunScheduler;
import com.billybyte.batchscheduler.BatchSchedulerParams;
import com.billybyte.batchscheduler.BatchSchedulerParams.ScheduleType;
import com.billybyte.commonstaticmethods.CollectionsStaticMethods;
import com.billybyte.commonstaticmethods.LoggingUtils;
import com.billybyte.commonstaticmethods.Utils;

import junit.framework.TestCase;

public class TestBatchRunScheduler extends TestCase	{
	@SuppressWarnings("unchecked")
	public void test1(){
		new LoggingUtils("testlog4j.properties", this.getClass());
		Utils.prtObMess(this.getClass(), " starting test1");

		List<BatchSchedulerParams> launchList = Utils.getXmlData(List.class, this.getClass(), 
					"batchRunSchedulerTest.xml");
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
		Utils.prtObMess(this.getClass(), " done with test1");
	}
	
	
	/*

 * 
 * 
 */
	

	/**
<com.billybyte.batchscheduler.BatchSchedulerParams>
		<alias>sayHelloOnce</alias>
		<scheduleType>SCHEDULE_AT_OFFSET_FROM_TODAY_IN_MILLS</scheduleType>
		<classOfMain>com.billybyte.batchscheduler.testcases.SayHelloTester</classOfMain>
		<cmdLineArgs>
			<string>Hi there from test2 - I'm waiting 0 seconds</string>
			<string>2000</string>
		</cmdLineArgs>
		<vmArgs>
			<string>-Xmx1500m</string>
			<string>-Xms300m</string>
		</vmArgs>
		<validDays>
			<java.lang.Integer>1</java.lang.Integer>
			<java.lang.Integer>2</java.lang.Integer>
			<java.lang.Integer>3</java.lang.Integer>
			<java.lang.Integer>4</java.lang.Integer>
			<java.lang.Integer>5</java.lang.Integer>
			<java.lang.Integer>6</java.lang.Integer>
			<java.lang.Integer>7</java.lang.Integer>
		</validDays>
		<nextScheduleInMills></nextScheduleInMills> 
</com.billybyte.batchscheduler.BatchSchedulerParams>

	 * do a test constructing List<BatchSchedulerParams> directly (as opposed through xml).
	 */
	public void test2(){
		// mimic the xml above
		new LoggingUtils("testlog4j.properties", this.getClass());
		Utils.prtObMess(this.getClass(), " starting test2");
		ScheduleType scheduleType = ScheduleType.SCHEDULE_AT_OFFSET_FROM_TODAY_IN_MILLS;
		Class<?> classOfMain = SayHelloTester.class;
		String[] cmdLineArgs = {"Hi there from test2 - I'm waiting 0 seconds","000"};
		String[] vmArgs = {"-Xmx1500m","-Xms300m"};
		String workingDirectory = null;
		Long waitTime = null;
		String alias = "sayHelloASecondTime";
		Boolean restart = false;
		Long yyyyMmDdOrOffsetToStart = null;
		String hhMmSsMmmToStart = null;
		Long nextScheduleInMills = null;
		Set<Integer> validDays = CollectionsStaticMethods.setFromArray(new Integer[]{
				1,2,3,4,5,6,7
		});
		String aliasToKill = null;
		
		BatchSchedulerParams bsp = 
				new BatchSchedulerParams(
						scheduleType, classOfMain, cmdLineArgs, 
						vmArgs, workingDirectory, waitTime, 
						alias, restart, yyyyMmDdOrOffsetToStart, 
						hhMmSsMmmToStart, nextScheduleInMills, 
						validDays, aliasToKill);
		List<BatchSchedulerParams> launchList = 
				new ArrayList<BatchSchedulerParams>();
		launchList.add(bsp);
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
		Utils.prtObMess(this.getClass(), " done with test2");

	}

}


