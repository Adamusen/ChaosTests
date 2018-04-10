import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
//import java.util.*;
import javax.swing.*;
import com.amd.aparapi.*;



public class Mandelbrot {
	public static JFrame frame = new JFrame("Mandelbrot");	
	public static JComponent imagepanel = new JComponent(){
		private static final long serialVersionUID = 1L;
		@Override
		protected void paintComponent(Graphics g) {
			g.drawImage(image, 0, 0, null);
		}
	};	
	public static BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
	public static BufferedImage imageb = new BufferedImage(500, 500, BufferedImage.TYPE_INT_RGB);
	//public static Graphics2D g;
	
	public static int[] rgbi = null;
	public static int[] rgb = null;
	public static MandelKernel kernel = null;
	
	public static double xs=-2.5f, ys=-1.5f, xe=0.5f, ye=1.5f;
	public static double txs=-2.5f, tys=-1.5f, txe=0.5f, tye=1.5f;
	
	public static final Object doorBell = new Object();
	
	public static boolean resizing=false;
	public static long resizestime = System.currentTimeMillis();
	
	public static int dragsx, dragsy;
	public static boolean dragging=false;
	
	public static long acttime = System.currentTimeMillis();
					
	
	
	public static class MandelKernel extends Kernel{
		final private int rgb[];
		final private int maxIterations = 512;
		@Constant final private int pallette[] = new int[maxIterations + 1];
		private double xs = .0;
	    private double ys = .0;
	    private double xe = .0;
	    private double ye = .0;
		
	    public MandelKernel(int[] _rgb) {
	    	rgb = _rgb;
	    	
	    	for (int i = 0; i < maxIterations; i++) {
	    		float h = i / (float) maxIterations;
	    		float b = 1.0f - h * h;
	            pallette[i] = Color.HSBtoRGB(h, 1f, b);
	        }
	    }
	    
	    public void setRectangle(double _xs, double _ys, double _xe, double _ye) {
	    	xs = _xs;
	    	ys = _ys;
	    	xe = _xe;
	    	ye = _ye;
	    }
	    	   	    
		@Override
		public void run() {
			int gid = getGlobalId(1) * getGlobalSize(0) + getGlobalId(0);
			double x = (xe-xs)/(double)(getGlobalSize(0)-1)*getGlobalId(0)+xs;
			double y = (ye-ys)/(double)(getGlobalSize(1)-1)*getGlobalId(1)+ys;
			
			int count = 0;
			
			double zx = x;
			double zy = y;
			double new_zx = 0.;
			
			while (count < maxIterations && zx * zx + zy * zy < 8) {
				new_zx = zx * zx - zy * zy + x;
	            zy = 2 * zx * zy + y;
	            zx = new_zx;
	            count++;
	        }
			
			rgb[gid] = pallette[count];
		}
	}	
	
	
	public static void drawimage(double _xs, double _ys, double _xe, double _ye) {								
		final Range range = Range.create2D(imageb.getWidth(), imageb.getHeight());
		kernel.setRectangle(_xs, _ys, _xe, _ye);
	    kernel.execute(range);	    
	    System.arraycopy(rgb, 0, rgbi, 0, rgb.length);	    
	    //System.out.println(_xs + ", " + _ys + ", " + _xe + ", " + _ye);
	}
	
