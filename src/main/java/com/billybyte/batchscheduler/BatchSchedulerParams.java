package com.billybyte.batchscheduler;

import java.util.HashSet;
import java.util.Set;

public class BatchSchedulerParams {
	public enum ScheduleType {
		SCHEDULE_AT_DATE_TIME,
		SCHEDULE_AS_HHMMSSMMM_FROM_TODAY,
		SCHEDULE_AT_OFFSET_FROM_TODAY_IN_MILLS,
	}

	private final Class<?> classOfMain;
	private final String[] cmdLineArgs;
	private final String[] vmArgs;
	private final String workingDirectory;
	private final Long waitTime;
	private final String alias;
	private final String aliasToKill;

	private  Object restart_Lock = new Object();
	private boolean restart;
	private final Long yyyyMmDdOrOffsetToStart;
	private final String hhMmSsMmmToStart;
	private final Long nextScheduleInMills;
	private final Set<Integer> validDays;
	private final ScheduleType scheduleType;
	
	public Class<?> getClassOfMain() {
		return classOfMain;
	}
	public String[] getCmdLineArgs() {
		return cmdLineArgs;
	}
	public String[] getVmArgs() {
		return vmArgs;
	}
	public String getWorkingDirectory() {
		return workingDirectory;
	}
	public Long getWaitTime(){
		return this.waitTime;
	}
	
	public String getAlias(){
		return this.alias;
	}

	public Boolean getRestart(){
		synchronized (restart_Lock) {
			return restart;
		}
	}
	
	public void setRestart(Boolean restart){
		synchronized (restart_Lock) {
			this.restart = restart;
		}
	}
	
	
	public Long getYyyyMmDdOrOffsetToStart() {
		return yyyyMmDdOrOffsetToStart;
	}
	public String getHhMmSsMmmToStart() {
		return hhMmSsMmmToStart;
	}
	
	public Set<Integer> getValidDays() {
		if(this.validDays ==null)return null;
		return new HashSet<Integer>(this.validDays);
	}
	public Long getNextScheduleInMills() {
		return nextScheduleInMills;
	}
	
	
	public String getAliasToKill() {
		return aliasToKill;
	}
	public ScheduleType getScheduleType() {
		return scheduleType;
	}
	/**
	 * 
	 * @param scheduleType ScheduleType (see ScheduleType enum)
	 * @param classOfMain - Class<?> the class of that has a main to run<br>
	 * 		If
	 * @param cmdLineArgs - String[] the  command line args of main
	 * @param vmArgs String[] vm args of main
	 * @param workingDirectory String 
	 * @param waitTime Long time to wait before launching
	 * @param alias - String name used to identify process in BatchRunScheduler
	 * @param restart - AtomicBoolean set to either true to restart the main<br> 
	 * 			after it does a System.exit, or<br>
	 * 			to not restart it
	 * @param yyyyMmDdOrOffsetToStart Long that is one of 3 things<br>
	 * 
	 * 	1	if scheduleType = SCHEDULE_AT_DATE_TIME<br>
	 * 			yyyyMmDdOrOffsetToStart is a yyyyMmDd Long date - like 20130812<br>
	 *  2	if scheduleType = SCHEDULE_AS_TIME_FROM_TODAY<br>  
	 *  		yyyyMmDdOrOffsetToStart is an offset of days from today, like 0 for no offset, or 1 for a one day offset<br>
	 *  3	if scheduleType = SCHEDULE_AT_OFFSET_FROM_TODAY_IN_MILLS<br>  
	 *  		yyyyMmDdOrOffsetToStart is an offset from now, in mills, to start this process<br>
	 *  	
	 * @param hhMmSsMmmToStart String like 14:22:13:002 for 2:22:13 pm and 2 milliseconds<br>
	 * 		hhMmSsMmmToStart can be null when scheduleType = SCHEDULE_AT_OFFSET_FROM_TODAY_IN_MILLS<br>
	 * @param nextScheduleInMills - Long mills before the rescheduled run<br>
	 * 		If this is null, then there is no rescheduling<br>
	 * 		No re-running occurs if the main that is run is still running<br>
	 * @param validDays - List<Integer> of valid days to launch
	 */
	public BatchSchedulerParams(
			ScheduleType scheduleType,
			Class<?> classOfMain, 
			String[] cmdLineArgs,
			String[] vmArgs, 
			String workingDirectory,
			Long waitTime,
			String alias,
			Boolean restart,
			Long yyyyMmDdOrOffsetToStart,
			String hhMmSsMmmToStart,
			Long nextScheduleInMills,
			Set<Integer> validDays,
			String aliasToKill) {
		super();
		this.scheduleType = scheduleType;
		this.classOfMain = classOfMain;
		this.cmdLineArgs = cmdLineArgs;
		this.vmArgs = vmArgs;
		this.workingDirectory = workingDirectory;
		this.waitTime = waitTime;
		this.alias = alias;
		this.restart = restart;
		this.yyyyMmDdOrOffsetToStart = yyyyMmDdOrOffsetToStart;
		this.hhMmSsMmmToStart = hhMmSsMmmToStart;
		this.nextScheduleInMills  = nextScheduleInMills;
		this.validDays = validDays;
		this.aliasToKill = aliasToKill;
	}
	
	
	/**
	 * Constructor to make a clone with new yyyyMmDdOfRestart and
	 *  new hhMmSsMmmOfRestart;
	 *  
	 * @param brp BatchRunParams
	 * @param yyyyMmDdOfRestart Long - like 20130805 for 08/05/2013
	 */
	public BatchSchedulerParams(BatchSchedulerParams brp,
			Long yyyyMmDdOfRestart,
			Long nextScheduleInMills){
		this(BatchSchedulerParams.ScheduleType.SCHEDULE_AT_OFFSET_FROM_TODAY_IN_MILLS,brp.getClassOfMain(), brp.getCmdLineArgs(), brp.getVmArgs(), 
				brp.getWorkingDirectory(), brp.getWaitTime(), brp.getAlias(), brp.getRestart(),
				yyyyMmDdOfRestart,null,nextScheduleInMills,brp.validDays ,
				brp.aliasToKill);
		
	}
	
	  private Object readResolve() {
	    this.restart_Lock = new Object();
	    return this;
	  }

}
