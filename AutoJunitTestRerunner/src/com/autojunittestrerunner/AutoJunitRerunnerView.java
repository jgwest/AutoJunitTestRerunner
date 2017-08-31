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
import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

public class AutoJunitRerunnerView extends ViewPart {

	Composite innerComposite;

	Text text;
	
	private static Object lock = new Object();
	private static AutoJunitRerunnerView currentActiveView = null;
	private static List<String> addedText = new ArrayList<String>();
	private static boolean viewInitialized = false;
	
	/**
	 * The constructor.
	 */
	public AutoJunitRerunnerView() {
	}

	public static AutoJunitRerunnerView getCurrentActiveView() {
		synchronized(lock) {
			return currentActiveView;
		}
	}
	
	private void createControlInner(Composite c) {
		c.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));

		Text t = new Text(c, SWT.READ_ONLY | SWT.MULTI | SWT.V_SCROLL);
		t.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));				
	
		t.setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WHITE));
		
		t.setText("Auto Junit Test Rerunner v1.31 - https://github.com/jgwest/AutoJunitTestRerunner for more information.\n"
				+ "-----------------------------------------------------------------------------\n");
		
		text = t;

	}
	
	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		
	    ScrolledComposite scrollComposite = new ScrolledComposite(parent, SWT.H_SCROLL | SWT.V_SCROLL);
	    scrollComposite.setExpandHorizontal(true);
	    scrollComposite.setExpandVertical(true);
	    
	    	innerComposite = new Composite(scrollComposite, SWT.NONE);
	    	innerComposite.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
	    	innerComposite.setLayout(new GridLayout(1, false));
	    	
	    	createControlInner(innerComposite);
	    	
	    	innerComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//	    	innerComposite.layout();
	    	
	    	
	    Point p = innerComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
	    scrollComposite.setMinSize(p);
    	
	    scrollComposite.setContent(innerComposite);
	    
	    scrollComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		
	    synchronized(lock) {
	    	viewInitialized = false;
	    	currentActiveView = this;
	    }
	    update(null);
	}

	@Override
	public void setFocus() {
//		update(null);
	}
	
	public static void addLine(final String str) {
		System.out.println("Writing text: "+str);
		AutoJunitRerunnerView inst;
		synchronized(lock) {
			addedText.add(str);
			
			inst = currentActiveView;
			
		}
		if(inst != null) {
			inst.update(str);
		}		
	}

	private void update(final String str2) {
		Display.getDefault().syncExec(new Runnable() {
			@Override
			public void run() {
				synchronized(lock) {
					if(text == null || text.isDisposed()) {
						return;
					}
					
					if(!viewInitialized) {
						for(String str : addedText) {
							text.append(str+"\n");
						}
						viewInitialized = true;
					} else {
						if(str2 != null) {
							text.append(str2+"\n");
						}
					}
				}
				
				text.getParent().layout();
				text.getParent().getParent().layout();
			}
		});
		
	}
}