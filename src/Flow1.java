import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.*;

import com.amd.aparapi.*;



public class Flow1 {
	public static JFrame frame = new JFrame("Mandelbrot");	
	public static JComponent imagepanel = new JComponent(){
		private static final long serialVersionUID = 1L;
		@Override
		protected void paintComponent(Graphics g) {
			g.drawImage(image, 0, 0, null);
		}
	};	
	public static BufferedImage image = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
	public static BufferedImage imageb = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
	
	public static int[] rgb = ((DataBufferInt) imageb.getRaster().getDataBuffer()).getData();
	public static int[] rgbi = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();	
	
	public static boolean resizing = false;
	public static long resizestime = System.currentTimeMillis();
	
	public static int parrsize = 50;
	public static Particle[] parr = new Particle[parrsize*parrsize];
	
	public static FlowKernel flowkernel = null;
	
	
	public static final class Particle {
		public float px, py;
		public float vx, vy;
		
		public Particle () {
			px=0; py=0;
			vx=0; vy=0;
		}
		
		public Particle (float _px, float _py, float _vx, float _vy) {
			px=_px;
			py=_py;
			vx=_vx;
			vy=_vy;
		}
	}
	
	
	public static class FlowKernel extends Kernel {		
		final private Particle[] parr;
		final private float fC = 0.0000000001f;
		final private float wC = 1f;
		
		public FlowKernel(Particle[] _parr) {
	    	parr = _parr;
	    }
		
		@Override
		public void run() {
			float fx=0f, fy=0f;
			float dx=0f, dy=0f, d2=0f;
			int gid = getGlobalId(0);
			
			for (int i=0;i<getGlobalSize(0);i++)
				if (i!=gid) {
					dx = parr[i].px-parr[gid].px;
					dy = parr[i].py-parr[gid].py;
					d2 = dx*dx + dy*dy;
					fx+= -(fC/d2)*(Math.abs(dx)*dx/d2);
					fy+= -(fC/d2)*(Math.abs(dy)*dy/d2);				
				}
			
			parr[gid].vx+=fx;
			parr[gid].vy+=fy;
			parr[gid].px+=parr[gid].vx;
			parr[gid].py+=parr[gid].vy;			
			
			if (parr[gid].px < 0) {
				parr[gid].vx = -parr[gid].vx;
				parr[gid].px = -parr[gid].px;
			}			
			
			if (parr[gid].px > 1) {
				parr[gid].vx = -parr[gid].vx;
				parr[gid].px = 2-parr[gid].px;
			}
			
			if (parr[gid].py < 0) {
				parr[gid].vy = -parr[gid].vy*(1/wC);
				parr[gid].py = -parr[gid].py;
			}
			
			if (parr[gid].py > 1) {
				parr[gid].vy = -parr[gid].vy*wC;
				parr[gid].py = 2-parr[gid].py;
			}
			
		}
	}
	
	public static void window() {
		frame.setSize(806, 828);
		//frame.setMinimumSize(new Dimension(400, 300));
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
		
		BorderLayout framelayout = new BorderLayout();
		frame.setLayout(framelayout);
		frame.add(imagepanel,BorderLayout.CENTER);
		
		frame.addWindowListener(new WindowAdapter(){
	         public void windowClosing(WindowEvent _windowEvent) {
	        	 if (flowkernel!=null) { flowkernel.dispose(); }
	        	 System.exit(0);
	         }
		});				
		
	}
	
	public static void listen() {
		
		
	}
	
	public static void drawimage() {
		imageb = new BufferedImage(800, 800, BufferedImage.TYPE_INT_RGB);
		rgb = ((DataBufferInt) imageb.getRaster().getDataBuffer()).getData();		
		
		for (int i=0;i<parr.length;i++) {
			/*int index = (int)(parr[i].py*800*800 + parr[i].px*800);
			if (index >= 0 && index < rgb.length)
				rgb[index] = 16777215;*/
			int ix = (int)(parr[i].px*800);
			int iy = (int)(parr[i].py*800);
			if (ix >= 0 && iy >= 0 && ix < 800 && iy < 800)
				imageb.setRGB((int)(parr[i].px*800), (int)(parr[i].py*800), 16777215);
		}
		
		System.arraycopy(rgb, 0, rgbi, 0, rgb.length);			
		image.flush();
        imagepanel.repaint();
	}
	
	public static void initparr() {
		for (int j=0;j<parrsize;j++)
			for (int i=0;i<parrsize;i++) {
				int gid = j * parrsize + i;
				parr[gid]=new Particle();
				parr[gid].px=(i+0.5f)*(1/(float)parrsize);
				parr[gid].py=(j+0.5f)*(1/(float)parrsize);
				parr[gid].vx=0;
				parr[gid].vy=0;
			}
		
		flowkernel = new FlowKernel(parr);
	}
	
	public static void main(String[] args) {
		window();
		listen();		
		initparr();
		
		while (true) {
			//long st=System.currentTimeMillis();
			for (int i=0;i<5;i++)
				flowkernel.execute(parr.length);
			drawimage();

			//System.out.println(System.currentTimeMillis()-st);
			
			/*try {
				Thread.sleep(1);
			} catch (Exception se) {
				se.printStackTrace();
			}*/
		}
				
		//System.out.println(imagepanel.getSize());
	}
	
}
