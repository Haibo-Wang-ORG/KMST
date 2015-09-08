package visulization;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import kmst.KMSTStar;


import common.Statistic;
import common.Trajectory;
import common.Util;


public class DrawTrajectories extends JFrame{
	private static final long serialVersionUID = -6720368520110137897L;

	String trajectories_directory = "./DataSet/Beijing/Directory/";
	String raw_trajectories_directory = "./DataSet/Beijing/Directory/";
	String raw_trajectories = "./DataSet/Extracted/trajectories.txt";
//	String raw_trajectories = "./DataSet/Random_Extracted/random_selected_trajectories.txt";
	
	BufferedImage img;
	
	int tminLon = 11597552;
	int tminLat = 3952585;
	int tmaxLon = 11725518;
	int tmaxLat = 4063243;
	
	int tdeltaHeight = tmaxLat - tminLat;
	int tdeltaWidth = tmaxLon - tminLon;
	
	int minLon = tminLon + tdeltaWidth/5;
	int minLat = tminLat + tdeltaHeight*1/5;
	int maxLon = tminLon + tdeltaWidth*5/5;
	int maxLat = tminLat + tdeltaHeight*1/2;
	
	int deltaHeight = maxLat - minLat;
	int deltaWidth = maxLon - minLon;
	
	int width, height;
	
	public DrawTrajectories(int height) {
		this.height = height;
		this.width = (int) ((float)this.height * ((float)deltaWidth / (float)deltaHeight));
		img = new BufferedImage(this.height, this.width, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.createGraphics();
//		Graphics g = EpsGraphics2D
//		drawTrajectories(g);
//		drawRawTrajectories(g);
		drawAllTrajectories(g);
		g.dispose();
	}
	
	public DrawTrajectories(int height, int[] tids) {
		this.height = height;
		this.width = (int) ((float)this.height * ((float)deltaWidth / (float)deltaHeight));
		img = new BufferedImage(this.height, this.width, BufferedImage.TYPE_INT_ARGB);
		Graphics g = img.createGraphics();
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int tid : tids) list.add(tid);
		drawTrajectories(g, list);
		g.dispose();
	}
	
	public void paint(Graphics eg) {
		eg.drawImage(img, 0, 0, null);
	}
	
//	private void drawTrajectories(Graphics g) {
//		try {
//			File file = new File(trajectories_directory);
//			String[] trajs = file.list();
//			
//			g.setColor(Color.blue);
//			for (String traj : trajs) {
//				BufferedReader reader = new BufferedReader(new FileReader(new File(trajectories_directory + traj)));
//				String line;
//				int preLon = -1, preLat = -1;
//				while ((line = reader.readLine()) != null) {
//					String[] fields = line.split(",");
//					float lon = Float.valueOf(fields[2]);
//					float lat = Float.valueOf(fields[3]);
//					if (preLon == -1 && preLat == -1) {
//						preLon = (int) lon;
//						preLat = (int) lat;
//						continue;
//					}
////					g.drawLine(getScreenLon(preLon), getScreenLat(preLat), getScreenLon((int)lon), getScreenLat((int)lat));
//					g.drawOval(getScreenLon((int)lon), getScreenLat((int)lat), 1, 1);
//					preLon = (int)lon;
//					preLat = (int)lat;
//				}
//				reader.close();
//			}
//			
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
//	private void drawRawTrajectories(Graphics g) {
//		try {
//			File file = new File(raw_trajectories_directory);
//			String[] trajs = file.list();
//			
//			g.setColor(Color.blue);
//			for (String traj : trajs) {
//				BufferedReader reader = new BufferedReader(new FileReader(new File(raw_trajectories_directory + traj)));
//				String line;
//				int preLon = -1, preLat = -1;
//				while ((line = reader.readLine()) != null) {
//					String[] fields = line.split(" ");
//					float lon = Float.valueOf(fields[0]);
//					float lat = Float.valueOf(fields[1]);
//					if (preLon == -1 && preLat == -1) {
//						preLon = (int) lon;
//						preLat = (int) lat;
//						continue;
//					}
//					g.drawOval(getScreenLon((int)lon), getScreenLat((int)lat), 1, 1);
//					preLon = (int)lon;
//					preLat = (int)lat;
//				}
//				reader.close();
//			}
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
	
	private void drawTrajectories(Graphics g, ArrayList<Integer> trajIDs) {
		try {
			File file = new File(trajectories_directory);
			String[] trajs = file.list();
			
//			g.setColor(Color.yellow);
			g.setColor(Color.black);
			int count = 1;
			for (String traj : trajs) {
				int id = Integer.valueOf(traj.split(".txt")[0]);
				if (!trajIDs.contains(id)) continue;
				if (count == 1) {
					g.setColor(Color.blue);
					count = 2;
				} else {
					g.setColor(Color.black);
				}
				BufferedReader reader = new BufferedReader(new FileReader(new File(trajectories_directory + traj)));
				String line;
				int preLon = -1, preLat = -1;
				while ((line = reader.readLine()) != null) {
					String[] fields = line.split(",");
					float lon = Float.valueOf(fields[2]);
					float lat = Float.valueOf(fields[3]);
					if (preLon == -1 && preLat == -1) {
						preLon = (int) lon;
						preLat = (int) lat;
						continue;
					}
//					g.drawLine(getScreenLon(preLon), getScreenLat(preLat), getScreenLon((int)lon), getScreenLat((int)lat));
					g.drawOval(getScreenLon((int)lon), getScreenLat((int)lat), 1, 1);
					preLon = (int)lon;
					preLat = (int)lat;
				}
				reader.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void drawAllTrajectories(Graphics g) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File(raw_trajectories)));
			
			String line;
			g.setColor(Color.black);
			while ((line = reader.readLine()) != null) {
				if (line.trim().equals("")) continue; // skip empty lines
				String[] fields = line.split(" ");
				float lon = Float.valueOf(fields[0]);
				float lat = Float.valueOf(fields[1]);
				g.drawOval(getScreenLon((int)lon), getScreenLat((int)lat), 1, 1);
			}
			
			System.out.println("Draw all trajectories complete!");
			
			File f = new File("./Dataset/f.jpg");
			ImageIO.write(img, "jpg", f);
			if (f.exists()) System.out.println("F exits!");
			else System.out.println("No F exists");
			
			reader.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private int getScreenLon(int lon) {
		return width*(lon-minLon)/deltaWidth;
	}
	
	private int getScreenLat(int lat) {
		return height - height*(lat-minLat)/deltaHeight;
	}
	
	public static void main(String[] args) {
//		KMSTStar wknnPlus = new KMSTStar();
//		Trajectory trajectory;
////		trajectory = Util.getTrajectory(297, Util.bjDirectoryPath);
//		trajectory = Util.getTrajectory(597, Util.bjDirectoryPath);
//		Statistic statistic = new Statistic();
//		long start = System.currentTimeMillis();
//		int[] tids = wknnPlus.kmstStar(wknnPlus.rtreeDB, trajectory, Util.EPSILON/2, Util.EPSILON, 20, statistic);
//		System.out.println("WKnnPlus takes " + (System.currentTimeMillis()-start));
//		
//		int len = 800;
//		DrawTrajectories show = new DrawTrajectories(len, tids);
//		
//		show.setBounds(50,50,len,len);
//		show.setBackground(Color.WHITE);
//		show.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//		show.setVisible(true);
	}
}
