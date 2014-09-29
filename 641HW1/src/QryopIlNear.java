import java.io.IOException;
import java.util.ArrayList;

public class QryopIlNear extends QryopIl {
	private int n;

	public QryopIlNear(int k, Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
		this.n = k;
	}

	@Override
	public void add(Qryop q) throws IOException {
		// TODO Auto-generated method stub
		this.args.add(q);

	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		// //if (r instanceof RetrievalModelUnrankedBoolean)
		return (evaluateBoolean(r));

		// return null;
	}

	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Initialization

		allocDaaTPtrs(r);
		// syntaxCheckArgResults(this.daatPtrs);

		QryResult result = new QryResult();
		result.invertedList.field = new String(
				this.daatPtrs.get(0).invList.field);
		// Each pass of the loop adds 1 document to result until all of
		// the inverted lists are depleted. When a list is depleted, it
		// is removed from daatPtrs, so this loop runs until daatPtrs is empty.

		// This implementation is intended to be clear. A more efficient
		// implementation would combine loops and use merge-sort.

		DaaTPtr ptr0 = this.daatPtrs.get(0);
		int[] docPos = new int[daatPtrs.size()];
		EVALUATEDOCUMENTS: for (; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc++) {

			int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);
			// Do the other query arguments have the ptr0Docid?

			for (int j = 1; j < this.daatPtrs.size(); j++) {

				DaaTPtr ptrj = this.daatPtrs.get(j);
				while (true) {
					if (ptrj.nextDoc >= ptrj.invList.postings.size())
						break EVALUATEDOCUMENTS; // No more docs can match
					else if (ptrj.invList.getDocid(ptrj.nextDoc) > ptr0Docid)
						continue EVALUATEDOCUMENTS; // The ptr0docid can't
													// match.
					else if (ptrj.invList.getDocid(ptrj.nextDoc) < ptr0Docid)
						ptrj.nextDoc++; // Not yet at the right doc.
					else {
						docPos[j] = ptrj.nextDoc;
						break; // ptrj matches ptr0Docid
					}
				}
			}
			docPos[0] = ptr0.nextDoc;

			// The ptr0Docid matched all query arguments, so save it.
			mergeInvertedList(docPos, n, result, ptr0Docid);
			// result.docScores.add(ptr0Docid, docScore);

		}

		freeDaaTPtrs();

		return result;
	}

	/**
	 * Merge inverted lists that contains the required term
	 * 
	 * @param docPos
	 *            Each inverted list has its own pointer indicating current
	 *            position in given doc
	 * @param n
	 * @param result
	 * @param docID
	 *            For different inverted list, the same document stored in
	 *            different position
	 */
	private void mergeInvertedList(int[] docPos, int n, QryResult result,
			int docID) {
		double docScore = 0.0;
		DaaTPtr ptr0 = this.daatPtrs.get(0);
		int curPos[] = new int[docPos.length];
		if (docPos[0] >= ptr0.invList.postings.size())
			return;
		ArrayList<Integer> positions = new ArrayList<Integer>();
		for (int i = 0; i < ptr0.invList.postings.get(docPos[0]).positions
				.size(); i++) {
			// for each position i in the ptr0
			// set current position to i
			DaaTPtr ptrLast = ptr0;
			curPos[0] = i;
			int j;

			for (j = 1; j < this.daatPtrs.size(); j++) {
				// for each daatPtr
				DaaTPtr ptrj = this.daatPtrs.get(j);
				if (ptrj.invList.postings.get(docPos[j]).positions
						.size() <= curPos[j]) {
					break;
				}
				int tmpCurPos = ptrj.invList.postings.get(docPos[j]).positions
						.get(curPos[j]);

				if (ptrLast.invList.postings.get(docPos[j - 1]).positions
						.size() <= curPos[j - 1]) {
					break;
				}
				int tmpLasPos = ptrLast.invList.postings.get(docPos[j - 1]).positions
						.get(curPos[j - 1]);

				ptrLast = ptrj;

				while (tmpCurPos <= tmpLasPos) {// current < last

					if (curPos[j] + 1 >= ptrj.invList.postings.get(docPos[j]).positions
							.size()) {
						tmpCurPos = Integer.MAX_VALUE;
						break;
					}
					tmpCurPos = ptrj.invList.postings.get(docPos[j]).positions
							.get(++curPos[j]);

				}

				if (tmpCurPos - tmpLasPos > n) {
					break;
				}
			}
			// when j == daatPtrs.size, it means that all daatPtrs has a
			// position matched, therefore score++
			if (j == this.daatPtrs.size()) {
				docScore++;
				for (int iii = 1; iii < curPos.length; iii++)
					curPos[iii]++;
				// not sure about the position, will come back later
				positions.add(curPos[j - 1]);

			}
		}// end of outer loop

		if (docScore > 0) {

			// result.docScores.add(docID, docScore);
			result.invertedList.appendPosting(docID, positions);
			positions = new ArrayList<Integer>();

		}
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}
}
