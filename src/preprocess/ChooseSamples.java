package preprocess;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.Random;

import common.Util;

public class ChooseSamples {
	public void doSampling(Util.DATATYPE type) {
		String sampleFile;
		int N;
		if (type == Util.DATATYPE.BEIJING) {
			sampleFile = Util.bjSample;
			N = Util.bjNum;
		} else if (type == Util.DATATYPE.BUS) {
			sampleFile = Util.busSample;
			N = Util.busNum;
		} else {
			sampleFile = Util.truckSample;
			N = Util.truckNum;
		}
		
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(sampleFile)));
			Random random = new Random();
			
			for (int i=0; i<20; i++) {
				writer.write((random.nextInt(N)+1) + "\n");
			}
			
			writer.flush();
			writer.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		ChooseSamples sampling = new ChooseSamples();
		sampling.doSampling(Util.DATATYPE.BUS);
		sampling.doSampling(Util.DATATYPE.TRUCK);
		sampling.doSampling(Util.DATATYPE.BEIJING);
	}
}
