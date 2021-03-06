import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class QryopSIOr extends QryopSl {

	public QryopSIOr(Qryop... q) {
		for (int i = 0; i < q.length; i++)
			this.args.add(q[i]);
	}

	@Override
	public double getDefaultScore(RetrievalModel r, long docid)
			throws IOException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void add(Qryop q) throws IOException {
		// TODO Auto-generated method stub
		this.args.add(q);

	}

	@Override
	public QryResult evaluate(RetrievalModel r) throws IOException {
		// TODO Auto-generated method stub
		// if (r instanceof RetrievalModelUnrankedBoolean)
		return (evaluateBoolean(r));
		// return null;
	}

	public QryResult evaluateBoolean(RetrievalModel r) throws IOException {

		// Initialization

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
							Math.max(map.get(docID),
									ptri.scoreList.getDocidScore(j)));
				}

			}
		}

		for (int key : map.keySet()) {
			result.docScores.add(key, map.get(key));
		}
		freeDaaTPtrs();

		return result;
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}
}
