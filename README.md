# batch-scheduler
Flexibly schedule running the main's in your java  processes on any operating system that supports the JVM.  No need to have separate schedulers for Windows, Mac and Linux.
See both mains and junit test cases in project for examples of how to use.

Example using xml file to define the main in a class to run
```
<list>

	<com.billybyte.batchscheduler.BatchSchedulerParams>
		<alias>sayHelloOnce</alias>
		<scheduleType>SCHEDULE_AT_OFFSET_FROM_TODAY_IN_MILLS</scheduleType>
		<classOfMain>com.billybyte.batchscheduler.testcases.SayHelloTester</classOfMain>
		<cmdLineArgs>
			<string>Hi there - I'm waiting 2 seconds</string>
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
		<nextScheduleInMills>5000</nextScheduleInMills> 
	</com.billybyte.batchscheduler.BatchSchedulerParams>

   
</list>

```


Java class SayHelloTester which I reference above:
```
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
```

TestCase code to schedule running the main in SayHelloTester:
```
package com.billybyte.batchscheduler.testcases;

import java.io.IOException;
import java.util.List;

import com.billybyte.batchscheduler.BatchRunScheduler;
import com.billybyte.batchscheduler.BatchSchedulerParams;
import com.billybyte.commonstaticmethods.LoggingUtils;
import com.billybyte.commonstaticmethods.Utils;

import junit.framework.TestCase;

public class TestBatchRunScheduler extends TestCase	{
	public void test1(){
		new LoggingUtils("testlog4j.properties", this.getClass());
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

	}

}

```

