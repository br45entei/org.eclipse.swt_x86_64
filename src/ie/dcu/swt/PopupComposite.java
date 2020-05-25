package ie.dcu.swt;

import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.events.ShellAdapter;
import org.eclipse.swt.events.ShellEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Layout;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

/** Class for displaying a pop-up control in its own shell. The control behaves
 * similar to a pop-up menu, but is a composite so can contain arbitrary
 * controls.
 * 
 * <p>
 * <b>Sample Usage:</b>
 * 
 * <pre>
 * PopupComposite popup = new PopupComposite(getShell());
 * Text text = new Text(popup, SWT.BORDER);
 * popup.pack();
 * popup.show(shell.toDisplay(new Point(10, 10)));
 * </pre>
 * 
 * @author <a href=
 *         "http://palea.cgrb.oregonstate.edu/svn/jaiswallab/Annotation/src/ie/dcu/swt/PopupComposite.java">Kevin
 *         McGuinness</a>
 * @author Brian_Entei (Small edits, some customizations) */
public class PopupComposite extends Composite {
	
	/** Style of the shell that will house the composite */
	private static final int SHELL_STYLE = SWT.MODELESS | SWT.NO_TRIM | SWT.ON_TOP | SWT.BORDER;
	
	/** Shell that will house the composite */
	private final Shell shell;
	
	/** Create a Pop-up composite with the default {@link SWT#BORDER} style.
	 * 
	 * @param parent
	 *            The parent shell. */
	public PopupComposite(Shell parent) {
		this(parent, SWT.BORDER);
	}
	
	/** Create a Pop-up composite. The default layout is a fill layout.
	 * 
	 * @param parent
	 *            The parent shell.
	 * @param style
	 *            The composite style. */
	public PopupComposite(Shell parent, int style) {
		this(parent, style, new FillLayout());
		this.setLayout(createLayout());
	}
	
	/** Create a Pop-up composite with the specified layout.
	 * 
	 * @param parent
	 *            The parent shell.
	 * @param style
	 *            The composite style.
	 * @param layout
	 *            The composite layout. */
	public PopupComposite(Shell parent, int style, Layout layout) {
		super(new Shell(parent, SHELL_STYLE), style);
		this.shell = this.getShell();
		if(layout != null) {
			this.shell.setLayout(layout);
			this.setLayout(createLayout());
		}
		this.shell.addShellListener(new ActivationListener());
		this.shell.addTraverseListener(new TraverseListener() {
			@Override
			public void keyTraversed(TraverseEvent e) {
				if(e.keyCode == SWT.ESC) {
					e.doit = false;
					PopupComposite.this.hide();
				}
			}
		});
	}
	
	/** Display the composite below the given tool item. The item will be sized
	 * such that it's width is at least the width of the given tool item.
	 * 
	 * @param bar
	 *            The tool bar.
	 * @param item
	 *            The tool item. */
	public void showBelow(ToolBar bar, ToolItem item) {
		Rectangle r = item.getBounds();
		Point p = bar.toDisplay(new Point(r.x, r.y + r.height));
		super.setSize(computeSize(item));
		show(p);
	}
	
	/** Display the composite in its own shell at the given point.
	 * 
	 * @param pt
	 *            The point where the pop-up should appear. */
	public void show(Point pt) {
		this.pack();
		
		if(this.getLayout() != null) {
			// Match shell and component sizes
			this.shell.setSize(super.getSize());
		}
		
		if(pt != null) {
			this.shell.setLocation(pt);
		}
		
		this.shell.open();
		this.shell.layout();
	}
	
	/** Display the composite in its own shell at the given point in
	 * display-relative coordinates.
	 * 
	 * @param pt
	 *            The point where the pop-up should appear. */
	public void showToDisplay(Point pt) {
		this.show(this.shell.toDisplay(pt));
	}
	
	/** Display the pop-up where it was last displayed. */
	public void show() {
		show(null);
	}
	
