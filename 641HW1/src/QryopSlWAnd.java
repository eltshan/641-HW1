/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlWAnd extends QryopSl {
	ArrayList<Double> weights = new ArrayList<Double>();
	double totalWeight = 0;

	/**
	 * It is convenient for the constructor to accept a variable number of
	 * arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
	 * 
	 * @param q
	 *            A query argument (a query operator).
	 */
	public QryopSlWAnd(Qryop... q) {
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

	public void addWeight(double weight) {
		weights.add(weight);
		totalWeight += weight;
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

		if (r instanceof RetrievalModelIndri) {
			RetrievalModelIndri rIndri = (RetrievalModelIndri) r;
			return evaluateIndri(rIndri);
		}
		return null;
	}

	private QryResult evaluateIndri(RetrievalModelIndri r) throws IOException {
		// Initialization

		allocDaaTPtrs(r);
		// System.out.println("argsize: " + args.size());
		QryResult result = new QryResult();
		int totalSize = this.args.size();

		while (totalSize > 0) {

			int nextDocid = getSmallestCurrentDocid();
			if (nextDocid < 0)
				break;
			// Create a new posting that is the union of the posting lists
			// that match the nextDocid.F
			double score = 1;
			int docID = nextDocid;
			for (int i = 0; i < this.daatPtrs.size(); i++) {
				double tmpScore = 1;
				DaaTPtr ptri = this.daatPtrs.get(i);
				if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
					double currentScore = ((QryopSl) this.args.get(i))
							.getDefaultScore(r, nextDocid);

					score = score
							* Math.pow(currentScore, weights.get(i)
									/ this.totalWeight);
					continue;
				}
				if (ptri.scoreList.getDocid(ptri.nextDoc) == nextDocid) {

					tmpScore *= ptri.scoreList.getDocidScore(ptri.nextDoc);
					ptri.nextDoc++;
					if (ptri.nextDoc >= ptri.scoreList.scores.size())
						totalSize--;
				} else {

					ArrayList<QryopSlScore> tmps = new ArrayList<QryopSlScore>();
					helper(tmps, args.get(i));

					double defaultScore = 1;
					for (QryopSlScore xxx : tmps) {
						defaultScore *= r.getDefaultScore(xxx.field, xxx.ctf,
								docID);
					}

					tmpScore *= defaultScore;
				}

				tmpScore = Math.pow(tmpScore, this.weights.get(i)
						/ this.totalWeight);
				score *= tmpScore;
			}

			result.docScores.add(docID, score);

		}

		freeDaaTPtrs();

		return result;

	}

	void helper(ArrayList<QryopSlScore> tmps, Qryop op) {
		if (op instanceof QryopSlScore) {
			tmps.add((QryopSlScore) op);
		} else {
			for (int i = 0; i < op.args.size(); i++) {
				helper(tmps, op.args.get(i));
			}
		}
	}

	public int getSmallestCurrentDocid() {

		int nextDocid = Integer.MAX_VALUE;

		for (int i = 0; i < this.daatPtrs.size(); i++) {
			DaaTPtr ptri = this.daatPtrs.get(i);
			if (ptri.nextDoc >= ptri.scoreList.scores.size())
				return -1;
			if (nextDocid > ptri.scoreList.getDocid(ptri.nextDoc))
				nextDocid = ptri.scoreList.getDocid(ptri.nextDoc);
		}

		return (nextDocid);
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

	/*
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (int i = 0; i < this.args.size(); i++)
			result += this.args.get(i).toString() + " ";

		return ("#AND( " + result + ")");
	}

	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}
}
