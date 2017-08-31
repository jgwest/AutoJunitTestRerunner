/*
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License. 
*/

package com.autojunittestrerunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.HandlerUtil;

import com.autojunittestrerunner.AutoTestRunListener.TestCompleteReturn;

public class JunitActionHandler extends AbstractHandler {
	
	public static final boolean DEBUG = false; 
	
	/**
	 * The constructor.
	 */
	public JunitActionHandler() {
	}

	
	private static AutoTestRunListener stats = AutoTestRunListener.getInstance();
	
	private static Object lock = new Object();
	
	private static ReRunThreadNew activeThread = null; 
	
	public static AutoTestRunListener getStats() {
		return stats;
	}

	
	/**
	 * The command has been executed, so extract the needed information
	 * from the application context.
	 */
	public Object execute(ExecutionEvent event) throws ExecutionException {

		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		
		try {
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("AutoJunitTestRerunner.AutoJunitRerunnerView");
		} catch (PartInitException e1) {
			/* ignore*/
			e1.printStackTrace();
		}
		
		
		TextDialog td = new TextDialog(window.getShell());
		int result = td.open();
		
		if(result == Window.CANCEL) {
			return null;
		}
		
		try {
		
			String testNameSubstring = td.getTestNameSubstring().toLowerCase();
			
			int timeToWaitBetweenLaunchesInSecs = Integer.parseInt(td.getPostTestDelay().trim());
			
			boolean keepTrying = td.isKeepTryingValue();
			
			boolean openDialogOnFail = td.isOpenDialogOnFail();
			
			boolean debugMode = td.isDebugMode();
			
			
			ILaunchManager m = DebugPlugin.getDefault().getLaunchManager();
	
			ILaunchConfiguration[] lc = m.getLaunchConfigurations();
			
			List<ILaunchConfiguration> list = new ArrayList<ILaunchConfiguration>(); 

			String matches = "";
			
			for(ILaunchConfiguration e : lc) {
				String name = e.getName().toLowerCase();
				
				if(name.contains(testNameSubstring)) {
					list.add(e);
					matches += "["+e.getName()+"]";
				}
			}

			
			if(list.size() == 0) {
				MessageDialog.openError(window.getShell(), "Error", "No launch configuration found that matched that name.");
				return null;
			} else if(list.size() > 1) {
				MessageDialog.openError(window.getShell(), "Error", "More than one launch configuration matched that name: "+matches);
				return null;
			} 

			synchronized(lock) {
				if(activeThread != null) {
					activeThread.setThreadKilled(true);
					activeThread.interrupt();
					
				} 

				ReRunThreadNew thread = new ReRunThreadNew(list.get(0), timeToWaitBetweenLaunchesInSecs, keepTrying, openDialogOnFail, debugMode);
				
				activeThread = thread;
				thread.start();

			}
						
		} catch(Throwable t2) {
			MessageDialog.openError(window.getShell(), "Error", t2.getClass()+" "+t2.getMessage());
			t2.printStackTrace();
		}
		
		
		return null;
	}

}


class ReRunThreadNew extends Thread {

	private final ILaunchConfiguration launchConfig;
	
	private final long timeToWaitBetweenLaunchesInSecs;
	
	private final boolean keepTryingAfterFail;
	
	private final boolean openDialogOnFail;
	
	private final boolean debugMode;
	

