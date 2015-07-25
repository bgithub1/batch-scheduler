package com.billybyte.batchscheduler;

import java.awt.HeadlessException;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.billybyte.commonstaticmethods.Dates;
import com.billybyte.commonstaticmethods.Reflection;
import com.billybyte.commonstaticmethods.RegexMethods;
import com.billybyte.commonstaticmethods.Utils;
import com.billybyte.ui.RedirectedConsoleForJavaProcess;
import com.billybyte.ui.messagerboxes.MessageBox;
import com.billybyte.ui.messagerboxes.MessageBox.MessageBoxNonModalWithTextBox;
/**
 * This class runs the mains of programs, and either waits for there competion
 *    or waits a specified time to launch the next process.
 * @author bperlman1
 *
 */
public class BatchRun {
	private final Map<Integer, Process> processIdToProcessMap = 
			new ConcurrentHashMap<Integer, Process>();
	
	/**
	 * 
	 * @param batchRunListXmlPath - an xml file path that has a list of 
	 * 	BatchRunParams instances.
	 * 
	 * @throws IOException
	 * @throws Exception
	 */
	public void launchList(
			String batchRunListXmlPath,
			boolean showMessageBoxAfterEachLaunch) throws IOException, Exception{
		
		
		@SuppressWarnings("unchecked")
		List<BatchRunParams> launchList = Utils.getXmlData(List.class, null, batchRunListXmlPath);
		for(int i = 0;i<launchList.size();i++){
			BatchRunParams bbp=launchList.get(i);
			String className = bbp.getClassOfMain().getCanonicalName();
			if(bbp.getClassOfMain().equals(BeansLaunch.class)){
				className = className + " : "+bbp.getCmdLineArgs()[0];
			}
			if(showMessageBoxAfterEachLaunch){
				String trueFalse = MessageBox.MessageBoxNoChoices("Execute Step "+i+ " Class: "+className, "true");
				if(trueFalse.compareTo("true")!=0)continue;
			}
			Process process=null;
			try {
				process = Reflection.createProcessFromProcessBuilder(
						bbp.getClassOfMain(),bbp.getVmArgs(),
						bbp.getCmdLineArgs(),bbp.getWorkingDirectory());
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			if(process==null)continue;
			if(bbp.getWaitTime()!=null){
				this.processIdToProcessMap.put(i, process);
				new Thread(new ProcessAsynchWait(process)).start();
				Utils.prtObMess(this.getClass(), "Launching processNum: "+i+". Kill using MessageBox");
				Thread.sleep(bbp.getWaitTime());
			}else{
				int pResponse;
				try {
					pResponse = process.waitFor();
					Utils.prt("process exit code = "+pResponse);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	
	
	public void launchList(String batchRunListXmlPath) throws IOException, Exception{
		launchList(batchRunListXmlPath,true);
	}
	
	public void killProcess(Integer processNumToKill){
		Process process = this.processIdToProcessMap.get(processNumToKill);
		if(process==null){
			Utils.prtObErrMess(this.getClass(), "no process for processNum: "+processNumToKill);
		}else{
			process.destroy();
			Utils.prtObMess(this.getClass(), "processNum: "+ processNumToKill + " has been destroyed" );
		}
		return;
	}
	
	/**
	 * 
	 * @param args
	 * 	@param 	String batchRunListXmlPath = args[0];
		@param boolean redirectConsoleFlag = (args.length>1) ? Boolean.parseBoolean(args[1]) : false;
		@param boolean showMsgBoxBeforeEachLaunch = args.length>2 ? new Boolean(args[2]) : redirectConsoleFlag;
		@param boolean waitForTimer = args.length>3 ? new Boolean(args[3]) : false;

	 */
	public static void main(String[] args) {
		String batchRunListXmlPath = args[0];
		boolean redirectConsoleFlag = (args.length>1) ? Boolean.parseBoolean(args[1]) : false;
		boolean showMsgBoxBeforeEachLaunch = args.length>2 ? new Boolean(args[2]) : redirectConsoleFlag;
		boolean waitForTimer = args.length>3 ? new Boolean(args[3]) : false;
		
		if(waitForTimer){
			while(true){
				Calendar c = Calendar.getInstance();
				if(!Dates.isBusinessDay("US", c))break;
				int hour = c.get(Calendar.HOUR_OF_DAY);
				int min = c.get(Calendar.MINUTE);
				if(hour>=19){
					if(hour>19) break;
					if(min>=30)break;
				}
				
				try {
					Thread.sleep(5*60*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		
		if(redirectConsoleFlag) {
			new RedirectedConsoleForJavaProcess(800, 1200, 1, 1, 
					BatchRun.class.getCanonicalName(),null);
		}
		BatchRun pLauncher = 
				new BatchRun();
		try {
			pLauncher.launchList(batchRunListXmlPath,showMsgBoxBeforeEachLaunch);
			if(redirectConsoleFlag) new KillMessBox(pLauncher);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	private static class ProcessAsynchWait implements Runnable{
		private final Process process;
		private ProcessAsynchWait(Process process) {
			super();
			this.process = process;
		}

		
		@Override
		public void run() {
			int pResponse;
			try {
				pResponse = process.waitFor();
				Utils.prt("process exit code = "+pResponse);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	@SuppressWarnings("serial")
	private static final class KillMessBox extends MessageBoxNonModalWithTextBox{
		private final BatchRun launcher;
		public KillMessBox(BatchRun launcher) throws HeadlessException {
			super("Enter process numbers to kill", "Kill Process", "0,1", true);
			this.launcher = launcher;
		}

		@Override
		protected void processCommaSepValuesAndDisplay(
				String[] messageBoxResponseCommaSepValues) {
			if(messageBoxResponseCommaSepValues==null)return;
			if(messageBoxResponseCommaSepValues.length<1)return;
			for(String pNumString : messageBoxResponseCommaSepValues){
				if(!RegexMethods.isNumber(pNumString))continue;
				int pNum = new Integer(pNumString);
				launcher.killProcess(pNum);
			}
		}

		@Override
		protected void processCsvDataAndDisplay(List<String[]> csvData) {
		}

		@Override
		protected MessageBoxNonModalWithTextBox newInstance() {
			return new KillMessBox(this.launcher);
		}
		
	}

}
