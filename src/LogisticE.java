import java.awt.image.*;
import javax.swing.*;
//import java.util.*;


public class LogisticE {
	public static JFrame frame = new JFrame("LogisticE");
	public static JPanel panel1 = new JPanel();
	public static JPanel panel2 = new JPanel();
	public static BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
	public static double startV = 0.001;
	public static double rate = 1.1;
	
	public static void window() {
		frame.setSize(600, 440);
		frame.setResizable(false);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.add(panel1);
		//frame.add(panel2);
		panel1.add(new JLabel(new ImageIcon(image)));
		//panel2.add(new JTextField());
		
	}
	
	public static void proc() {
		while (true) {
			int[] bca = new int[600*400];
			image.setRGB(0, 0, 600, 400, bca, 0, 0);
			
			double prevV = startV;			
			for (int i=0;i<600;i++) {
				double newV = eq1(prevV, rate);				
				int newVi = (int)(400-newV*400);
				if (newVi>399) newVi=399;
				if (newVi<0) newVi=0;
				image.setRGB(i, newVi, 16777215);
				prevV = newV;
			}
			
			image.flush();
			panel1.repaint();
			
			try {
				Thread.sleep(10);
			} catch (Exception e) {
				e.printStackTrace();
			}
			
			rate+=0.01;
			if (rate>4) rate+=-3;
		}
		
	}
	
	public static double eq1(double x, double r) {		
		return r*x*(1-x);
	}
	
	public static void main(String[] args) {
		window();
		proc();
	}
}