	private boolean threadKilled = false;
	
	
	public ReRunThreadNew(ILaunchConfiguration launchConfig, long timeToWaitBetweenLaunchesInSecs, boolean keepTryingAfterFail, boolean openDialogOnFail, boolean debugMode) {
		super(ReRunThreadNew.class.getName());
		setDaemon(true);
		this.launchConfig = launchConfig;
		this.timeToWaitBetweenLaunchesInSecs = timeToWaitBetweenLaunchesInSecs = Math.max(15, timeToWaitBetweenLaunchesInSecs);
		this.keepTryingAfterFail = keepTryingAfterFail;
		this.openDialogOnFail = openDialogOnFail;
		this.debugMode = debugMode;
		
	}

	
	public static void out(String str) {
		System.out.println(str);
		AutoJunitRerunnerView.addLine(str);
	}

	
	@Override
	public void run() {
		
		JunitActionHandler.getStats().reset();
		
		out("");
		out(launchConfig.getName()+":");
		out("[0] Launching: "+(new Date()));
		
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				try {
					System.out.println("* launch called.");
					DebugUITools.launch(launchConfig, debugMode ? ILaunchManager.DEBUG_MODE : ILaunchManager.RUN_MODE);
				} catch(Throwable t) {
					t.printStackTrace();

					String text = "Eclipse's DebugUITools threw an NPE... this is a known issue w/ some versions of Eclipse, restart the workbench (and blame Eclipse.)";
					out(text);
					Shell shell = Display.getDefault().getActiveShell();
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Auto Junit Test Rerunner", text);
					shell.forceActive();
					shell.setFocus();

				}
			}
		});
		
		
		int totalSuccessfulRuns = 0;
		
		int count = 0;

		boolean currOpenDialogOnFail = openDialogOnFail; 
				
		TestCompleteReturn isTestComplete = null;
		while(!threadKilled) {
			
			try {
				TimeUnit.SECONDS.sleep(1);
				count+=1;
			} catch (InterruptedException e) {
				break;
			}
		
			try {
				
				isTestComplete = isTestComplete();
				int errorsValue = getErrorsValue();
				int failuresValue = getFailuresValue();
				
				if(count % 120 == 0) {
					out("["+totalSuccessfulRuns+"] status: "+isTestComplete  + " errors: "+errorsValue + " failures: "+failuresValue + "   total-seconds-elapsed: "+count+" total-successful-runs: "+totalSuccessfulRuns);
				}
				
				if(currOpenDialogOnFail &&(errorsValue > 0 || failuresValue > 0)	) {
					currOpenDialogOnFail = false;
					Display.getDefault().syncExec(new Runnable() {
						@Override
						public void run() {
							MessageDialog.openError(Display.getDefault().getActiveShell(), "Auto Junit Test Rerunner", "Test case has failed.");
						}
					});
				}
				
				if(isTestComplete == TestCompleteReturn.COMPLETE_TERMINATED) {
					// Usually this is because the user terminated it
					break;
				}
				
				if(isTestComplete == TestCompleteReturn.COMPLETE_FINISHED) {
					
					if(getErrorsValue() > 0 || getFailuresValue() > 0) {
						// Failed!!!
						break;
					} else {
						
						JunitActionHandler.getStats().reset();
						
						System.out.println("waiting: "+timeToWaitBetweenLaunchesInSecs);
						TimeUnit.SECONDS.sleep(timeToWaitBetweenLaunchesInSecs);
						
						// Run it again!!!!
						Display.getDefault().syncExec(new Runnable() {
								public void run() {
									System.out.println("* launch called.");
									DebugUITools.launch(launchConfig, debugMode ? ILaunchManager.DEBUG_MODE : ILaunchManager.RUN_MODE);	
								}
						});
						
						totalSuccessfulRuns ++;
						out("["+totalSuccessfulRuns+"] Launching: "+(new Date()));
						
					}
					
					
				}
			} catch(Throwable t) {
				t.printStackTrace();
			}
			
		} // end while loop
		
		if(threadKilled) {
			return;
		}
		

		if(isTestComplete == TestCompleteReturn.COMPLETE_TERMINATED || isTestComplete == null) {
			out("User terminated. ["+isTestComplete+"]");
			return;
		}
		
		final int total = totalSuccessfulRuns;
		
		if(keepTryingAfterFail) {
			
			out("Test case failed. Number of consecutive successful test runs (before failure): "+total);
			
			File f = logFailuresAndConsoleToFile();
			if(f != null) {
				out("Test failures and console logged to:");
				out(f.getPath());
			}
			
			try { TimeUnit.SECONDS.sleep(timeToWaitBetweenLaunchesInSecs); } catch (InterruptedException e) { e.printStackTrace(); }
			
			run();
			return;
			
		} else {
		
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Auto Junit Test Rerunner Statistics", "Number of consecutive successful test runs (before first failure): "+total);		
				}
			});
		}
		
		
		
	}

	private File  logFailuresAndConsoleToFile() {
		final String CRLF = "\r\n";
		
		File tempFile = null;
		try {
			tempFile = File.createTempFile("AutoJunitTestRerunner-test-"+System.currentTimeMillis()+"", ".txt");
			FileWriter fw = new FileWriter(tempFile);
			
			String consoleContents = AutoTestRunListener.getConsoleContents();
			if(consoleContents == null) {
				consoleContents = "";
			}
			
			List<String> failedTestKeys = AutoTestRunListener.getInstance().getFailedTestKeys();
			
			Map<String, String> failureTraces = AutoTestRunListener.getInstance().getFailedTestStackTraceMap();
			
			for(String key : failedTestKeys) {
				
				String trace = failureTraces.get(key);
				
				fw.write(key+":"+CRLF);
				if(trace != null) {
					fw.write(trace);	
				}
				
				fw.write(CRLF+CRLF);
				
			}
	
			fw.write("Console: "+CRLF+CRLF);
			fw.write(consoleContents);
			
			fw.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return tempFile;
		
	}
	
	private TestCompleteReturn isTestComplete() {
		return JunitActionHandler.getStats().getCurrentStatus();
	}
	
	private Integer getErrorsValue() {
		
		return JunitActionHandler.getStats().getTestsErrored();
	
	}

	private Integer getFailuresValue() {
		
		return JunitActionHandler.getStats().getTestsFailed();
				
	}

	public void setThreadKilled(boolean threadKilled) {
		this.threadKilled = threadKilled;
	}
	
}