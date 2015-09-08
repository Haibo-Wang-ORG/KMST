package preprocess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class Distribution {
	public static void main(String[] args) {
		try {
			BufferedReader reader = new BufferedReader(new FileReader(new File("./DataSet/Beijing/Directory/count.txt")));
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File("./DataSet/Beijing/Directory/distributin.txt")));
			String line;
			int num = 0;
			int tmp = 25;
			while ((line = reader.readLine()) != null) {
				String[] items = line.split("\t");
				int number = Integer.valueOf(items[1]);
				if (number == tmp) {
					num ++;
					continue;
				} else {
					writer.write(tmp + " " + num + "\n");
					tmp = number;
					num = 1;
				}
			}
			writer.write(tmp + " " + num + "\n");
			
			writer.flush();
			writer.close();
			reader.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
