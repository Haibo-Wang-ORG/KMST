package kmst;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import common.Trajectory;
import common.Util;

public class SequentialScan {
	Util.DATATYPE type;
	
	public SequentialScan(Util.DATATYPE type) {
		this.type = type;
	}
	
	
	public static void main(String[] args) {
//		new SequentialScan(Util.DATATYPE.TRUCK).sequentialScan(Util.getTrajectory(37, Util.DATATYPE.TRUCK), 20);
		
//		new SequentialScan(Util.DATATYPE.TRUCK).doStatistic();
//		new SequentialScan(Util.DATATYPE.BUS).doStatistic();
		
		long start = System.currentTimeMillis();
		new SequentialScan(Util.DATATYPE.BEIJING).sequentialScan(Util.getTrajectory(7777, Util.DATATYPE.BEIJING), 20);
		System.out.println(System.currentTimeMillis()-start);
	}
	
	public int [] sequentialScan(Trajectory Q, int k) {
		int K = k;
		int [] ArrayT = new int[K];
		float [] ArrayW = new float[K];
		for (int i=0; i<K; i++)
			ArrayW[i] = Float.MAX_VALUE;
		
		int len;
		if (type == Util.DATATYPE.BEIJING) {
			len = Util.bjNum;
		} else if (type == Util.DATATYPE.BUS) {
			len = Util.busNum;
		} else {
			len = Util.truckNum;
		}
		
		for (int m=1; m<=len; m++) {
			Trajectory trajectory = Util.getTrajectory(m, type);
			
			float distance = Util.distance(trajectory, Q, type);
			int i;
			if (distance < ArrayW[K-1]) {
				for (i=0; i<K; i++) {
					if (distance < ArrayW[i]) {
						for (int j=K-1; j>i; j--) {
							ArrayW[j] = ArrayW[j-1];
							ArrayT[j] = ArrayT[j-1];
						}
						break;
					}
				}
				ArrayW[i] = distance;
				ArrayT[i] = m;
			}
		}
		
//		for (int i=0; i<K; i++)
//			System.out.println(ArrayT[i] + ":\t" + ArrayW[i]);
		
		return ArrayT;
	}
	
	public void doStatistic() {
		int testNum = 1;
		int K = 20;
		
		try {
			BufferedWriter writer;
			BufferedReader reader;
			if (type == Util.DATATYPE.BEIJING) {
				writer = new BufferedWriter(new FileWriter(new File(Util.bjScan)));
				reader = new BufferedReader(new FileReader(new File(Util.bjSample)));
			} else if (type == Util.DATATYPE.BUS) {
				writer = new BufferedWriter(new FileWriter(new File(Util.busScan)));
				reader = new BufferedReader(new FileReader(new File(Util.busSample)));
			} else {
				writer = new BufferedWriter(new FileWriter(new File(Util.truckScan)));
				reader = new BufferedReader(new FileReader(new File(Util.truckSample)));
			}
			
			String line;
			while ((line = reader.readLine()) != null) {
				String[] items = line.split(";");
				int tid = Integer.valueOf(items[0]);
				Trajectory query = Util.getTrajectory(tid, type);
				
				long start = System.currentTimeMillis();
				for (int i=0; i<testNum; i++) {
					sequentialScan(query, K);
				}
				long end = System.currentTimeMillis();
				long time = (end - start) / testNum;
				writer.write(tid + ";" + time + "\n");
				System.out.println(tid + " processed!");
			}
			
			reader.close();
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
//	public void doStatistic1() {
//		int testNum = 1;
//		
//		try {
//			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(Util.truckScan)));
//			BufferedReader reader = new BufferedReader(new FileReader(new File(Util.truckSample)));
//			String line;
//			while ((line = reader.readLine()) != null) {
//				String[] items = line.split(";");
//				int tid = Integer.valueOf(items[0]);
////				int num = Integer.valueOf(items[1]);
//				Trajectory query = Util.getTrajectory(tid, Util.DATATYPE.TRUCK);
//				Statistic statistic = new Statistic();
////				wknn(rtreeDB, query, Util.EPSILON, K, statistic); // abandon the first one
//				long start = System.currentTimeMillis();
//				for (int i=0; i<testNum; i++) {
//					sequentialScan(query, 5, Util.DATATYPE.TRUCK);
//				}
//				long end = System.currentTimeMillis();
//				long time = (end - start) / testNum;
//				writer.write(tid + ";" + time + "\n");
//				System.out.println(tid + " processed!");
//			}
//			reader.close();
//			writer.flush();
//			writer.close();
//		} catch (Exception e) {
//			e.printStackTrace();
//		}
//	}
}
