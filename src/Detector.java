import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Detector extends JFrame{
	private static int VOTE_CUTOFF = 200;
	private static int VOTES_NEEDED = 60;
	private static double ACCUMULATOR_FIDELITY = 0.05;
	
	private BufferedImage fullimage;
	private BufferedImage image;
	private BufferedImage sobel;
	private BufferedImage accumulatorSnapshot;
	
	private ArrayList<Circle> circles;
	
	public Detector() {
		this.setSize(1600, 640);
		this.setTitle("Circle Detector");
		this.setVisible(true);
		this.setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	public void loadImage(String path) {
		try {
			fullimage = ImageIO.read(new File(path));
			image = ImageIO.read(new File(path));
			ColorConvertOp converter = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null);
			converter.filter(image, image);
		} catch (IOException e) {
			System.out.println("Failed to open image");
		}
	}

	@Override
	public void paint(Graphics g) {
		super.paint(g);
		
		if (fullimage != null)
			g.drawImage(fullimage, 0, 30, null);
		
		if (sobel != null)
			g.drawImage(sobel, image.getWidth(), 30, null);
		
		if (accumulatorSnapshot != null)
			g.drawImage(accumulatorSnapshot, image.getWidth()*2, 30, null);
		
		if (circles != null) {
			for (Circle circle : circles) {
				g.setColor(Color.BLUE);
				Graphics2D graphics = (Graphics2D) g;
				graphics.setStroke(new BasicStroke(3));
				g.drawOval(circle.x-circle.r + 1, circle.y-circle.r + 31, circle.r*2, circle.r*2);
				g.drawLine(circle.x - 10, circle.y + 30, circle.x + 10, circle.y + 30);
				g.drawLine(circle.x, circle.y + 20, circle.x, circle.y + 40);
			}
		}
		
	}
	
	public void update() {
		this.paint(this.getGraphics());
	}
	
	public void applySobel() {
		sobel = new BufferedImage(image.getWidth()-2, image.getHeight()-2, BufferedImage.TYPE_BYTE_GRAY);
		
		for (int x = 1; x < image.getWidth()-1; ++x) {
			for (int y = 1; y < image.getHeight()-1; ++y) {
				int gx =-(image.getRGB(x - 1, y + 1)&0xff) * 4
						-(image.getRGB(x - 1, y    )&0xff) * 8
						-(image.getRGB(x - 1, y - 1)&0xff) * 4
						+(image.getRGB(x + 1, y + 1)&0xff) * 4
						+(image.getRGB(x + 1, y    )&0xff) * 8
						+(image.getRGB(x + 1, y - 1)&0xff) * 4;
				
				int gy =-(image.getRGB(x - 1, y + 1)&0xff) * 4
						-(image.getRGB(x,     y + 1)&0xff) * 8
						-(image.getRGB(x + 1, y + 1)&0xff) * 4
						+(image.getRGB(x - 1, y - 1)&0xff) * 4
						+(image.getRGB(x,     y - 1)&0xff) * 8
						+(image.getRGB(x + 1, y - 1)&0xff) * 4;
				
				int sobelValue = Math.min(Math.max((int) Math.sqrt(gx*gx + gy*gy), 0),255);
				
				sobel.setRGB(x-1, y-1, new Color(sobelValue,sobelValue,sobelValue).getRGB());
			}
		}
	}
	
	public ArrayList<Circle> applyHoughCircle(int minRadius, int maxRadius) {
		ArrayList<Circle> candidates = new ArrayList<Circle>();
		
		for (int r = minRadius; r < maxRadius; ++r) {
			
			BufferedImage accumulator = new BufferedImage(image.getWidth()-2, image.getHeight()-2, BufferedImage.TYPE_INT_RGB);
			int width = accumulator.getWidth();
			int height = accumulator.getHeight();
			
			
			for (int x = 0; x < sobel.getWidth(); ++x) {
				for (int y = 0; y < sobel.getHeight(); ++y) {
					if ((sobel.getRGB(x, y)&0xff) > VOTE_CUTOFF) {
						
						for (double theta = 0; theta < 6.283; theta += ACCUMULATOR_FIDELITY) {
							int endx = x + (int) (Math.sin(theta) * r);
							int endy = y + (int) (Math.cos(theta) * r);
							
							if (endx >= 0 && endx < width && endy >=0 && endy < height) {
								int currentValue = accumulator.getRGB(endx, endy)&0xff;
								int value = currentValue + ((sobel.getRGB(x, y)&0xff) / VOTE_CUTOFF);
								value *= 1;
								if (value > 255) {
									value = 255;
								}
								
								if (value > VOTES_NEEDED) {
									candidates.add(new Circle(endx, endy, r));
									//System.out.println("Possible circle (" + endx + "," + endy + ") radius " + r);
								}
								
								accumulator.setRGB(endx,  endy, new Color(value, value, value).getRGB());
							}
						}
					}
				}
			}
			
			this.accumulatorSnapshot = accumulator;
			this.update();
		}
		
		
		//Compress candidates into individual circles
		HashMap<Circle, ArrayList<Circle>> revisionList = new HashMap<Circle, ArrayList<Circle>>();
		
		for (Circle c : candidates) {
			boolean duplicate = false;
			
			for (Circle key : revisionList.keySet()) {
				if (c.isInside(key)) {
					duplicate = true;
					revisionList.get(key).add(c);
					break;
				}
			}
			
			if (!duplicate) {
				revisionList.put(c, new ArrayList<Circle>());
				revisionList.get(c).add(c);
			}
		}
		
		//Final circles
		ArrayList<Circle> circles = new ArrayList<Circle>();
		
		for (Circle key : revisionList.keySet()) {
			Circle averageCircle = new Circle(0, 0, 0);
			int number = revisionList.get(key).size();
			
			for (Circle c : revisionList.get(key)) {
				averageCircle.x += c.x;
				averageCircle.y += c.y;
				averageCircle.r += c.r;
			}
			
			averageCircle.x /= number;
			averageCircle.y /= number;
			averageCircle.r /= number;
			
			circles.add(averageCircle);
		}
		this.circles = circles;
		return circles;
		
	}
	
}
