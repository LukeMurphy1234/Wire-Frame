import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.util.*;
import java.awt.Graphics2D;
import java.awt.Point;

import javax.imageio.ImageIO;

public class Wireframe {

	BufferedImage image, isolatedImage;
	int imageHeight,imageWidth;
	static String outputFile = "example_output.png";
	ArrayList<Point> points;

	List<ArrayList<Point>> pTs;

/*Settings*/

	int gridSize = 3;
	int connectedPoints = 10;
	int distanceOfPoints = 32;
	double scaleFactor = 2;
	/*Always false*/
	Boolean girdOverlap = false;

	double blackPercent = 0.0,redPercent = 1.5, greenPercent = 0.0, bluePercent = 0.2;

/*Settings*/

	public Wireframe(int width, int height) {
		image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		imageHeight = height;
		imageWidth = width;
	}

	public Wireframe(String inputFile) {
		try {
			image = ImageIO.read(new File(inputFile));

			imageWidth = image.getWidth();
			imageHeight = image.getHeight();

		}
		catch(IOException e) {
			e.printStackTrace();
		}
	}

	public void create() {

		this.grayScale();
		this.choosePoints();
		this.createPoints();
		this.drawLines();
		this.createImageFile(outputFile);
	}

	public void createPoints() {

		Boolean pointFoundOnLine = false;

		int rowsFound = 0;

		pTs = new ArrayList<ArrayList<Point>>();

		for(int i = 0;i < imageHeight;i++) {
			for(int j = 0;j < imageWidth;j++) {
				if(getGrayScale(image.getRGB(j, i)) != 0) {
					if(!pointFoundOnLine) {

						rowsFound++;
						pointFoundOnLine = true;
						pTs.add(new ArrayList<Point>());
						pTs.get(rowsFound - 1).add(new Point(j, i));

					}
					else {
						pTs.get(rowsFound - 1).add(new Point(j, i));	
					}
				}
			}
			pointFoundOnLine = false;
		}
	}

