package com.billybyte.batchscheduler;


public class BatchRunParams {
	private final Class<?> classOfMain;
	private final String[] cmdLineArgs;
	private final String[] vmArgs;
	private final String workingDirectory;
	private final Long waitTime;
	
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
	
	
	public BatchRunParams(
			Class<?> classOfMain, String[] cmdLineArgs,
			String[] vmArgs, String workingDirectory,
			Long waitTime) {
		super();
		this.classOfMain = classOfMain;
		this.cmdLineArgs = cmdLineArgs;
		this.vmArgs = vmArgs;
		this.workingDirectory = workingDirectory;
		this.waitTime = waitTime;

	}
	
	
}