	public static void window() {
		frame.setSize(800, 645);
		frame.setMinimumSize(new Dimension(400, 300));
		frame.setResizable(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
		
		BorderLayout framelayout = new BorderLayout();
		frame.setLayout(framelayout);
		frame.add(imagepanel,BorderLayout.CENTER);
		
		frame.addWindowListener(new WindowAdapter(){
	         public void windowClosing(WindowEvent _windowEvent) {
	        	 if (kernel!=null) { kernel.dispose(); }
	        	 System.exit(0);
	         }
		});				
		
	}
	
	public static void listen() {
		frame.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) {
				resizestime = System.currentTimeMillis();
				if (resizing==false) {
					resizing=true;
					(new Thread(new Mandelbrot.resizeThread() ) ).start();
				}								
		    }
		    
		    public void componentHidden(ComponentEvent e) {}
		    public void componentShown(ComponentEvent e) {}
		    public void componentMoved(ComponentEvent e) {}
		});
		
		imagepanel.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				double scale=(double)(Math.pow(1.25, notches));
				
				int toxp=e.getX();
				int toyp=e.getY();
				double tox=(txe-txs)*(toxp/(double)imageb.getWidth())+txs;
				double toy=(tye-tys)*(toyp/(double)imageb.getHeight())+tys;
				
				txs=tox-(tox-txs)*scale;
				txe=tox+(txe-tox)*scale;
				tys=toy-(toy-tys)*scale;
				tye=toy+(tye-toy)*scale;
				
				acttime = System.currentTimeMillis();
				synchronized (doorBell) {
					doorBell.notify();
				}								
			}
		});
		
		imagepanel.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton()==1) {
					dragsx=e.getX();
					dragsy=e.getY();
					dragging=true;
					(new Thread(new Mandelbrot.dragThread(dragsx, dragsy) ) ).start();
				}
			}

			public void mouseReleased(MouseEvent e) {
				dragging=false;
			}
			
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {}
		});
		
	}
	
	public static class resizeThread implements Runnable {
		public void run() {
			while (System.currentTimeMillis()-resizestime<100) {
				try {
					Thread.sleep(5);
				} catch (Exception se) {
					se.printStackTrace();
				}
			}
			
			int nix = imagepanel.getWidth();
			int niy = imagepanel.getHeight();
			
			if (kernel!=null) { kernel.dispose(); }
			image = new BufferedImage(nix, niy, BufferedImage.TYPE_INT_RGB);
			imageb = new BufferedImage(nix, niy, BufferedImage.TYPE_INT_RGB);				
			rgb = ((DataBufferInt) imageb.getRaster().getDataBuffer()).getData();
		    rgbi = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
			kernel = new MandelKernel(rgb);				
			
			double oxm=(xe-xs)/2.+xs;
			xs=oxm-((ye-ys)/2f)*(nix/(double)niy);
			xe=oxm+((ye-ys)/2f)*(nix/(double)niy);
			txs=xs;
			txe=xe;
			drawimage(xs, ys, xe, ye);
			image.flush();
	        imagepanel.repaint();
	        
	        resizing=false;
		}
	}
	
	public static class dragThread implements Runnable {
		int mpx, mpy;
		
		public dragThread (int _mpx, int _mpy) {
			mpx=_mpx;
			mpy=_mpy;
		}
		
		public void run() {
			int imageW=imageb.getWidth();
			int imageH=imageb.getHeight();
			double otxs=txs;
			double otxe=txe;
			double otys=tys;
			double otye=tye;
			
			while (dragging) {								
				try {
					mpx=imagepanel.getMousePosition().x;
					mpy=imagepanel.getMousePosition().y;
				} catch (Exception e) { }
				
				txs=otxs-((mpx-dragsx) / (double)(imageW) ) * (txe-txs);
				txe=otxe-((mpx-dragsx) / (double)(imageW) ) * (txe-txs);
				tys=otys-((mpy-dragsy) / (double)(imageH) ) * (tye-tys);
				tye=otye-((mpy-dragsy) / (double)(imageH) ) * (tye-tys);
				
				acttime = System.currentTimeMillis();
				synchronized (doorBell) {
					doorBell.notify();
				}
				
				try {
					Thread.sleep(10);
				} catch (Exception se) {
					se.printStackTrace();
				}
			}
		}
	}
	
	public static void act() {
		while (true) {
			while (xs==txs && ys==tys && xe==txe && ye==tye) {
				synchronized (doorBell) {
					try {
						doorBell.wait();
					} catch (InterruptedException ie) {
						ie.getStackTrace();
					}
				}
			}
						
			xs=txs; xe=txe;
			ys=tys; ye=tye;			
			
			drawimage(xs, ys, xe, ye);
			image.flush();
	        imagepanel.repaint();
		}
	}
	
	public static void main(String[] args) {
		window();
		listen();
		act();
	}	
	
}