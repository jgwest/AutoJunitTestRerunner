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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class TextDialog extends Dialog {
	
	private Text testField;
	private String testTextStr = "";
	private static String lastTestValue = ValueDB.getInstance().getValue("testValue");
	
	private Text postTestDelay;
	private String postTestDelayTextStr = "";
	private static String lastDelayValue = ValueDB.getInstance().getValue("delayValue");;
	
	private Button keepTryingBox;
	private boolean keepTryingValue = false;
	private static boolean lastTryingValue = (ValueDB.getInstance().getValue("tryingValue") != null ? Boolean.parseBoolean(ValueDB.getInstance().getValue("tryingValue")): false)		;

	private Button openDialogOnFailBox;
	private boolean openDialogOnFailValue = false;
	private static boolean lastOpenDialogOnFailValue = (ValueDB.getInstance().getValue("openDialogOnFailValue") != null ? Boolean.parseBoolean(ValueDB.getInstance().getValue("openDialogOnFailValue")): false)		;

	
	private Button debugModeBox;
	private boolean debugModeValue = true;
	private static boolean lastDebugModeValue = (ValueDB.getInstance().getValue("debugModeValue") != null ? Boolean.parseBoolean(ValueDB.getInstance().getValue("debugModeValue")): true);
	
	public TextDialog(Shell parentShell) {
		super(parentShell);
	}

	@Override
	protected Control createDialogArea(Composite parent) {
		
		parent.getShell().setImage(Activator.getDefault().getImage("/icons/sample.png"));
		parent.getShell().setText("Test Run Options");

		Composite container = (Composite) super.createDialogArea(parent);
		GridLayout layout = new GridLayout(2, false);
		layout.marginRight = 5;
		layout.marginLeft = 10;
		container.setLayout(layout);

		Label lblUser = new Label(container, SWT.NONE);
		lblUser.setText("Test Name Substring:");

		testField = new Text(container, SWT.BORDER);
		testField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		testField.setText(lastTestValue != null ? lastTestValue : "");

		Label lblPostTestDelay = new Label(container, SWT.NONE);
		lblPostTestDelay.setText("Time to wait between test launches (in seconds):");

		postTestDelay= new Text(container, SWT.BORDER);
		postTestDelay.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		postTestDelay.setText(lastDelayValue != null ? lastDelayValue : "0");

		
		Label lblKeepTrying = new Label(container, SWT.NONE);
		lblKeepTrying.setText("Keep trying after fail, and record fail %:");
		
		keepTryingBox = new Button(container, SWT.CHECK);
		keepTryingBox.setSelection(lastTryingValue);

		// Open dialog
		Label lblOpenDialogOnFail = new Label(container, SWT.NONE);
		lblOpenDialogOnFail.setText("Open dialog immediately on test case fail:");
		
		openDialogOnFailBox = new Button(container, SWT.CHECK);
		openDialogOnFailBox.setSelection(lastOpenDialogOnFailValue);

		// Debug mode
		Label lblDebugMode = new Label(container, SWT.NONE);
		lblDebugMode.setText("Run tests w/ Eclipse Debug mode (recommended):");
		
		debugModeBox = new Button(container, SWT.CHECK);
		debugModeBox.setSelection(lastDebugModeValue);

		
		
		postTestDelay.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
		
		return container;
	}

	@Override
	protected Point getInitialSize() {
		return new Point(500, 250);
	}

	@Override
	protected void okPressed() {
		testTextStr = testField.getText();
		lastTestValue = testTextStr;
		ValueDB.getInstance().putValue("testValue", testTextStr);

		postTestDelayTextStr = postTestDelay.getText();
		lastDelayValue = postTestDelayTextStr;
		ValueDB.getInstance().putValue("delayValue", postTestDelayTextStr);

		keepTryingValue = keepTryingBox.getSelection();
		lastTryingValue = keepTryingValue;
		ValueDB.getInstance().putValue("tryingValue", ""+keepTryingValue);

		openDialogOnFailValue = openDialogOnFailBox.getSelection();
		lastOpenDialogOnFailValue= openDialogOnFailValue;
		ValueDB.getInstance().putValue("openDialogOnFailValue", ""+openDialogOnFailValue);

		debugModeValue = debugModeBox.getSelection();
		lastDebugModeValue= debugModeValue;
		ValueDB.getInstance().putValue("debugModeValue", ""+debugModeValue);
		
		super.okPressed();
	}

	public String getTestNameSubstring() {
		return testTextStr;
	}

	public String getPostTestDelay() {
		return postTestDelayTextStr;
	}
	
	public boolean isKeepTryingValue() {
		return keepTryingValue;
	}
	
	public boolean isOpenDialogOnFail() {
		return openDialogOnFailValue;
	}
	
	public boolean isDebugMode() {
		return debugModeValue;
	}

}