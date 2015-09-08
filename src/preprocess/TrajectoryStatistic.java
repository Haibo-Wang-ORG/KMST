package preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Random;

public class TrajectoryStatistic {
	public void doStatistic() {
		String directory_path = "./DataSet/Beijing/Directory/";
		int num = 10779;
		ArrayList<TrajCount> trajCounts = new ArrayList<TrajCount>();
		
		try {
			for (int i=0; i<num; i++) {
				BufferedReader reader = new BufferedReader(new FileReader(new File(directory_path+i+".txt")));
				String line;
				int c = 0;
				while ((line = reader.readLine()) != null) {
					c ++;
				}
				trajCounts.add(new TrajCount(i, c));
				reader.close();
			}
			Collections.sort(trajCounts, new TrajCounComparator());
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(directory_path+"count.txt")));
			for (TrajCount trajCount : trajCounts) {
				writer.write(trajCount.trajId + "\t" + trajCount.num + "\n");
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void sampleTrajectories() {
		String directory_path = "./DataSet/Beijing/Directory/";
		int num = 10779;
		ArrayList<TrajCount> trajCounts = new ArrayList<TrajCount>();
		
		ArrayList<Integer> lists = new ArrayList<Integer>();
		Random rand = new Random();
		for (int i=0; i<20; i++) {
			lists.add(rand.nextInt(num));
		}
		
		try {
			for (Integer i : lists) {
				BufferedReader reader = new BufferedReader(new FileReader(new File(directory_path+i+".txt")));
				String line;
				int c = 0;
				while ((line = reader.readLine()) != null) {
					c ++;
				}
				trajCounts.add(new TrajCount(i, c));
				reader.close();
			}
			Collections.sort(trajCounts, new TrajCounComparator());
			
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(directory_path+"sample.txt")));
			for (TrajCount trajCount : trajCounts) {
				writer.write(trajCount.trajId + ";" + trajCount.num + "\n");
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	class TrajCount{
		public int trajId;
		public int num;
		
		public TrajCount(int trajId, int num) {
			this.trajId = trajId;
			this.num = num;
		}
	}
	
	class TrajCounComparator implements Comparator<TrajCount> {
		@Override
		public int compare(TrajCount o1, TrajCount o2) {
			return (o1.num > o2.num) ? 1 : 0;
		}
	}
	
	public static void main(String[] args) {
		TrajectoryStatistic statistic = new TrajectoryStatistic();
//		statistic.doStatistic();
		statistic.sampleTrajectories();
	}
}
