import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;

import javax.sound.sampled.Line;

public class QueryExpander {
	private int fbDocs;
	private int fbTerms;
	private int fbMu;

	public QueryExpander() {

	}

	public HashMap<Integer, Double> readTopKfbDocs(String fileName,int qryID)
			throws IOException {
		HashMap<Integer, Double> result = new HashMap<Integer, Double>();
		BufferedReader br = null;
		try {

			br = new BufferedReader(new FileReader(new File(fileName)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String line = null;
		String[] splittedLine = null;
		while (result.size() <= this.fbDocs && (line = br.readLine()) != null) {
			splittedLine = line.split(" ");
						
		}
		
		br.close();
		return result;

	}

	public String expandQuery(String orgQuery,
			HashMap<Integer, Double> doc2Score) {
		return null;
	}
}
