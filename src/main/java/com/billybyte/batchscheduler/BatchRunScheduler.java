package com.billybyte.batchscheduler;

import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.billybyte.batchscheduler.BatchSchedulerParams.ScheduleType;
import com.billybyte.clientserver.ServiceBlock;
import com.billybyte.clientserver.webserver.WebServiceComLib;
import com.billybyte.commoninterfaces.QueryInterface;
import com.billybyte.commonstaticmethods.Dates;
import com.billybyte.commonstaticmethods.LoggingUtils;
import com.billybyte.commonstaticmethods.Reflection;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.ui.RedirectedConsoleForJavaProcess;
import com.thoughtworks.xstream.XStream;


/**
 * BatchRunScheduler schedules any Java main to be executed in the future.
 * The main can be:
 * 		- executed once;
 * 		- executed repeatedly from a date;
 * 		- automatically restarted if it exits (when it has an unrecoverable error
 * 			like losing a connection temporarily (like Meteor DDP connections)
 * 		
 * @author bperlman1
 *
 */
public class BatchRunScheduler {
	private final LoggingUtils logger = new LoggingUtils(this.getClass());
	private final List<BatchSchedulerParams> launchList;
	private final BlockingQueue<BatchSchedulerParams> bbpQueue = 
			new ArrayBlockingQueue<BatchSchedulerParams>(1000);
	private final Map<String, ProcessAsynchStarter> processInfoMap = 
			new ConcurrentHashMap<String, BatchRunScheduler.ProcessAsynchStarter>();
	
	private final AtomicReference<Thread> queueThread = new AtomicReference<Thread>();
	private final Map<String, TimerInfoAbstract> timerInfoMap  = 
			new ConcurrentHashMap<String, BatchRunScheduler.TimerInfoAbstract>();
	
	private final QueryInterface<String, String> killProcessQuery = new QueryInterface<String, String>() {
		
		@Override
		public String get(String alias, int timeoutValue, TimeUnit timeUnitType) {
			String[] parts = alias.split(",");
			if(parts.length>1){
				Boolean allowRestart=false;
				try {
					allowRestart = new Boolean(parts[1]);
				} catch (Exception e) {
				}
				return killProcess(alias, allowRestart);
			}
			return killProcess(alias,false);
		}
	};		
	
	private String killProcess(String alias,boolean allowRestart){
		ProcessAsynchStarter pas = processInfoMap.get(alias);
		if(pas!=null){
			pas.killProcess(allowRestart);
			processInfoMap.remove(alias);
		}else{
			return "can't find process";
		}
		return "success";
	}
	
	public BatchRunScheduler(
			List<BatchSchedulerParams> launchList){
		this.launchList = launchList;
	}
	
	
	
	/**
	 * Pass a service block so that you can kill processes by alias via
	 *   a WebService
	 *   
	 * @param launchList
	 * @param sbKillProcess
	 */
	public BatchRunScheduler(
			List<BatchSchedulerParams> launchList,
			ServiceBlock sbKillProcess){
		this(launchList);
		if(sbKillProcess!=null)
			WebServiceComLib.getQueryService(sbKillProcess, new XStream());
	}
	
	public QueryInterface<String, String> getKillProcessQuery(){
		return this.killProcessQuery;
	}
	