	public void drawLines() {

		BufferedImage newImage = new BufferedImage((int) (scaleFactor * imageWidth), (int) (scaleFactor * imageHeight), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2d = newImage.createGraphics();

		g2d.setColor(Color.BLACK);		
		g2d.fillRect(0,0, (int) (scaleFactor * imageWidth), (int) (scaleFactor * imageHeight));
		g2d.setColor(Color.WHITE);

		for(int i = 0;i < pTs.size();i++) {

			for(int j = 0;j < pTs.get(i).size();j++) {

				List<Point> nearest = new ArrayList<Point>();
				double scaleValue = 1;

				for(int m = -(distanceOfPoints/2);m < (distanceOfPoints - (distanceOfPoints/2));m++) {
					for(int n = -(distanceOfPoints/2);n < (distanceOfPoints - (distanceOfPoints/2));n++) {

						if((i + m > 0 && i + m < pTs.size())) {

							if( j + n > 0 && j + n < pTs.get(i + m).size()) {
								if(m != 0 && n != 0)
									nearest.add(pTs.get(i + m).get(j + n));
							}
						}

					}
				}

				double distance = -1,minDistance = -1;
				int minIndex = -1;
				Point tempPoint;

				for(int m = 0;m < nearest.size();m++) {
					for(int n = m ;n < nearest.size();n++) {
						if(distance == -1) {
							distance = getDistance(pTs.get(i).get(j), nearest.get(n));
							minDistance = distance;
							minIndex = n;
						}
						else {
							distance = getDistance(pTs.get(i).get(j), nearest.get(n));
							if(minDistance > distance) {
								minIndex = n;
								minDistance = distance; 
							}
						}
					}
					tempPoint = nearest.get(m);
					nearest.set(m, nearest.get(minIndex));
					nearest.set(minIndex, tempPoint);

					minIndex = -1;
					distance = -1;
					minDistance = -1;
				}

				if(nearest.size() > 0) {
					for(int k = 0;k < nearest.size() && k < connectedPoints;k++) {
						g2d.drawLine((int) (scaleFactor * pTs.get(i).get(j).getX()), (int) (scaleFactor * pTs.get(i).get(j).getY()), (int) (scaleFactor * nearest.get(k).getX()), (int) (scaleFactor * nearest.get(k).getY()));
					}
				}
			}
		}
		image = newImage;
	}

	public double getDistance(Point a, Point b) {

		double distance;
		
		distance = Math.sqrt(Math.pow(a.getX() - b.getX(), 2) + Math.pow(a.getY() - b.getY(), 2));

		return distance;

	} 

	public void grayScale() {
		BufferedImage grayScaleImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

		Color o,n;
		int sum = 0;

		for(int i = 0;i < imageWidth;i++) {
			for(int j = 0;j < imageHeight;j++) {
				o = new Color(image.getRGB(i, j));

				sum = o.getRed() + o.getGreen() + o.getBlue();
				sum /= 3;

				n = new Color(sum, sum, sum);

				grayScaleImage.setRGB(i, j, n.getRGB());
			}
		}

		image = grayScaleImage;
		grayScaleImage = null;
	}

	public double sigmoid(int x) {

		double transformedX = (x / 21.25) - 6;
		double sigmoid_Y = 1 / (1 + Math.pow(Math.E, -transformedX));

		return sigmoid_Y;
	}

	public void contrast() {
		BufferedImage contrastImage = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

		int color;

		for(int i = 0;i < imageWidth;i++) {
			for(int j = 0;j < imageHeight;j++) {
				color = getGrayScale(image.getRGB(i, j));

				color = (int) (255 * sigmoid(color));

				color = 0xff000000 | (color << 16) | (color << 8) | color;

				contrastImage.setRGB(i, j, color);
			}
		}

		image = contrastImage;

	}

	 public int getGrayScale(int rgb) {
        int r = (rgb >> 16) & 0xff;
        int g = (rgb >> 8) & 0xff;
        int b = (rgb) & 0xff;

        int gray = (int)(0.2126 * r + 0.7152 * g + 0.0722 * b);

        return gray;
    }

	public void choosePoints() {

		BufferedImage temp = new BufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_INT_RGB);

		Random r = new Random();

		int sum = 0; 
		Color o;

		if(girdOverlap) {

			for(int i = gridSize/2;i < imageWidth - (gridSize - 1);i++) {
				for(int j = gridSize/2;j < imageHeight - (gridSize - 1);j++) {

					sum = 0;
					for(int m = -(gridSize/2);m < (gridSize/2) + 1;m++) {
						for(int n = -(gridSize/2);n < (gridSize/2) + 1;n++) {
							o = new Color(image.getRGB(i + m, j + n));
							sum += o.getRed();
						}
					}
					sum /= gridSize * gridSize;

					if(sigmoid(sum) <= r.nextDouble()) {
						temp.setRGB(i, j, Color.BLACK.getRGB());
					}
					else {
						temp.setRGB(i, j, Color.WHITE.getRGB());	
					}
				}
			}
		}
		else {
			for(int i = 0;i < imageWidth/gridSize;i++) {
				for(int j = 0;j < imageHeight/gridSize;j++) {

					for(int m = -(gridSize/2);m < (gridSize/2) + 1;m++) {
						for(int n = -(gridSize/2);n < (gridSize/2) + 1;n++) {
							o = new Color(image.getRGB( (i*gridSize + (gridSize/2) ) + m, (j*gridSize + (gridSize/2) ) + n));
							sum += o.getRed();
						}
					}

					sum /= gridSize * gridSize;

					if(sigmoid(sum) <= r.nextDouble()) {
						temp.setRGB( i*gridSize + (gridSize/2)  , j*gridSize + (gridSize/2)  , Color.WHITE.getRGB());
					}
					else {
						temp.setRGB( i*gridSize + (gridSize/2)  , j*gridSize + (gridSize/2)  , Color.BLACK.getRGB());	
					}

				}
			}
		}

		image = temp;
		temp = null;
	}

	public void createImageFile(String fileName) {
		try{
			File imageFile = new File(fileName);
			ImageIO.write(image, "png", imageFile);
		}
		catch(IOException e) {
			System.out.println("Couldn't make image file!");
		}
	}

	public static void main(String args[]) {
		Wireframe i = new Wireframe("example_input.jpg");

		i.create();
	}
}