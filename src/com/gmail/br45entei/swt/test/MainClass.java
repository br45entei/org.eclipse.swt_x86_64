package com.gmail.br45entei.swt.test;

import com.gmail.br45entei.data.Property;
import com.gmail.br45entei.swt.Functions;
import com.gmail.br45entei.util.StringUtil;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Spinner;
import org.eclipse.wb.swt.SWTResourceManager;

/** @author Brian_Entei */
public class MainClass {
	//XXX Copy/paste code for future laziness:
	/*private static final Property<Shell> updateUI = new Property<Shell>().setRunnable(new Runnable() {
		@Override
		public final void run() {
			final Shell shell = updateUI.getValue();
			while(!shell.isDisposed()) {
				shell.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if(shell.isDisposed()) {
							return;
						}
						updateUI();
					}
				});
				Functions.sleep();
			}
		}
	});
	
	static {
		Thread updateUIThread = new Thread(updateUI.setValue(shell));
		updateUIThread.setDaemon(true);
		updateUIThread.start();
	}*/
	
	protected static ProgressBar pb1;
	protected static Button buttonEnableButton;
	protected static Button buttonButton;
	protected static Button buttonThreadedEnableButton;
	protected static Button buttonThreadedButton;
	protected static StyledText styledText;
	protected static Spinner spinner;
	
