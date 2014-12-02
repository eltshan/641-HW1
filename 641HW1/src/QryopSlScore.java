import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
	String field;
	int ctf;

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
		if (r instanceof RetrievalModelBM25) {
			RetrievalModelBM25 r25 = (RetrievalModelBM25) r;
			return evaluateBM25(r25);
		}
		if (r instanceof RetrievalModelIndri) {
			RetrievalModelIndri rIndri = (RetrievalModelIndri) r;
			return evaluateIndri(rIndri);
		}
		// if (r instanceof RetrievalModelUnrankedBoolean)
		return (evaluateBoolean(r));

		// return null;
	}

	private QryResult evaluateIndri(RetrievalModelIndri rIndri)
			throws IOException {
		QryResult result = args.get(0).evaluate(rIndri);

		// hash docID with its score
		InvList inv;

		inv = result.invertedList;

		for (int i = 0; i < result.invertedList.df; i++) {// for each term
			// pointer
			int docID = inv.getDocid(i);
			// double tf = inv.getTf(i);
			//
			// double doclen = rIndri.getDls().getDocLength(inv.field, docID);
			//
			// double score = lambda * (tf + mu * p) / (doclen + mu)
			// + (1 - lambda) * p;
			// System.out.println(C + "\t" + p + "\t" + tf + "\t" + doclen +
			// "\t"
			// + score);
			// System.out.println(score + " " + rIndri.calculateScore(inv, i));
			// result.docScores.add(docID, rIndri.calculateScore(inv, i));

			result.docScores.add(docID, rIndri.calculateScore(inv, i));

		}

		// total number of docs contains at least one term

		result.docScores.ctf = result.invertedList.ctf;
		this.field = result.invertedList.field;
		this.ctf = result.invertedList.ctf;
		freeDaaTPtrs();
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
			if (r instanceof RetrievalModelRankedBoolean) {
				result.docScores.add(result.invertedList.postings.get(i).docid,
						result.invertedList.getTf(i));

			} else if (r instanceof RetrievalModelUnrankedBoolean)
				result.docScores.add(result.invertedList.postings.get(i).docid,
						(float) 1.0);
		}

		// The SCORE operator should not return a populated inverted list.
		// If there is one, replace it with an empty inverted list.

		if (result.invertedList.df > 0)
			result.invertedList = new InvList();

		return result;
	}

	public QryResult evaluateBM25(RetrievalModelBM25 r) throws IOException {
		// Initialization

		QryResult result = args.get(0).evaluate(r);

		InvList inv;

		double k_1 = r.getK_1();
		double b = r.getB();

		double doc_len = 0.0;

		inv = result.invertedList;
		int df_t = inv.df;
		if (!r.docCount.containsKey(inv.field))
			r.docCount.put(inv.field, QryEval.READER.getDocCount(inv.field));
		int N = QryEval.READER.numDocs();

		if (!r.aveLength.containsKey(inv.field))
			r.aveLength.put(inv.field,
					(double) QryEval.READER.getSumTotalTermFreq(inv.field)
							/ (float) QryEval.READER.getDocCount(inv.field));
		double avgLen = r.aveLength.get(inv.field);
		double idf = Math.log((N - df_t + 0.5) / (df_t + 0.5));

		for (int i = 0; i < result.invertedList.df; i++) {// for each term
			int docID = inv.getDocid(i);
			int tf_td = inv.getTf(i);
			try {
				doc_len = r.getDls().getDocLength(inv.field, docID);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			double tf_weight = (tf_td / (tf_td + k_1
					* ((1 - b) + b * (doc_len / avgLen))));

			// System.out.println(idf + " " + tf_weight);
			result.docScores.add(docID, idf * tf_weight);
		}

		// total number of docs contains at least one term

		freeDaaTPtrs();
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

		if (r instanceof RetrievalModelUnrankedBoolean)
			return (0.0);

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
