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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jdt.junit.JUnitCore;
import org.eclipse.jdt.junit.TestRunListener;
import org.eclipse.jdt.junit.model.ITestCaseElement;
import org.eclipse.jdt.junit.model.ITestElement;
import org.eclipse.jdt.junit.model.ITestElement.ProgressState;
import org.eclipse.jdt.junit.model.ITestElement.Result;
import org.eclipse.jdt.junit.model.ITestRunSession;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;

public class AutoTestRunListener extends TestRunListener {

	private static final AutoTestRunListener instance = new AutoTestRunListener();
	
	public static enum TestCompleteReturn { COMPLETE_FINISHED, COMPLETE_TERMINATED, NOT_COMPLETE};
	
	private AutoTestRunListener() {
		JUnitCore.addTestRunListener(this);
	}
	
	public static AutoTestRunListener getInstance() {
		return instance;
	}
	
	// ---------
	
	private TestCompleteReturn currStatus = TestCompleteReturn.NOT_COMPLETE;
	private int testsRun = 0;
	private int testsFailed = 0;
	private int testsErrored = 0;
	
	
	// synchronize on access; acquire failedTestStackTraceMap _first_ before acquiring this IF you are acquiring both -- this is  to prevent deadlock
	// keys are sorted chronologically
	private List<String> failedTestKeys = new ArrayList<String>();
	
	// synchronize on access
	private HashMap<String /** failing test name */,  /*failure stack trace*/ String> failedTestStackTraceMap = new HashMap<String, String>();
	
	private ITestRunSession currSession;
	
	public void reset() {
		currStatus = TestCompleteReturn.NOT_COMPLETE;
		testsRun = 0;
		testsFailed = 0;
		testsErrored = 0;

		currSession = null;
		
		synchronized(failedTestStackTraceMap) {
			
			failedTestStackTraceMap.clear();
			
			synchronized(failedTestKeys) {
				failedTestKeys.clear();
			}
		}
		
	}
	
	public TestCompleteReturn getCurrentStatus() {
		
		// If the status is not complete, check the session's status directly 
		if(currStatus == TestCompleteReturn.NOT_COMPLETE && currSession != null) {			
			ITestElement.ProgressState progressState = currSession.getProgressState();
			
			// Don't check for COMPLETED here.
			
			
			if(JunitActionHandler.DEBUG) {
				System.out.println("gcs: "+progressState.toString());	
			}
			if(progressState == ProgressState.STOPPED) {
				currStatus = TestCompleteReturn.COMPLETE_TERMINATED;
			}
			
		}
		return currStatus;
	}
	
	
	@Override
	public void sessionLaunched(ITestRunSession session) {
		reset();
		currSession = session;

		if(JunitActionHandler.DEBUG) {
			System.out.println("sessionLaunched "+new Date() );
		}
		
	}

	@Override
	public void sessionStarted(ITestRunSession session) {
		if(JunitActionHandler.DEBUG) {
			System.out.println("sessionStarted "+new Date());
		}
	}

	
	@Override
	public void sessionFinished(ITestRunSession session) {
		ProgressState ps = session.getProgressState();
		
		if(ps == ProgressState.STOPPED) {
			currStatus = TestCompleteReturn.COMPLETE_TERMINATED;
		} else {
			
			currStatus = TestCompleteReturn.COMPLETE_FINISHED;
			
		}

		if(testsErrored == 0 && testsFailed == 0) {
			ITestElement.Result r = session.getTestResult(true);
			
			if(r == Result.ERROR) {
				testsErrored++;
			} else if(r == Result.FAILURE) {
				testsFailed++;
			}
			
		}
		
		currSession = null;
			
		if(JunitActionHandler.DEBUG) {
			System.out.println("sessionFinished: "+session.getProgressState()+" "+new Date());
		}
		
	}

	@Override
	public void testCaseStarted(ITestCaseElement testCaseElement) {
		if(JunitActionHandler.DEBUG) {
			System.out.println("testCaseStarted "+new Date());
		}
	}


	@Override
	public void testCaseFinished(ITestCaseElement testCaseElement) {
		ITestCaseElement.Result r = testCaseElement.getTestResult(false);

		boolean testErrorOrFail = false;
		
		testsRun++;
		if(r == ITestCaseElement.Result.ERROR) {
			testsErrored++;
			testErrorOrFail = true;
			
		} else if(r == ITestCaseElement.Result.FAILURE) {
			testErrorOrFail = true;
			testsFailed++;
			
		} else if(r == ITestCaseElement.Result.OK) {
			// ignore.
		}

		if(testErrorOrFail) {
			
			synchronized(failedTestStackTraceMap) {
				
				String key = testCaseElement.getTestClassName()+"."+testCaseElement.getTestMethodName();
				failedTestStackTraceMap.put(key, testCaseElement.getFailureTrace().getTrace());
				
				synchronized(failedTestKeys) {
					
					failedTestKeys.add(key);
					
				}
			}
			
		}
		
		if(JunitActionHandler.DEBUG) {
			System.out.println("testCaseFinished "+new Date());
		}
	}

	
	public int getTestsRun() {
		return testsRun;
	}
	
	public int getTestsErrored() {
		return testsErrored;
	}
	
	public int getTestsFailed() {
		return testsFailed;
	}
	
	
	public static String getConsoleContents() {
		org.eclipse.ui.console.IConsole console = DebugUITools.getConsole(DebugUITools.getCurrentProcess());

		final String[] consoleContents = new String[1];
		
		if(console instanceof org.eclipse.debug.ui.console.IConsole) {
			final org.eclipse.debug.ui.console.IConsole cast = (org.eclipse.debug.ui.console.IConsole)console;
			Display.getDefault().syncExec(new Runnable() {
				@Override
				public void run() {
					
					IDocument doc = cast.getDocument();
					consoleContents[0] = doc.get();
				}
				
			});
			
		}

		
		return consoleContents[0];
	}
	
	/** Return a copy of the failed test stack trace map*/
	public Map<String, String> getFailedTestStackTraceMap() {
		synchronized(failedTestStackTraceMap) {
			HashMap<String, String> result = new HashMap<String, String>();
			
			for(Map.Entry<String, String> entry : failedTestStackTraceMap.entrySet()) {
				result.put(entry.getKey(), entry.getValue());
			}
			
			return result;
			
		}
		
		
	}
	
	public List<String> getFailedTestKeys() {
		List<String> result = new ArrayList<String>();
		
		synchronized(failedTestKeys) {
			result.addAll(failedTestKeys);
		}
		
		return result;
	}
	
}
