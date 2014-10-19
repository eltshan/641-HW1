/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {

	private int ctf = 0;
	private String fieldString = "";
	public double MLE = 0;

	/**
	 * Construct a new SCORE operator. The SCORE operator accepts just one
	 * argument.
	 * 
	 * @param q
	 *            The query operator argument.
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore(Qryop q) {
		this.args.add(q);
	}

	/**
	 * Construct a new SCORE operator. Allow a SCORE operator to be created with
	 * no arguments. This simplifies the design of some query parsing
	 * architectures.
	 * 
	 * @return @link{QryopSlScore}
	 */
	public QryopSlScore() {
	}

	/**
	 * Appends an argument to the list of query operator arguments. This
	 * simplifies the design of some query parsing architectures.
	 * 
	 * @param q
	 *            The query argument to append.
	 */
	public void add(Qryop a) {
		this.args.add(a);
	}

	/**
	 * Evaluate the query operator.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluate(RetrievalModel r) throws IOException {

		if (r instanceof RetrievalModelUnrankedBoolean)
			return (evaluateBoolean(r));

		if (r instanceof RetrievalModelRankedBoolean)
			return (evaluateRankedBoolean(r));

		if (r instanceof RetrievalModelBM25)
			return (evaluateBM25(r));

		if (r instanceof RetrievalModelIndri)
			return (evaluateIndri((RetrievalModelIndri) r));

		return null;
	}

	public QryResult evaluateIndri(RetrievalModelIndri r) throws IOException {
		RetrievalModelIndri model = (RetrievalModelIndri) r;
		QryResult result = args.get(0).evaluate(r);

		double lambda = model.getLambda();
		double mu = model.getMu();

		fieldString = result.invertedList.field;
		ctf = result.invertedList.ctf;

		double C = QryEval.READER.getSumTotalTermFreq(fieldString);

		MLE = result.invertedList.ctf / (double) C;
		// System.out.println(MLE);

		int df = result.invertedList.df;

		for (int i = 0; i < result.invertedList.df; i++) {
			int docid = result.invertedList.postings.get(i).docid;
			int tf = result.invertedList.postings.get(i).tf;

			long doclen = model.getDls().getDocLength(fieldString, docid);

			double score = lambda * (tf + mu * MLE) / (doclen + mu)
					+ (1 - lambda) * MLE;
			// System.out.println(docid + score);

			result.docScores.add(docid, score);
		}
		return result;
	}

	public QryResult evaluateBM25(RetrievalModel r) throws IOException {
		RetrievalModelBM25 model = (RetrievalModelBM25) r;
		QryResult result = args.get(0).evaluate(r);
		//
		// fieldString = result.invertedList.field;
		//
		// double k1 = model.k1;
		// double b = model.b;
		// double k3 = model.k3;
		// int N = QryEval.READER.numDocs();
		//
		// double avglen = QryEval.READER.getSumTotalTermFreq(fieldString)
		// / (float) QryEval.READER.getDocCount(fieldString);
		//
		// int df = result.invertedList.df;
		// double p1 = Math.log((N - df + 0.5) / (df + 0.5));
		//
		// for (int i = 0; i < result.invertedList.df; i++) {
		// int docid = result.invertedList.postings.get(i).docid;
		// int tf = result.invertedList.postings.get(i).tf;
		//
		// long doclen = QryEval.dls.getDocLength(fieldString, docid);
		//
		// double score = p1
		// * (tf / (tf + k1 * ((1 - b) + b * (doclen / avglen))));
		//
		// result.docScores.add(docid, score);
		// }
		return result;
	}

	public QryResult evaluateRankedBoolean(RetrievalModel r) throws IOException {

		QryResult result = args.get(0).evaluate(r);
		for (int i = 0; i < result.invertedList.df; i++) {

			result.docScores.add(result.invertedList.postings.get(i).docid,
					result.invertedList.postings.get(i).tf);
		}

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/**
	 * Evaluate the query operator for boolean retrieval models.
	 * 
	 * @param r
	 *            A retrieval model that controls how the operator behaves.
	 * @return The result of evaluating the query.
	 * @throws IOException
	 */
	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Evaluate the query argument.

		QryResult result = args.get(0).evaluate(r);

		// Each pass of the loop computes a score for one document. Note:
		// If the evaluate operation above returned a score list (which is
		// very possible), this loop gets skipped.

		for (int i = 0; i < result.invertedList.df; i++) {

			// DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
			// Unranked Boolean. All matching documents get a score of 1.0.

			result.docScores.add(result.invertedList.postings.get(i).docid,
					(float) 1.0);
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	/*
	 * Calculate the default score for a document that does not match the query
	 * argument. This score is 0 for many retrieval models, but not all
	 * retrieval models.
	 * 
	 * @param r A retrieval model that controls how the operator behaves.
	 * 
	 * @param docid The internal id of the document that needs a default score.
	 * 
	 * @return The default score.
	 */
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {

		if (r instanceof RetrievalModelBM25)
			return (0.0);

		if (r instanceof RetrievalModelIndri) {

			RetrievalModelIndri model = (RetrievalModelIndri) r;

			double lambda = model.getLambda();
			double mu = model.getMu();

			long doclen = model.getDls().getDocLength(fieldString, (int) docid);
			// int tf = 0;

			double score = lambda * mu * MLE / ((double) doclen + mu)
					+ (1 - lambda) * MLE;

			return score;

		}

		return 0.0;
	}

	/**
	 * Return a string version of this query operator.
	 * 
	 * @return The string version of this query operator.
	 */
	public String toString() {

		String result = new String();

		for (Iterator<Qryop> i = this.args.iterator(); i.hasNext();)
			result += (i.next().toString() + " ");

		return ("#SCORE( " + result + ")");
	}
}
