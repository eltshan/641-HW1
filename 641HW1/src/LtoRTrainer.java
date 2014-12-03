import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import javax.swing.text.html.parser.DTD;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.queryparser.flexible.core.util.StringUtils;
import org.apache.lucene.util.BytesRef;

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

	public void train(HashSet<Integer> SkippedFeatures,
			HashMap<String, Double> pageRankScore) throws Exception {
		// System.out.println(queryFileName);
		int qid = 0;
		BufferedReader qryBr = new BufferedReader(new FileReader(new File(
				queryFileName)));
		BufferedReader docBr = new BufferedReader(new FileReader(new File(
				docFileName)));
		BufferedWriter featureVectorBr = new BufferedWriter(new FileWriter(
				new File(featureVectorFileName)));
		String qryLine = null;
		String query = null;
		String[] parsedQryLine = null;
		String[] nextDocLine = docBr.readLine().split(" ");
		Double nonValue = Double.MAX_VALUE;
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

			System.out.println(qid);
			ArrayList<LtoRFeature> qryVector = new ArrayList<LtoRFeature>();
			while (nextDocLine != null
					&& Integer.parseInt(nextDocLine[0]) == qid) {// fetch each
				// document
				String[] docLine = nextDocLine;
				String tmptmpString = docBr.readLine();
				if (tmptmpString == null)
					nextDocLine = null;
				else {
					nextDocLine = tmptmpString.split(" ");
				}
				String fileName = docLine[2];
				int score = Integer.parseInt(docLine[3]);
				int docId = QryEval.getInternalDocid(fileName);

				LtoRFeature featureVector = new LtoRFeature(score, qid, 18,
						fileName);
				Document d = null;
				d = QryEval.READER.document(docId);
				// 1, get spam score
				if (SkippedFeatures.contains(1)) {
					featureVector.addFeature(nonValue);
				} else {

					int spamscore = Integer.parseInt(d.get("score"));
					featureVector.addFeature(spamscore);
				}

				// 2, add url depth score
				String rawUrl = d.get("rawUrl");

				if (SkippedFeatures.contains(2)) {
					featureVector.addFeature(nonValue);
				} else {
					int numOfSlash = rawUrl.length()
							- rawUrl.replace("/", "").length();
					featureVector.addFeature(numOfSlash);
				}

				// 3, add wiki score
				if (SkippedFeatures.contains(3)) {
					featureVector.addFeature(nonValue);
				} else {
					int wikiScore = rawUrl.contains("wikipedia.org") ? 1 : 0;
					featureVector.addFeature(wikiScore);
				}
				// 4, addd page rank score
				if (SkippedFeatures.contains(4)
						|| pageRankScore.get(fileName) == null)
					featureVector.addFeature(nonValue);
				else {
					featureVector.addFeature(pageRankScore.get(fileName));
				}

				// 5,6,7 score related to body
				String field = "body";
				Terms terms = QryEval.READER.getTermVector(docId, field);
				if (terms == null) {
					// field doesn't exist!
					featureVector.addFeature(nonValue);
					featureVector.addFeature(nonValue);
					featureVector.addFeature(nonValue);

				} else {
					tv = new TermVector(docId, field);
					if (!SkippedFeatures.contains(5))
						featureVector.addFeature(calculateBM25Score(map, tv,
								field, docId));
					else
						featureVector.addFeature(nonValue);
					if (!SkippedFeatures.contains(6))
						featureVector.addFeature(calcualteIndriScore(map, tv,
								field, docId));
					else
						featureVector.addFeature(nonValue);
					if (!SkippedFeatures.contains(7))
						featureVector
								.addFeature(calculateOverlapScore(map, tv));
					else
						featureVector.addFeature(nonValue);
				}
				// 8,9,10 score related to title
				field = "title";
				terms = QryEval.READER.getTermVector(docId, field);
				if (terms == null) {
					featureVector.addFeature(nonValue);
					featureVector.addFeature(nonValue);
					featureVector.addFeature(nonValue);
				} else {
					tv = new TermVector(docId, field);
					if (!SkippedFeatures.contains(8))
						featureVector.addFeature(calculateBM25Score(map, tv,
								field, docId));
					else
						featureVector.addFeature(nonValue);
					if (!SkippedFeatures.contains(9))
						featureVector.addFeature(calcualteIndriScore(map, tv,
								field, docId));
					else
						featureVector.addFeature(nonValue);
					if (!SkippedFeatures.contains(10))
						featureVector
								.addFeature(calculateOverlapScore(map, tv));
					else
						featureVector.addFeature(nonValue);
				}
				// 11,12,13 score related to url
				field = "url";
				terms = QryEval.READER.getTermVector(docId, field);
				if (terms == null) {
					featureVector.addFeature(nonValue);
					featureVector.addFeature(nonValue);
					featureVector.addFeature(nonValue);
				} else {
					tv = new TermVector(docId, field);
					if (!SkippedFeatures.contains(11))
						featureVector.addFeature(calculateBM25Score(map, tv,
								field, docId));
					else
						featureVector.addFeature(nonValue);
					if (!SkippedFeatures.contains(12))
						featureVector.addFeature(calcualteIndriScore(map, tv,
								field, docId));
					else
						featureVector.addFeature(nonValue);
					if (!SkippedFeatures.contains(13))
						featureVector
								.addFeature(calculateOverlapScore(map, tv));
					else
						featureVector.addFeature(nonValue);
				}
				// 14,15,16 score related to inlink
				field = "inlink";
				terms = QryEval.READER.getTermVector(docId, field);
				if (terms == null) {
					featureVector.addFeature(nonValue);
					featureVector.addFeature(nonValue);
					featureVector.addFeature(nonValue);
				} else {
					tv = new TermVector(docId, field);
					if (!SkippedFeatures.contains(14))
						featureVector.addFeature(calculateBM25Score(map, tv,
								field, docId));
					else
						featureVector.addFeature(nonValue);
					if (!SkippedFeatures.contains(15))
						featureVector.addFeature(calcualteIndriScore(map, tv,
								field, docId));
					else
						featureVector.addFeature(nonValue);
					if (!SkippedFeatures.contains(16))
						featureVector
								.addFeature(calculateOverlapScore(map, tv));
					else
						featureVector.addFeature(nonValue);
					// my own features

					// featureList.add(featureVector);
				}
				featureVector.addFeature(1);
				featureVector.addFeature(1);
				qryVector.add(featureVector);
				// featureVectorBr.write(featureVector.toString());
			}// end of current query
			normalize(qryVector);
			for (LtoRFeature tmp : qryVector)
				featureVectorBr.write(tmp.toString());

		}
		qryBr.close();
		docBr.close();
		featureVectorBr.close();

	}

	public void normalize(ArrayList<LtoRFeature> qryVector) {
		double nonValue = Double.MAX_VALUE;
		for (int i = 0; i < 18; i++) {
			double maxValue = Integer.MIN_VALUE;
			double minValue = Integer.MAX_VALUE;
			for (int j = 0; j < qryVector.size(); j++) {
				if (qryVector.get(j).getFeature(i) == nonValue)
					continue;

				if (qryVector.get(j).getFeature(i) > maxValue) {
					maxValue = qryVector.get(j).getFeature(i);
				}
				if (qryVector.get(j).getFeature(i) < minValue) {
					minValue = qryVector.get(j).getFeature(i);
				}
			}
			if (maxValue == minValue || maxValue == Integer.MIN_VALUE
					|| minValue == Integer.MAX_VALUE) {
				for (int j = 0; j < qryVector.size(); j++) {
					qryVector.get(j).setFeature(i, 0);
				}
			} else {
				for (int j = 0; j < qryVector.size(); j++) {

					if (qryVector.get(j).getFeature(i) == nonValue)
						qryVector.get(j).setFeature(i, 0);
					else
						qryVector.get(j).setFeature(
								i,
								(qryVector.get(j).getFeature(i) - minValue)
										/ (maxValue - minValue));
				}
			}
		}
	}

	public double calculateOverlapScore(HashMap<String, Integer> map,
			TermVector tv) {
		double overlapScore = 0;
		for (int i = 1; i < tv.stems.length; i++) {
			if (map.containsKey(tv.stems[i])) {
				overlapScore += 1;
			}
		}
		// System.out.println(overlapScore / map.size());
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
		int logN = 0;
		for (int i = 1; i < tv.stems.length; i++) {
			if (!stringTerms.containsKey(tv.stems[i])) {
				continue;
			}
			df_t = tv.stemDf(i);
			tf_t = tv.stemFreq(i);
			doclen = (int) QryEval.s.getDocLength(field, docId);
			N = QryEval.READER.getDocCount(field);

			logN = QryEval.READER.numDocs();
			avgLength = (double) (QryEval.READER.getSumTotalTermFreq(field) / N);
			double idf = Math.log((logN - df_t + 0.5) / (df_t + 0.5));
			double tf_weight = tf_t
					/ (tf_t + k1 * ((1 - b) + b * (doclen / avgLength)));

			bm25Score += idf * tf_weight;

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
		HashSet<String> tmpCopy = new HashSet<String>(stringTerms.keySet());
		for (int i = 1; i < tv.stems.length; i++) {
			// if the term appears in the query
			if (!stringTerms.containsKey(tv.stems[i])) {
				continue;
			}
			tmpCopy.remove(tv.stems[i]);
			tf_qd = tv.stemFreq(i);

			P_qc = (double) tv.totalStemFreq(i) / lengthC;

			double first = lambda * (tf_qd + mu * P_qc) / (docLength + mu);
			double second = (1 - lambda) * P_qc;
			indriScore *= (first + second);
		}

		if (tmpCopy.size() == stringTerms.size())
			return 0.0;

		for (String tmp : tmpCopy) {
			tf_qd = 0;

			P_qc = (double) QryEval.READER.totalTermFreq(new Term(field,
					new BytesRef(tmp))) / lengthC;

			double first = lambda * (tf_qd + mu * P_qc) / (docLength + mu);
			double second = (1 - lambda) * P_qc;
			indriScore *= (first + second);
		}
		tmpCopy.clear();
		indriScore = Math.pow(indriScore, 1.0 / stringTerms.size());
		return indriScore;
	}

}
