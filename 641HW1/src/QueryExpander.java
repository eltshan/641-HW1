import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.TreeMap;

import javax.sound.sampled.Line;

import org.apache.lucene.search.similarities.TFIDFSimilarity;

public class QueryExpander {
	private int fbDocs;
	private int fbTerms;
	private int fbMu;
	private double lengthC = 0;
	private DocLengthStore dls = null;

	public class Term_Score {
		String term;
		double score;

		public Term_Score(String t, double s) {
			this.term = t;
			this.score = s;
		}
	}

	public QueryExpander(int docs, int terms, int mu) {
		this.fbDocs = docs;
		this.fbTerms = terms;
		this.fbMu = mu;
		try {
			this.lengthC = QryEval.READER.getSumTotalTermFreq("body");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public HashMap<String, Double> readTopKfbDocs(String fileName, int qryID)
			throws IOException {
		// <docID, score>
		System.out.println(fileName);
		HashMap<String, Double> result = new HashMap<String, Double>();
		BufferedReader br = null;
		try {

			br = new BufferedReader(new FileReader(new File(fileName)));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		String line = null;
		String[] splittedLine = null;
		while (result.size() < this.fbDocs && (line = br.readLine()) != null) {

			splittedLine = line.split(" ");
			if (Integer.parseInt(splittedLine[0]) == qryID) {

				result.put(splittedLine[2], Double.parseDouble(splittedLine[4]));
			}
		}

		br.close();
		return result;

	}

	public String expandQuery(String orgQuery,
			HashMap<Integer, Double> doc2Score) {
		TermVector tv = null;
		double score = 0;
		Map<String, Double> scores = new HashMap<String, Double>();
		PriorityQueue<Map.Entry<String, Double>> minHeap = new PriorityQueue<Map.Entry<String, Double>>(
				this.fbTerms, new Comparator<Map.Entry<String, Double>>() {

					@Override
					public int compare(Entry<String, Double> o1,
							Entry<String, Double> o2) {
						if (o1.getValue() > o2.getValue())
							return 1;
						else if (o1.getValue() < o2.getValue())
							return -1;
						return 0;
					}
				});
		for (int docId : doc2Score.keySet()) {
			// System.out.println("docID is:" + docId);
			int docLength = 0;

			try {
				tv = new TermVector(docId, "body");
				docLength = (int) dls.getDocLength("body", docId);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			for (int i = 0; i < tv.stems.length; i++) {
				String term = tv.stems[i];

				if (term == null || term.contains("."))
					continue;

				if (!scores.containsKey(term))
					scores.put(term, 0.0);
				try {
					double MLE = (tv.totalStemFreq(i) / this.lengthC);
					score = (tv.stemFreq(i) + this.fbMu * MLE)
							/ (docLength + this.fbMu) * doc2Score.get(docId)
							* Math.log(this.lengthC / tv.totalStemFreq(i));

					scores.put(term, scores.get(term) + score);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				// score = this.
				// scores.put(term, scores.get(term) + tv.stemFreq(i) + this)
			}
		}

		for (Map.Entry<String, Double> pair : scores.entrySet()) {
			// System.out.print(minHeap.size() + " " + pair.getKey());
			// System.out.println(pair.getKey() + " " + pair.getValue());
			if (minHeap.size() < this.fbTerms) {
				minHeap.offer(pair);
			} else {
				if (minHeap.peek().getValue() < pair.getValue()) {
					minHeap.remove();
					minHeap.add(pair);
				}
			}
		}

		StringBuffer sb = new StringBuffer();
		sb.append("#WAND ( ");
		while (!minHeap.isEmpty()) {

			sb.append(minHeap.peek().getValue() + " " + minHeap.peek().getKey()
					+ " ");
			minHeap.remove();
		}

		sb.append(")");

		return sb.toString();
	}

	public void setDls(DocLengthStore d) {
		this.dls = d;
	}
}
