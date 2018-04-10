import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

//import java.util.*;
import javax.swing.*;

import com.amd.aparapi.*;



public class MandelbrotR {
	public static JFrame frame = new JFrame("Mandelbrot");	
	public static JComponent imagepanel = new JComponent(){
		private static final long serialVersionUID = 1L;
		@Override
		protected void paintComponent(Graphics g) {
			g.drawImage(image, 0, 0, null);
		}
	};	
	public static BufferedImage image = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
	public static BufferedImage imageb = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
	//public static Graphics2D g;
	
	public static int[] rgbi = null;
	public static int[] rgb = null;
	public static MandelKernel kernel = null;
	
	public static double xs=-2.f, ys=-2.f, xe=2.f, ye=2.f;
	public static double txs=-2.f, tys=-2.f, txe=2.f, tye=2.f;
	
	public static final Object doorBell = new Object();
	
	public static boolean resizing=false;
	public static long resizestime = System.currentTimeMillis();
	
	public static int dragsx, dragsy;
	public static boolean dragging=false;
	public static boolean zooming=true;
	public static boolean scalesetting=false;
	public static double zoomscale = 1.25;
	public static double powscale = 0.05;
	
	public static long acttime = System.currentTimeMillis();
	
	public static class MandelKernel extends Kernel{		
		final private int rgb[];		
		final private int maxIterations = 127;
		@Constant final private int pallette[] = new int[maxIterations + 1];
		private double xs = .0;
	    private double ys = .0;
	    private double xe = .0;
	    private double ye = .0;
	    public double p = 2;
		
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
			//double cabsv = Math.sqrt(x*x + y*y);
			
			while (count < maxIterations && zx*zx + zy*zy < 8) {				
				double teta = Math.atan(zy/zx);
				double absv = Math.sqrt(zx*zx + zy*zy);
				
				if (zx<0) {
					teta=Math.PI+teta;
				}
				
				zx=Math.pow(absv, count)*Math.cos(count*teta)+x;
				zy=Math.pow(absv, count)*Math.sin(count*teta)+y;
						
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
	}
	
	public static void window() {
		frame.setSize(640, 480);
		frame.setMinimumSize(new Dimension(100, 100));
		frame.setResizable(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
		
		BorderLayout framelayout = new BorderLayout();
		frame.setLayout(framelayout);
		frame.add(imagepanel,BorderLayout.CENTER);
		/*JPanel datapanel = new JPanel();
		JTextField t1 = new JTextField(Double.toString(xs) + ", ");
		datapanel.add(t1);
		frame.add(datapanel,BorderLayout.SOUTH);*/
		
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
					(new Thread(new MandelbrotR.resizeThread() ) ).start();
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
				
				if (scalesetting==false) {
					if (zooming == true) {
						double scale=(double)(Math.pow(zoomscale, notches));
						
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
					} else {
						kernel.p=kernel.p-notches*powscale;
						
						drawimage(xs, ys, xe, ye);
						image.flush();
				        imagepanel.repaint();
					}
				} else {
					if (zooming == true) {
						zoomscale=(zoomscale-1)*Math.pow(1.25, -notches)+1;
					} else {
						powscale=powscale*Math.pow(1.25, -notches);
					}
				}
											
			}
		});
		
		imagepanel.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent e) {
				if (e.getButton()==1) {
					dragsx=e.getX();
					dragsy=e.getY();
					dragging=true;
					(new Thread(new MandelbrotR.dragThread(dragsx, dragsy) ) ).start();
				}
				
				if (e.getButton()==2) {
					scalesetting=!scalesetting;
				}
				
				if (e.getButton()==3) {
					zooming=!zooming;
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
			double pow = 2;
			
			if (kernel!=null) {
				pow=kernel.p;
				kernel.dispose();				
			}
			image = new BufferedImage(nix, niy, BufferedImage.TYPE_INT_RGB);
			imageb = new BufferedImage(nix, niy, BufferedImage.TYPE_INT_RGB);				
			rgb = ((DataBufferInt) imageb.getRaster().getDataBuffer()).getData();
		    rgbi = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
			kernel = new MandelKernel(rgb);
			kernel.p = pow;
			
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
				
				txs=otxs-((mpx-dragsx) / (double)(imageW) ) * (otxe-otxs);
				txe=otxe-((mpx-dragsx) / (double)(imageW) ) * (otxe-otxs);
				tys=otys-((mpy-dragsy) / (double)(imageH) ) * (otye-otys);
				tye=otye-((mpy-dragsy) / (double)(imageH) ) * (otye-otys);
				
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