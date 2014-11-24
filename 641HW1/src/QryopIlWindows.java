import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.PriorityQueue;

import javax.swing.text.Position;

public class QryopIlWindows extends QryopIl {
	private int n;

	public QryopIlWindows(int k, Qryop... q) {
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
	 * Merge inverted lists that contains the required term The basic idea is
	 * the maintain a minHeap, and also get the max value Each time compare the
	 * minValue and the maxValue, if they are within the range of n, add it to
	 * result list if not, pop the minValue, and fetch the next Value
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

	class myClass {
		public myClass(int pos, int id) {
			this.position = pos;
			this.pointID = id;
		}

		int position;
		int pointID;
	}

	private void mergeInvertedList(int[] docPos, int n, QryResult result,
			int docID) {
		int curPos[] = new int[docPos.length];
		ArrayList<Integer> positions = new ArrayList<Integer>();

		Comparator<myClass> scoreComparator = new Comparator<myClass>() {
			@Override
			public int compare(myClass o1, myClass o2) {

				if (o1.position < o2.position)
					return -1;
				if (o1.position > o2.position)
					return 1;
				else
					return 0;
			};
		};

		PriorityQueue<myClass> minHeap = new PriorityQueue<myClass>(
				docPos.length, scoreComparator);

		DaaTPtr ptri = null;
		int currentMax = Integer.MIN_VALUE;
		for (int i = 0; i < this.daatPtrs.size(); i++) {
			ptri = this.daatPtrs.get(i);

			curPos[i] = 0;
			minHeap.add(new myClass(
					ptri.invList.postings.get(docPos[i]).positions
							.get(curPos[i]), i));
			if (ptri.invList.postings.get(docPos[i]).positions.get(curPos[i]) > currentMax) {
				currentMax = ptri.invList.postings.get(docPos[i]).positions
						.get(curPos[i]);
			}
			curPos[i]++;

		}
		boolean flag = true;
		// if flag = false, which means there is no window left
		// System.out.println("hello");
		while (flag) {

			if (currentMax - minHeap.peek().position <= n) {


				positions.add(minHeap.peek().position);
				// why did I clear the heap here?
				minHeap.clear();
				for (int i = 0; i < docPos.length; i++) {

					if (undateHeap(minHeap, docPos, curPos, i) == false) {
						flag = false;
						// System.out.println("fail!");
						break;
					}
					if (this.daatPtrs.get(i).invList.postings.get(docPos[i]).positions
							.get(curPos[i] - 1) > currentMax)
						currentMax = this.daatPtrs.get(i).invList.postings
								.get(docPos[i]).positions.get(curPos[i] - 1);
				}

			} else {
				// System.out.println("fail!");
				myClass tmp = minHeap.remove();
				if (undateHeap(minHeap, docPos, curPos, tmp.pointID) == false)
					flag = false;
				if (this.daatPtrs.get(tmp.pointID).invList.postings
						.get(docPos[tmp.pointID]).positions
						.get(curPos[tmp.pointID] - 1) > currentMax)
					currentMax = this.daatPtrs.get(tmp.pointID).invList.postings
							.get(docPos[tmp.pointID]).positions
							.get(curPos[tmp.pointID] - 1);
			}

		}

		if (positions.size() > 0) {

			result.invertedList.appendPosting(docID, positions);
			positions = new ArrayList<Integer>();

		}
	}

	private boolean undateHeap(PriorityQueue<myClass> minHeap, int[] docPos,
			int[] curPos, int i) {

		DaaTPtr ptri = this.daatPtrs.get(i);
		if (ptri.invList.postings.get(docPos[i]).positions.size() <= curPos[i])
			return false;
		minHeap.offer(new myClass(
				ptri.invList.postings.get(docPos[i]).positions.get(curPos[i]++),
				i));
		return true;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}
}
