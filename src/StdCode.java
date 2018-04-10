import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import javax.swing.*;
import com.amd.aparapi.*;



public class StdCode {
	public static JFrame frame = new JFrame("Mandelbrot");	
	public static JComponent imagepanel = new JComponent(){
		private static final long serialVersionUID = 1L;
		@Override
		protected void paintComponent(Graphics g) {
			g.drawImage(image, 0, 0, null);
		}
	};	
	public static BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
	public static BufferedImage imageb = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
	
	public static int[] rgb = ((DataBufferInt) imageb.getRaster().getDataBuffer()).getData();
	public static int[] rgbi = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
	public static DrawKernel drawkernel = new DrawKernel(rgb);
	
	public static boolean resizing=false;
	public static long resizestime = System.currentTimeMillis();
	
	
	public static class DrawKernel extends Kernel {		
		final private int rgb[];
		final private int maxC = 512;
		@Constant final private int pallette[] = new int[maxC];
		
		public DrawKernel(int[] _rgb) {
	    	rgb = _rgb;
	    	
	    	for (int i = 0; i < maxC; i++) {
	    		float h = i / (float) maxC;
	    		float b = 1.0f - h * h;
	            pallette[i] = Color.HSBtoRGB(h, 1f, b);
	        }
	    }
		
		@Override
		public void run() {
			int gid = getGlobalId(1) * getGlobalSize(0) + getGlobalId(0);
			
			if (getGlobalId(0) < maxC) {
				rgb[gid] = pallette[getGlobalId(0)];
			}
		}
	}
	
	public static void window() {
		frame.setSize(816, 638);
		frame.setMinimumSize(new Dimension(400, 300));
		frame.setResizable(true);
		frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		frame.setVisible(true);
		
		BorderLayout framelayout = new BorderLayout();
		frame.setLayout(framelayout);
		frame.add(imagepanel,BorderLayout.CENTER);
		
		frame.addWindowListener(new WindowAdapter(){
	         public void windowClosing(WindowEvent _windowEvent) {
	        	 if (drawkernel!=null) { drawkernel.dispose(); }
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
					(new Thread(new StdCode.resizeThread() ) ).start();
				}								
		    }
		    
		    public void componentHidden(ComponentEvent e) {}
		    public void componentShown(ComponentEvent e) {}
		    public void componentMoved(ComponentEvent e) {}
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
			
			if (drawkernel!=null) { drawkernel.dispose(); }
			image = new BufferedImage(nix, niy, BufferedImage.TYPE_INT_RGB);
			imageb = new BufferedImage(nix, niy, BufferedImage.TYPE_INT_RGB);				
			rgb = ((DataBufferInt) imageb.getRaster().getDataBuffer()).getData();
		    rgbi = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
			drawkernel = new DrawKernel(rgb);				
			
			drawimage();
			image.flush();
	        imagepanel.repaint();
	        
	        resizing=false;
		}
	}
	
	public static void drawimage() {
		final Range range = Range.create2D(imageb.getWidth(), imageb.getHeight());
		drawkernel.execute(range);
		System.arraycopy(rgb, 0, rgbi, 0, rgbi.length);
		image.flush();
        imagepanel.repaint();
	}
	
	
	public static void main(String[] args) {
		window();
		listen();
		drawimage();
		
		//System.out.println(imagepanel.getSize());
	}
	
}
