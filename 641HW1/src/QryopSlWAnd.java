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

		int orgSize = this.daatPtrs.size();
		while (this.daatPtrs.size() > 0) {

			int nextDocid = getSmallestCurrentDocid();

			// Create a new posting that is the union of the posting lists
			// that match the nextDocid.F
			double score = 1;
			int docID = nextDocid;
			for (int i = 0; i < this.daatPtrs.size(); i++) {
				DaaTPtr ptri = this.daatPtrs.get(i);

				if (ptri.scoreList.getDocid(ptri.nextDoc) == nextDocid) {

					score *= ptri.scoreList.getDocidScore(ptri.nextDoc);
					ptri.nextDoc++;

				} else {

					ArrayList<QryopSlScore> tmps = new ArrayList<QryopSlScore>();
					helper(tmps, args.get(i));

					double defaultScore = 1;
					for (QryopSlScore xxx : tmps) {
						defaultScore *= r.getDefaultScore(xxx.field, xxx.ctf,
								docID);
					}

					score *= defaultScore;
				}
			}

			// If a DaatPtr has reached the end of its list, remove it.
			// The loop is backwards so that removing an arg does not
			// interfere with iteration.
			// System.out
			// .println("score is:"
			// + score
			// + " weight is: "
			// + (this.weights.get(orgSize - this.daatPtrs.size()) /
			// this.totalWeight));
			result.docScores.add(
					docID,
					Math.pow(score,
							this.weights.get(orgSize - this.daatPtrs.size())
									/ this.totalWeight));
			// System.out.println("daatSize " + daatPtrs.size());

			for (int i = this.daatPtrs.size() - 1; i >= 0; i--) {
				DaaTPtr ptri = this.daatPtrs.get(i);

				if (ptri.nextDoc >= ptri.scoreList.scores.size()) {
					this.daatPtrs.remove(i);
				}
			}
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
