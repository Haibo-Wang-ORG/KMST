package preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

import common.Util;

public class BuildDataFile {
	public void doBuildDataFile(String fromDirectory, String dataFile, Util.DATATYPE type) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dataFile)));
			int len;
			if (type == Util.DATATYPE.BEIJING) {
				len = Util.bjNum;
			} else if (type == Util.DATATYPE.BUS) {
				len = Util.busNum;
			} else {
				len = Util.truckNum;
			}
			
			for (int i=1; i<=len; i++) {
				BufferedReader reader = new BufferedReader(new FileReader(fromDirectory + i + ".txt"));
				String line;
				while ((line = reader.readLine()) != null) {
					writer.write(line + ";" + i + "\n");
				}
				reader.close();
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void buildWeights(String fromDirectory, String weightFile, Util.DATATYPE type) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(weightFile)));
			int len;
			if (type == Util.DATATYPE.BEIJING) {
				len = Util.bjNum;
			} else if (type == Util.DATATYPE.BUS) {
				len = Util.busNum;
			} else {
				len = Util.truckNum;
			}
			
			for (int i=1; i<=len; i++) {
				BufferedReader reader = new BufferedReader(new FileReader(fromDirectory + i + ".txt"));
				String line;
				float weights = 0;
				while ((line = reader.readLine()) != null) {
//					writer.write(line + ";" + i + "\n");
					weights += 1; // weights += line[j];
				}
				writer.write(i + ";" + weights + "\n");
				reader.close();
			}
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		BuildDataFile build = new BuildDataFile();
//		build.doBuildDataFile(Util.busDirectoryPath, Util.busDataFile, Util.DATATYPE.BUS);
//		build.doBuildDataFile(Util.truckDirectoryPath, Util.truckDataFile, Util.DATATYPE.TRUCK);
//		build.doBuildDataFile(Util.bjDirectoryPath, Util.bjDataFile, Util.DATATYPE.BEIJING);
		
//		build.buildWeights(Util.busDirectoryPath, Util.busWeights, Util.DATATYPE.BUS);
//		build.buildWeights(Util.truckDirectoryPath, Util.truckWeights, Util.DATATYPE.TRUCK);
		build.buildWeights(Util.bjDirectoryPath, Util.bjWeights, Util.DATATYPE.BEIJING);
	}

}