	/** Hide the pop-up. */
	public void hide() {
		this.shell.setVisible(false);
	}
	
	/** Returns <code>true</code> if the shell is currently activated.
	 * 
	 * @return <code>true</code> if the shell is visible. */
	public boolean isDisplayed() {
		return this.shell.isDisposed() ? false : this.shell.isVisible();
	}
	
	/** Creates the default layout for the composite.
	 * 
	 * @return the default layout. */
	private static FillLayout createLayout() {
		FillLayout layout = new FillLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
		return layout;
	}
	
	/** Computes the optimal size with respect to the given tool item.
	 * 
	 * @param item
	 *            The tool item.
	 * @return The optimal size. */
	private Point computeSize(ToolItem item) {
		Point s2 = computeSize(item.getWidth(), SWT.DEFAULT);
		Point s1 = computeSize(SWT.DEFAULT, SWT.DEFAULT);
		return s1.x > s2.x ? s1 : s2;
	}
	
	/** Sets the receiver's text, which is the string that the
	 * window manager will typically display as the receiver's
	 * <em>title</em>, to the argument, which must not be null.
	 * <p>
	 * Note: If control characters like '\n', '\t' etc. are used
	 * in the string, then the behavior is platform dependent.
	 * </p>
	 *
	 * @param string the new text
	 *
	 * @exception IllegalArgumentException
	 *                <ul>
	 *                <li>ERROR_NULL_ARGUMENT - if the text is null</li>
	 *                </ul>
	 * @exception SWTException
	 *                <ul>
	 *                <li>ERROR_WIDGET_DISPOSED - if the receiver has been
	 *                disposed</li>
	 *                <li>ERROR_THREAD_INVALID_ACCESS - if not called from the
	 *                thread that created the receiver</li>
	 *                </ul>
	 */
	public void setText(String string) {
		this.shell.setText(string);
	}
	
	@Override
	public Point getLocation() {
		return this.shell.getLocation();
	}
	
	@Override
	public void setLocation(int x, int y) {
		this.shell.setLocation(x, y);
	}
	
	@Override
	public void setLocation(Point location) {
		this.shell.setLocation(location);
	}
	
	@Override
	public Point getSize() {
		return this.shell.getSize();
	}
	
	@Override
	public void setSize(int width, int height) {
		this.shell.setSize(width, height);
	}
	
	@Override
	public void setSize(Point size) {
		this.shell.setSize(size);
	}
	
	@Override
	public Rectangle getBounds() {
		return this.shell.getBounds();
	}
	
	@Override
	public void setBounds(int x, int y, int width, int height) {
		this.shell.setBounds(x, y, width, height);
	}
	
	@Override
	public void setBounds(Rectangle bounds) {
		this.shell.setBounds(bounds);
	}
	
	/** Class that handles shell appearance and disappearance appropriately.
	 * Specifically, it hides the shell when it becomes de-activated (for
	 * example,
	 * when the user clicks on the parent shell). Also, there is a minimum delay
	 * which is enforced between showing and hiding the pop-up, to prevent
	 * undesirable behavior such as hiding and immediately re-displaying the
	 * pop-up when the user selects a button responsible for showing the tool
	 * item. */
	protected final class ActivationListener extends ShellAdapter {
		private static final int TIMEOUT = 500;
		private long time = -1;
		
		@Override
		public void shellDeactivated(ShellEvent e) {
			// Record time of event
			this.time = (e.time & 0xFFFFFFFFL);
			
			// Hide
			hide();
		}
		
		@Override
		public void shellActivated(ShellEvent e) {
			if(this.time > 0) {
				// Find elapsed time 
				long elapsed = ((e.time & 0xFFFFFFFFL) - this.time);
				
				// If less than a timeout, don't activate
				if(elapsed < TIMEOUT) {
					hide();
					
					// Next activation event is fine
					this.time = -1;
				}
			}
		}
	}
	
}