	/** @param args Program command line arguments */
	public static void main(String[] args) {
		Display display = new Display();
		Shell shell = new Shell(display);
		shell.setSize(451, 427);
		shell.setMinimumSize(shell.getSize());
		shell.setText("Simultanious Resizable/Updatable SWT Window Demo");
		//shell.setLayout(new GridLayout());
		
		pb1 = new ProgressBar(shell, SWT.SMOOTH);
		pb1.setSize(414, 17);
		pb1.setLocation(10, 10);
		//pb1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pb1.setMinimum(0);
		pb1.setMaximum(10000);
		
		//new Thread(updateUI.setValue(shell)).start();
		new LongRunningOperation(shell).start();
		
		buttonEnableButton = new Button(shell, SWT.CHECK);
		buttonEnableButton.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(final SelectionEvent e) {
				buttonButton.setEnabled(buttonEnableButton.getSelection());
			}
		});
		buttonEnableButton.setBounds(10, 33, 185, 25);
		buttonEnableButton.setText("Enable button:");
		
		buttonButton = new Button(shell, SWT.NONE);
		buttonButton.setBounds(201, 33, 75, 25);
		buttonButton.setText("Button");
		
		buttonThreadedEnableButton = new Button(shell, SWT.CHECK);
		buttonThreadedEnableButton.setBounds(10, 64, 185, 25);
		buttonThreadedEnableButton.setText("Threaded enable button:");
		
		buttonThreadedButton = new Button(shell, SWT.NONE);
		buttonThreadedButton.setBounds(201, 64, 75, 25);
		buttonThreadedButton.setText("Button");
		
		styledText = new StyledText(shell, SWT.BORDER | SWT.FULL_SELECTION | SWT.READ_ONLY | SWT.WRAP | SWT.V_SCROLL);
		styledText.setBackground(SWTResourceManager.getColor(SWT.COLOR_BLACK));
		styledText.setForeground(SWTResourceManager.getColor(SWT.COLOR_WHITE));
		styledText.setBounds(10, 95, 414, 284);
		
		spinner = new Spinner(shell, SWT.BORDER);
		spinner.setTextLimit(5);
		spinner.setMaximum(10000);
		spinner.setSelection(500);
		spinner.setBounds(298, 66, 59, 22);
		
		shell.open();
		while(!shell.isDisposed()) {
			if(!display.readAndDispatch()) {
				display.sleep();
			}
		}
		if(shell.isDisposed()) {
			System.out.println("Shell disposed");
		}
	}
	
	/** UpdateUI code */
	public static final Property<Shell> updateUI = new Property<Shell>().setRunnable(new Runnable() {
		@Override
		public final void run() {
			final Shell shell = updateUI.getValue();
			final Property<Long> lastTextUpdate = new Property<>(Long.valueOf(System.currentTimeMillis()));
			while(!shell.isDisposed()) {
				shell.getDisplay().asyncExec(new Runnable() {
					@Override
					public void run() {
						if(pb1.getSelection() == pb1.getMaximum()) {
							pb1.setSelection(0);
						} else {
							Functions.setSelectionFor(pb1, pb1.getSelection() + 1);
						}
						Functions.setSizeFor(pb1, new Point(shell.getSize().x - 35, pb1.getSize().y));
						Functions.setLocationFor(buttonButton, new Point(shell.getSize().x - 25 - buttonButton.getSize().x, buttonButton.getLocation().y));
						Functions.setLocationFor(buttonThreadedButton, new Point(shell.getSize().x - 25 - buttonThreadedButton.getSize().x, buttonThreadedButton.getLocation().y));
						Functions.setEnabledFor(buttonThreadedButton, buttonThreadedEnableButton.getSelection());
						long now = System.currentTimeMillis();
						if(now - lastTextUpdate.getValue().longValue() > spinner.getSelection()) {
							lastTextUpdate.setValue(Long.valueOf(now));
							styledText.setText(StringUtil.generateRandomChars(100, false).replace("t", "\r\n").replace("T", "\r\n"));
						}
					}
				});
				Functions.sleep();
			}
		}
	});
	
	/** @author Brian_Entei */
	public static final class LongRunningOperation extends Thread {
		
		protected final Display display;
		protected final Shell shell;
		
		/** @param shell The parent shell */
		public LongRunningOperation(Shell shell) {
			this.display = shell.getDisplay();
			this.shell = shell;
		}
		
		@Override
		public void run() {
			/*final Property<Long> lastTextUpdate = new Property<>(Long.valueOf(System.currentTimeMillis()));
			final Property<Long> threadSleepValue = new Property<>(Long.valueOf(10L));
			while(!this.shell.isDisposed()) {
				this.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						threadSleepValue.setValue(Long.valueOf(spinner_1.getSelection()));
						if(pb1.getSelection() == pb1.getMaximum()) {
							pb1.setSelection(0);
						} else {
							Functions.setSelectionFor(pb1, pb1.getSelection() + 1);
						}
						Functions.setSizeFor(pb1, new Point(LongRunningOperation.this.shell.getSize().x - 35, pb1.getSize().y));
						Functions.setLocationFor(buttonButton, new Point(LongRunningOperation.this.shell.getSize().x - 25 - buttonButton.getSize().x, buttonButton.getLocation().y));
						Functions.setLocationFor(buttonThreadedButton, new Point(LongRunningOperation.this.shell.getSize().x - 25 - buttonThreadedButton.getSize().x, buttonThreadedButton.getLocation().y));
						Functions.setEnabledFor(buttonThreadedButton, buttonThreadedEnableButton.getSelection());
						long now = System.currentTimeMillis();
						if(now - lastTextUpdate.getValue().longValue() > spinner.getSelection()) {
							lastTextUpdate.setValue(Long.valueOf(now));
							styledText.setText(StringUtil.generateRandomChars(100, false).replace("t", "\r\n").replace("T", "\r\n"));
						}
					}
				});
				Functions.sleep();//threadSleepValue.getValue().longValue());
			}*/
			while(!this.shell.isDisposed()) {
				try {
					Thread.sleep(12L);
				} catch(InterruptedException e) {
				}
				this.display.asyncExec(new Runnable() {
					@Override
					public void run() {
						if(LongRunningOperation.this.shell.isDisposed()) {
							return;
						}
						if(pb1.getSelection() == pb1.getMaximum()) {
							pb1.setSelection(0);
						} else {
							Functions.setSelectionFor(pb1, pb1.getSelection() + 1);
						}
						Functions.setEnabledFor(buttonThreadedButton, buttonThreadedEnableButton.getSelection());
					}
				});
			}
		}
	}
	
}
