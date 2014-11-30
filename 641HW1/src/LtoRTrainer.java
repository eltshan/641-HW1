import java.io.IOException;
import java.util.HashMap;

import javax.swing.text.html.parser.DTD;

import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.flexible.core.util.StringUtils;

public class LtoRTrainer {

	static public void train(String query, int qid) {
		int k1 = 0;
		int k3 = 0;
		int b = 0;
		while (true) {
			String tmps[] = null;
			HashMap<String, Integer> map = new HashMap<String, Integer>();
			try {
				tmps = QryEval.tokenizeQuery(query);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			for (String str : tmps) {
				if (map.containsKey(str)) {
					map.put(str, map.get(str) + 1);
				} else {
					map.put(str, 1);
				}
			}
			TermVector tv = null;
			while (true) {// fetch each document
				String fileName = null;
				int score = 0;

				int docId = 0;

				LtoRFeature featureVector = new LtoRFeature(score, qid, 18,
						fileName);
				// get spam score
				Document d = null;
				try {
					d = QryEval.READER.document(docId);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int spamscore = Integer.parseInt(d.get("score"));
				// add spam score
				featureVector.addFeature(spamscore);
				// add url depth score
				String rawUrl = d.get("rawUrl");
				int numOfSlash = rawUrl.length()
						- rawUrl.replace("/", "").length();
				featureVector.addFeature(numOfSlash);
				// add wiki score
				int wikiScore = rawUrl.contains("wikipedia.org") ? 1 : 0;
				featureVector.addFeature(wikiScore);
				// addd page rank score

				// add bm25:

				// bm25 score with body
				double bm25Body = 0;

				try {
					docId = QryEval.getInternalDocid(fileName);
					tv = new TermVector(docId, "body");
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}// get doc ID

				bm25Body = calculateBM25Score(tv, "body", docId, k1, k3, b);
				
				//to be done
			}

		}

	}

	static public double calculateBM25Score(TermVector tv, String field,
			int docId, int k1, int k3, int b) {
		int df_t = 0;
		int tf_t = 0;
		int doclen = 0;
		double avgLength = 0;
		int qtf_t = 0;
		int N = 0;
		double bm25Score = 0.0;
		for (int i = 0; i < tv.stems.length; i++) {
			try {
				df_t = tv.stemDf(i);
				tf_t = tv.stemFreq(i);
				doclen = (int) QryEval.s.getDocLength(field, docId);
				N = QryEval.READER.getDocCount(field);
				avgLength = (double) (QryEval.READER.getSumTotalTermFreq(field) / N);
				bm25Score += Math.log((N - df_t + 0.5) / (df_t + 0.5)) * tf_t
						/ (tf_t + k1 * ((1 - b) + b * (doclen / avgLength)))
						* (k3 + 1) / k3;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		return bm25Score;
	}

	static public double calcualteIndriScore(
			HashMap<String, Integer> stringTerms, TermVector tv, String field,
			int docid, int lambda, int mu) throws IOException {
		double indriScore = 1.0;

		double lengthC = QryEval.READER.getSumTotalTermFreq(field);// QryEval.READER.getSumTotalTermFreq(invl.field);
		double tf_qd = 0;// invl.getTf(i);
		double P_qc = 0;// (double) (invl.ctf) / lengthC;

		double docLength = QryEval.s.getDocLength(field, docid);

		for (int i = 0; i < tv.stems.length; i++) {
			// if the term appears in the query
			tf_qd = 0;
			if (stringTerms.containsKey(tv.stemAt(i))) {
				tf_qd = tv.stemFreq(i);
			}
			P_qc = (double) tv.totalStemFreq(i) / lengthC;

			double first = lambda * (tf_qd + mu * P_qc) / (docLength + mu);
			double second = (1 - lambda) * P_qc;
			indriScore *= (first + second);
		}

		return indriScore;
	}
}
