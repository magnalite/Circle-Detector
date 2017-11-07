import java.util.ArrayList;

public class Main {

	public static void main(String[] args) {
		Detector testdetect = new Detector();
		testdetect.loadImage("test1.jpg");
		testdetect.applySobel();
		
		ArrayList<Circle> circles = testdetect.applyHoughCircle(50, 85);
		
		//testdetect.drawCircles(circles);
		
		testdetect.update();
		
		System.out.println("Finished");
	}
	
}