	/**
	 * 
	 * @param batchRunListXmlPath - an xml file path that has a list of 
	 * 	BatchRunParams instances.
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	public void launchList() throws IOException, Exception{
		if(this.queueThread.get()==null){
			Thread t = new Thread(new ProcessSchedulerRunnable());
			this.queueThread.set(t);
			t.start();
		}
		for(int i = 0;i<launchList.size();i++){
			BatchSchedulerParams bbp=launchList.get(i);
			bbpQueue.put(bbp);
		}
	}
	


	/**
	 * Start or Kill a process<br>
	 * If brp.getClassOfMain()==null then this  ProcessAsynchStarter is a "Killer"<br>
	 * 	Killers do not launch a new process, they kill an existing one.<br>
	 *  The process that gets killed will cause the <br>
	 *  		pResponse = process.get().waitFor()  statement to fall through <br>
	 *  		as if it ended normally.  <br>
	 * @author bperlman1
	 *
	 */
	private  class ProcessAsynchStarter extends TimerTask{
		private final BatchSchedulerParams bsp;
		private final AtomicReference<Process> process = new AtomicReference<Process>();
		/**
		 * 
		 * @param bsp BatchRunParams
		 */
		private ProcessAsynchStarter(
				BatchSchedulerParams bsp){
				super();
			logger.info(bsp.getAlias()+ " constructor for ProcessAsynchStarter");
			this.bsp = bsp;
		}

		
		@Override
		public void run() {
			String alias = bsp.getAlias();
			// first wait
			// ready to start process or kill it
			boolean restart = bsp.getRestart();
			// ************** are we a Killer ??? ************************
			if(bsp.getClassOfMain()==null){
				// ***************** !!!!! we are a Killer Process !!!!! ***********************
				// either kill process with this alias, or kill process
				//  with another alias
				if(bsp.getAliasToKill()!=null){
					// kill this alias
					String aliasToKill = bsp.getAliasToKill();
					logger.info("Killer " + alias + " is Killing Process " + aliasToKill + " with restart = " + restart);
					BatchRunScheduler.this.killProcess(aliasToKill,restart);
					// remove alias of the process that got killed
					processInfoMap.remove(aliasToKill);
					// add myself to processInfoMap if I'm not already there
					if(!processInfoMap.containsKey(alias)){
						processInfoMap.put(alias, this);
					}
				}else{
					// kill this process 
					logger.info("Killing Process " + alias + " with restart = " + restart);
					BatchRunScheduler.this.killProcess(alias,restart);
					processInfoMap.remove(alias);
				}
				process.set(null);
				return;
			}
	
			
			if(processInfoMap.containsKey(alias)){
				logger.info("Process " + alias + " is already running");
				return;
			}

			Set<Integer> validDaysSet = bsp.getValidDays();
			if(validDaysSet!=null){
				// see if this is a valid week day to run this class
				//  if not, just ignore the whole thing
				Calendar today = Calendar.getInstance();
				int weekDay = today.get(Calendar.DAY_OF_WEEK);
				if(!validDaysSet.contains(weekDay)){
					logger.info("Today is not a valid day to execute " + bsp.getAlias());
					return;
				}
			}
			
			// start process
			String className = bsp.getClassOfMain().getCanonicalName();
			
			if(bsp.getClassOfMain().equals(BeansLaunch.class)){
				className = className + " : "+bsp.getCmdLineArgs()[0];
			}
			
			//**************** !!!!!!!!!!!!!!!! create the process here !!!!!!!!!!!!!! *********************
			Process newProcess =null;
			try {
				newProcess = Reflection.createProcessFromProcessBuilder(
						bsp.getClassOfMain(),bsp.getVmArgs(),
						bsp.getCmdLineArgs(),bsp.getWorkingDirectory());
				process.set(newProcess);
				logger.info("Process started " + 
						bsp.getAlias() + "," + 
						Arrays.toString(bsp.getCmdLineArgs()) + ","+
						Arrays.toString(bsp.getVmArgs())) ;
			} catch (Exception e1) {
				logger.error(Utils.stackTraceAsString(e1));
				return;
			}

			
			if(bsp.getClassOfMain()!=null){
				BatchRunScheduler.this.processInfoMap.put(alias,this);
			}

			
			int pResponse;
			try {
				//**************** !!!!!!!!!!!!!!!! wait for process to end here !!!!!!!!!!!!!! *********************
				//  (either by just ending, or being killed by a Killer instance of ProcessAsynchStarter)
				pResponse = process.get().waitFor();
				logger.info("Exiting process " + 
						alias + "," + 
						Arrays.toString(bsp.getCmdLineArgs()) + ","+
						Arrays.toString(bsp.getVmArgs()) + ","+
						"process exit code = "+pResponse);
				processInfoMap.remove(alias);
				this.process.set(null);
				
				if(bsp.getRestart() !=null && bsp.getRestart()){
					// create a new bbp with a new calendar for immediate execution
					BatchSchedulerParams newBrp = 
							new BatchSchedulerParams(bsp, 0l, 0l);
					logger.info("Restarting process immediately " + 
							alias + "," + 
							Arrays.toString(bsp.getCmdLineArgs()) + ","+
							Arrays.toString(bsp.getVmArgs()));
					bbpQueue.put(newBrp);
				}
				// exit this thread here
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		
		private void killProcess(boolean restart){
			bsp.setRestart(restart);
			Process p =  process.get();
			if(p!=null){
				// if this is a regular process, kill it
				//   sometimes we actually kill a "killer" process,
				//   which only has a timer
				p.destroy();
			}
			if(!restart){
				// kill the timer too
				timerInfoMap.get(bsp.getAlias()).getTimer().cancel();
			}
		}
		
	}
	
	
	
	
	/**
	 * Start task on a timer
	 * @param brp BatchRunParams
	 * 
	 */
	private void startProcess(BatchSchedulerParams brp){
		try {
			if(brp.getWaitTime()!=null){
				Thread.sleep(brp.getWaitTime());
			}
		} catch (InterruptedException e) {
			logger.error(Utils.stackTraceAsString(e));
		}
		
		if(brp.getScheduleType()!=null){
			// use schedule type
			ScheduleType type = brp.getScheduleType();
			TimerInfoAbstract ti = null;
			switch(type){
			case SCHEDULE_AT_DATE_TIME:
				ti = 
					new TimerInfoScheduleAtDateTime(brp);
				break;
			case SCHEDULE_AT_OFFSET_FROM_TODAY_IN_MILLS:
				ti = 
				new TimerInfoScheduleAtOffsetFromTodayInMills(brp);
				break;
			case SCHEDULE_AS_HHMMSSMMM_FROM_TODAY:
				ti = 
				new TimerInfoScheduleAsTimeFromToday(brp);
				break;
			default:
				logger.error(brp.getAlias() + " : " + ScheduleType.class.getCanonicalName() + " has an invalid value of " + type.toString());
				return;
			}
			this.timerInfoMap.put(brp.getAlias(), ti);
			ti.schedule();
		}else{
			logger.error(brp.getAlias() + " : " + ScheduleType.class.getCanonicalName() + " is null");
		}
	}
	
	/**
	 * This runnable is responsible for starting up creating all
	 *   launching all ProcessAsyncWait threads.   
	 *   See JavaDoc for ProcessAsyncWait.
	 *   
	 * @author bperlman1
	 *
	 */
	private class ProcessSchedulerRunnable implements Runnable{
		private ProcessSchedulerRunnable(){//(BlockingQueue<BatchRunParams> bbpQueue) {
			super();
		}

		@Override
		public void run() {
			while(true){
				try {
					BatchSchedulerParams bsp = bbpQueue.poll(1, TimeUnit.MINUTES);
					System.gc();
					if(bsp!=null){
						logger.info("starting batch process");
						startProcess(bsp);//,bbpQueue);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	private abstract class TimerInfoAbstract {
		protected final BatchSchedulerParams bsp;
		private Timer t = new Timer();
		abstract void schedule();
		
		TimerInfoAbstract(BatchSchedulerParams bsp){
			this.bsp = bsp;
		}
		
		Timer getTimer(){
			return t;
		}
	}
	
	private class TimerInfoScheduleAtDateTime extends TimerInfoAbstract{
		TimerInfoScheduleAtDateTime(BatchSchedulerParams bsp) {
			super(bsp);
		}

		@Override
		void schedule() {
			Long dateOrOffset = bsp.getYyyyMmDdOrOffsetToStart();
			if(dateOrOffset==null){
				logger.error(bsp.getAlias() + BatchSchedulerParams.ScheduleType.SCHEDULE_AT_DATE_TIME + " schedule time of "  + " is null");
				return;
			}
			if(bsp.getHhMmSsMmmToStart()==null){
				logger.error(bsp.getAlias() + BatchSchedulerParams.ScheduleType.SCHEDULE_AT_DATE_TIME + " hh:mm:ss:mmm "  + " is null");
				return;
			}

			Long today = Dates.getYyyyMmDdFromCalendar(Calendar.getInstance());
			// date is either a valid yyyyMmDd long or it's an offset
			
			if(today>dateOrOffset){
				// this is probably an offset
				logger.error(bsp.getAlias() + BatchSchedulerParams.ScheduleType.SCHEDULE_AT_DATE_TIME + " schedule time of " + dateOrOffset + " has passed");
				return;
			}
			
			Long date = null;  // date we will use to create timer
			date = dateOrOffset;
			
			Calendar start = Dates.getCalenderFromYYYYMMDDLong(date);
			int year = start.get(Calendar.YEAR);
			int month = start.get(Calendar.MONTH);
			int day = start.get(Calendar.DAY_OF_MONTH);
			
			int hourOfDay = 0;
			int minute = 0;
			int second = 0;

			String[] hhMMSsMmmToStart = bsp.getHhMmSsMmmToStart().split(":");
			if(hhMMSsMmmToStart.length>0){
				hourOfDay = new Integer(hhMMSsMmmToStart[0]);
			}
			if(hhMMSsMmmToStart.length>1){
				minute =  new Integer(hhMMSsMmmToStart[1]);
			}
			if(hhMMSsMmmToStart.length>2){
				second =  new Integer(hhMMSsMmmToStart[2]);
			}

			start.set(year, month, day, hourOfDay, minute, second);
			Calendar now = Calendar.getInstance();
			if(now.compareTo(start)>0){
				// add a day to start
				logger.error(bsp.getAlias() + BatchSchedulerParams.ScheduleType.SCHEDULE_AT_DATE_TIME + 
						" schedule time of " + start.getTime().toString() + " has passed");
				return;
			}
			
			// get the startTime
			Date startDateTime = start.getTime();
			// get the next schedule time
			//  if it's less than or equal to zero, don't use it
			Long nextScheduleInMills = bsp.getNextScheduleInMills();

			ProcessAsynchStarter pas = new ProcessAsynchStarter(bsp);

			// update process info map, unless this is just a kill process BatchRunParams
			
			if(nextScheduleInMills==null || nextScheduleInMills<=0){
				getTimer().schedule(pas, startDateTime);
			}else{
				getTimer().schedule(pas, startDateTime,nextScheduleInMills);
			}

		}
		
		
		
	}
	
	private class TimerInfoScheduleAtOffsetFromTodayInMills extends TimerInfoAbstract{

		TimerInfoScheduleAtOffsetFromTodayInMills(BatchSchedulerParams bsp) {
			super(bsp);
		}

		@Override
		void schedule() {
			Long offsetInMills = bsp.getYyyyMmDdOrOffsetToStart()==null ? 0 : bsp.getYyyyMmDdOrOffsetToStart();
			
			Long nextScheduleInMills = bsp.getNextScheduleInMills();

			ProcessAsynchStarter pas = new ProcessAsynchStarter(bsp);

			// update process info map, unless this is just a kill process BatchRunParams
			
			if(nextScheduleInMills==null || nextScheduleInMills<=0){
				getTimer().schedule(pas, offsetInMills);
			}else{
				getTimer().schedule(pas, offsetInMills,nextScheduleInMills);
			}
			
		}
		
	}
	
	private class TimerInfoScheduleAsTimeFromToday extends TimerInfoAbstract{

		TimerInfoScheduleAsTimeFromToday(BatchSchedulerParams bsp) {
			super(bsp);
		}

		@Override
		void schedule() {
			Calendar start = Calendar.getInstance();
			int year = start.get(Calendar.YEAR);
			int month = start.get(Calendar.MONTH);
			int day = start.get(Calendar.DAY_OF_MONTH);
			
			int hourOfDay = 0;
			int minute = 0;
			int second = 0;
			if(bsp.getHhMmSsMmmToStart()==null){
				logger.error(bsp.getAlias() + BatchSchedulerParams.ScheduleType.SCHEDULE_AS_HHMMSSMMM_FROM_TODAY + " needs a valid hhMmSsMmm string.  It is null");
				return;
			}

			String[] hhMMSsMmmToStart = bsp.getHhMmSsMmmToStart().split(":");
			if(hhMMSsMmmToStart.length>0){
				hourOfDay = new Integer(hhMMSsMmmToStart[0]);
			}
			if(hhMMSsMmmToStart.length>1){
				minute =  new Integer(hhMMSsMmmToStart[1]);
			}
			if(hhMMSsMmmToStart.length>2){
				second =  new Integer(hhMMSsMmmToStart[2]);
			}
			
			start.set(year, month, day, hourOfDay, minute, second);
			Calendar now = Calendar.getInstance();
			if(now.compareTo(start)>0){
				// add a day to start
				start = Dates.addToCalendar(start, 1, Calendar.DAY_OF_YEAR, true);
			}
			Date startDateTime = start.getTime();
			// get the next schedule time
			//  if it's less than or equal to zero, don't use it
			Long nextScheduleInMills = bsp.getNextScheduleInMills();

			ProcessAsynchStarter pas = new ProcessAsynchStarter(bsp);

			// update process info map, unless this is just a kill process BatchRunParams
			
			if(nextScheduleInMills==null || nextScheduleInMills<=0){
				getTimer().schedule(pas, startDateTime);
			}else{
				getTimer().schedule(pas, startDateTime,nextScheduleInMills);
			}
		}
		
	}
	
	public static void main(String[] args) {
		new LoggingUtils("defaultlog4j.properties", null);
		Utils.prt("usage");
		Utils.prt("-xml batchRunListXmlPath  ");
		Utils.prt("[-clz classOfResource] (to get batchRunListXml file from a resource which contains this class) ");
		Utils.prt("[-red redirectConsoleFlag] (to redirect System out and err to Swing console) ");
		Utils.prt("[-sb serviceBlockString] (like http://localhost,80,http://localhost,5905 for using a local glassfish on port 5905");

		// get pairs
		Map<String, String> argsMap = new HashMap<String, String>();
		for(int i = 0;i<args.length;i=i+2){
			argsMap.put(args[i],args[i+1]);
		}
		String batchRunListXmlPath = argsMap.get("-xml");
		Class<?> classOfResource;
		try {
			classOfResource = argsMap.containsKey("-clz") ? Class.forName(argsMap.get("-clz")) : null;
		} catch (ClassNotFoundException e1) {
			e1.printStackTrace();
			return;
		}
		boolean redirectConsoleFlag = argsMap.containsKey("-red") ? new Boolean(argsMap.get("-red")) : false;
		String commaSeparatedArgs = argsMap.containsKey("-sb") ? argsMap.get("-sb") : null;
		
		if(redirectConsoleFlag) {
			new RedirectedConsoleForJavaProcess(800, 1200, 1, 1, 
					BatchRunScheduler.class.getCanonicalName(),null);
		}
		@SuppressWarnings("unchecked")
		List<BatchSchedulerParams> launchList = Utils.getXmlData(List.class, classOfResource, batchRunListXmlPath);
		BatchRunScheduler pLauncher=null;
		if(commaSeparatedArgs!=null){
			ServiceBlock sb = new ServiceBlock(commaSeparatedArgs);
			pLauncher = 
					new BatchRunScheduler(launchList,sb);
		}else{
			pLauncher = 
					new BatchRunScheduler(launchList);
		}
		try {
			pLauncher.launchList();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	

}
