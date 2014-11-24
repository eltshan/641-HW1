import java.io.IOException;
import java.util.HashMap;

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
				for (int i = 0; i < tv.stems.length; i++) {
					try {
						int df_t = tv.stemDf(i);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}

				}
			}

		}

	}

	static public double calculateBM25Score(TermVector tv) {
		int df_t = 0;
		int tf_t = 0;
		int doclen = 0;
		int avg_doclen = 0;
		int qtf_t = 0;
		int N = 0;

		return 0;
	}
}
