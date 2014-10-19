/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlSum extends QryopSl {

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
	 * 
	 * @param q
	 *            A query argument (a query operator).
	 */
	public QryopSlSum(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param {q} q The query argument (query operator) to append.
	 * @return void
	 * @throws IOException
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluates the query operator, including any child operators and returns
	 * the result.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelBM25)
			return evaluateBM25(r);
		return null;
	}

	/**
	 * Evaluates the query operator for boolean retrieval models, including any
	 * child operators and returns the result.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */

	public QryResult evaluateBM25(RetrievalModel r) throws IOException {
		allocDaaTPtrs(r);

		QryResult result = new QryResult();

		// integer is docid / double is the score
		HashMap<Integer, Double> map = new HashMap<Integer, Double>();

		for (int i = 0; i < this.daatPtrs.size(); i++) {// for each term pointer
			DaaTPtr ptri = this.daatPtrs.get(i);
			for (int j = 0; j < ptri.scoreList.scores.size(); j++) {
				// put current docID to scorelist
				int docID = ptri.scoreList.getDocid(j);
				if (!map.keySet().contains(docID)) {
					map.put(docID, ptri.scoreList.getDocidScore(j));

				} else {
					// recalculate the score using min
					map.put(docID,
							map.get(docID) + ptri.scoreList.getDocidScore(j));
				}

			}
		}

		for (int key : map.keySet()) {
			result.docScores.add(key, map.get(key));
		}
		freeDaaTPtrs();

		return result;
	}

	/*
	 * Calculate the default score for the specified document if it does not
	 * match the query operator. This score is 0 for many retrieval models, but
	 * not all retrieval models.
	 * 
	 * @param r A retrieval model that controls how the operator behaves.
	 * 
	 * @param docid The internal id of the document that needs a default score.
	 * 
	 * @return The default score.
	 */
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean)
			return (0.0);

		return 0.0;
	}

	/*
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";

		return ("#SUM( " + result + ")");
	}
}
