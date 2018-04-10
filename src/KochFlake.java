import java.awt.event.*;
import java.awt.image.*;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.*;
import javax.swing.*;


public class KochFlake {
	public static JFrame frame = new JFrame("KochFlake");
	public static JPanel panel1 = new JPanel();
	public static BufferedImage image = new BufferedImage(800, 600, BufferedImage.TYPE_INT_RGB);
	public static Graphics2D g;
	public static ArrayList<P> al = new ArrayList<P>();
	public static final double d30 = Math.PI/6;
	public static final int maxdim = 9;
	
	public static double scale = 400;
	public static P origo= new P(400, 80);
	
	public static int msx,msy;
	public static double oox,ooy;
	public static int drawperiod=500;
	
	public static class P {
		private double x;
		private double y;
		
		public P () {this.x=0; this.y=0; };
		
		public P (double x, double y) {
			this.x=x;
			this.y=y;
		}
		
		public double getx() {
			return this.x;
		}
		
		public double gety() {
			return this.y;
		}
		
		public void setx(double x) {
			this.x=x;
		}
		
		public void sety(double y) {
			this.y=y;
		}
	}
	
	public static int sgn(double n) {
		if (n>0) {return 1;}
			else if (n<0) {return -1;}
				else return 0;
	}
	
	public static void window() {
		frame.setSize(800, 640);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.add(panel1);
		panel1.add(new JLabel(new ImageIcon(image)));
		panel1.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int notches = e.getWheelRotation();
				scale=scale*Math.pow(2, -notches);
			}
		});
		panel1.addMouseListener(new MouseListener() {
			public void mousePressed(MouseEvent e) {				
				if (e.getButton()==1) {
					msx=e.getX();
					msy=e.getY();
					oox=origo.getx();
					ooy=origo.gety();
				}
				
				if (e.getButton()==2 || e.getButton()==3) {
					origo= new P(400, 80);
					scale = 400;
				}
			}

			public void mouseReleased(MouseEvent e) {
				if (e.getButton()==1) {
					origo.setx(-(msx-e.getX())+oox );
					origo.sety(-(msy-e.getY())+ooy );
					
					drawperiod=5;
					
					try {
						Thread.sleep(10);
					} catch (Exception se) {
						se.printStackTrace();
					}
					
					drawperiod=500;
				}				
			}
			
			public void mouseEntered(MouseEvent e) {}
			public void mouseExited(MouseEvent e) {}
			public void mouseClicked(MouseEvent e) {}
		});
		
		g=image.createGraphics();		
	}
	
	public static class Calcal implements Runnable {
	    public void run() {
	    	al.clear();
			al.add(new P() );
			al.add(new P(Math.sin(d30), -Math.cos(d30)) );
			al.add(new P(-Math.sin(d30), -Math.cos(d30)) );
			al.add(new P() );
			
			for (int k=1;k<maxdim;k++) {
				int maxj=al.size()-1;			
				
				try {
					Thread.sleep(1000);
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				for (int j=0;j<maxj;j++) {
					int sp=j*4;
					
					double spx=al.get(sp).getx();
					double spy=al.get(sp).gety();
					double hossz=Math.pow(1./3, k);
					double dx=al.get(sp+1).getx()-spx;
					double dy=al.get(sp+1).gety()-spy;
					double szog=Math.abs(Math.atan(dx/dy));				
					int sgnx=sgn(dx);
					int sgny=sgn(dy);
					
					double szog2=szog;
					if (sgnx>=0 && sgny<0) {szog2=Math.PI-szog2;}
					else if (sgnx<0 && sgny<0) {szog2+=Math.PI;}
					else if (sgnx<0 && sgny>=0) {szog2=Math.PI*2-szog2;}
					szog2+=-Math.PI/3;
					
					P p1=new P();
					p1.setx(spx+Math.sin(Math.abs(szog))*hossz*sgnx);
					p1.sety(spy+Math.cos(Math.abs(szog))*hossz*sgny);
					
					P p2=new P();
					p2.setx(p1.getx()+Math.sin(szog2)*hossz);
					p2.sety(p1.gety()+Math.cos(szog2)*hossz);
					
					P p3=new P();
					p3.setx(spx+Math.sin(Math.abs(szog))*2*hossz*sgnx);
					p3.sety(spy+Math.cos(Math.abs(szog))*2*hossz*sgny);
					
					al.add(sp+1, p1);
					al.add(sp+2, p2);
					al.add(sp+3, p3);
				}
			}
	    }
	}
	
	public static void drawing() {
		g.setColor(Color.BLUE);
		g.setBackground(Color.BLACK);
		g.clearRect(0, 0, 800, 600);

		for (int i=0;i<al.size()-1;i++) {
			P sp=al.get(i);
			P ep=al.get(i+1);
			int sx=(int)(sp.getx()*scale+origo.getx() );
			int sy=(int)(-sp.gety()*scale+origo.gety() );
			int ex=(int)(ep.getx()*scale+origo.getx() );
			int ey=(int)(-ep.gety()*scale+origo.gety() );
			
			g.drawLine(sx, sy, ex, ey);
		}
		
		image.flush();
		panel1.repaint();
	}
	
	public static void main (String[] args) {
		window();
		
		Thread calc = new Thread(new Calcal() );
		calc.start();

		while (true) {
			drawing();
			
			try {
				Thread.sleep(drawperiod);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}
}
