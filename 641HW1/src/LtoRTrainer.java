import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.text.html.parser.DTD;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queryparser.flexible.core.util.StringUtils;

public class LtoRTrainer {

	double k1;
	double k3;
	double b;
	double mu;
	double lambda;
	String queryFileName;
	String docFileName;
	String pageRankFileName;
	String featureVectorFileName;

	public void train(String query, int qid) throws Exception {
		System.out.println(queryFileName);
		BufferedReader qryBr = new BufferedReader(new FileReader(new File(
				queryFileName)));
		BufferedReader docBr = new BufferedReader(new FileReader(new File(
				docFileName)));
		BufferedWriter featureVectorBr = new BufferedWriter(new FileWriter(
				new File(featureVectorFileName)));
		String qryLine = null;
		String[] parsedQryLine = null;
		while ((qryLine = qryBr.readLine()) != null) {// for each query
			String tmps[] = null;
			parsedQryLine = qryLine.split(":");
			qid = Integer.parseInt(parsedQryLine[0]);
			query = parsedQryLine[1];
			HashMap<String, Integer> map = new HashMap<String, Integer>();

			tmps = QryEval.tokenizeQuery(query);

			for (String str : tmps) {
				if (map.containsKey(str)) {
					map.put(str, map.get(str) + 1);
				} else {
					map.put(str, 1);
				}
			}
			TermVector tv = null;
			// ArrayList<LtoRFeature> featureList = new
			// ArrayList<LtoRFeature>();

			for (int i = 0; i < 10; i++) {// fetch each document
				String[] docLine = docBr.readLine().split(" ");
				String fileName = docLine[2];
				// System.out.println(fileName);
				int score = Integer.parseInt(docLine[3]);

				int docId = QryEval.getInternalDocid(fileName);

				LtoRFeature featureVector = new LtoRFeature(score, qid, 18,
						fileName);
				// get spam score
				Document d = null;
				d = QryEval.READER.document(docId);

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

				// add body field related score:

				// bm25 score with body

				Terms terms = QryEval.READER.getTermVector(docId, "body");
				if (terms == null) {
					// field doesn't exist!
					// QryEval.debugOut("Doc missing field: " + docId + " " +
					// "body");
				} else {
					tv = new TermVector(docId, "body");

					featureVector.addFeature(calculateBM25Score(map, tv,
							"body", docId));
					featureVector.addFeature(calcualteIndriScore(map, tv,
							"body", docId));
					featureVector.addFeature(calculateOverlapScore(map, tv));
				}
				// title
				terms = QryEval.READER.getTermVector(docId, "title");
				if (terms == null) {

				} else {
					tv = new TermVector(docId, "title");

					featureVector.addFeature(calculateBM25Score(map, tv,
							"title", docId));
					featureVector.addFeature(calcualteIndriScore(map, tv,
							"title", docId));
					featureVector.addFeature(calculateOverlapScore(map, tv));
				}
				// url

				terms = QryEval.READER.getTermVector(docId, "url");
				if (terms == null) {

				} else {
					tv = new TermVector(docId, "url");

					featureVector.addFeature(calculateBM25Score(map, tv, "url",
							docId));
					featureVector.addFeature(calcualteIndriScore(map, tv,
							"url", docId));
					featureVector.addFeature(calculateOverlapScore(map, tv));
				}
				//
				terms = QryEval.READER.getTermVector(docId, "inlink");
				if (terms == null) {

				} else {
					tv = new TermVector(docId, "inlink");

					featureVector.addFeature(calculateBM25Score(map, tv,
							"inlink", docId));
					featureVector.addFeature(calcualteIndriScore(map, tv,
							"inlink", docId));
					featureVector.addFeature(calculateOverlapScore(map, tv));

					// my own features
					featureVector.addFeature(0);
					featureVector.addFeature(1);
					// featureList.add(featureVector);
				}

				featureVectorBr.write(featureVector.toString());
			}

		}

	}

	public double calculateOverlapScore(HashMap<String, Integer> map,
			TermVector tv) {
		double overlapScore = 0;
		for (int i = 0; i < tv.stems.length; i++) {
			if (map.containsKey(tv.stemAt(i))) {
				overlapScore += 1;
			}
		}
		return overlapScore / map.size();

	}

	public double calculateBM25Score(HashMap<String, Integer> stringTerms,
			TermVector tv, String field, int docId) throws IOException {
		int df_t = 0;
		int tf_t = 0;
		int doclen = 0;
		double avgLength = 0;
		int qtf_t = 0;
		int N = 0;
		double bm25Score = 0.0;
		for (int i = 0; i < tv.stems.length; i++) {
			if (!stringTerms.containsKey(tv.stemAt(i))) {
				continue;
			}

			df_t = tv.stemDf(i);
			tf_t = tv.stemFreq(i);
			doclen = (int) QryEval.s.getDocLength(field, docId);
			N = QryEval.READER.getDocCount(field);
			avgLength = (double) (QryEval.READER.getSumTotalTermFreq(field) / N);
			bm25Score += Math.log((N - df_t + 0.5) / (df_t + 0.5)) * tf_t
					/ (tf_t + k1 * ((1 - b) + b * (doclen / avgLength)))
					* (k3 + 1) / k3;

		}
		return bm25Score;
	}

	public double calcualteIndriScore(HashMap<String, Integer> stringTerms,
			TermVector tv, String field, int docid) throws IOException {
		double indriScore = 1.0;

		double lengthC = QryEval.READER.getSumTotalTermFreq(field);// QryEval.READER.getSumTotalTermFreq(invl.field);
		double tf_qd = 0;// invl.getTf(i);
		double P_qc = 0;// (double) (invl.ctf) / lengthC;

		double docLength = QryEval.s.getDocLength(field, docid);

		for (int i = 1; i < tv.stems.length; i++) {
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
