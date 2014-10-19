import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

public class RetrievalModelIndri extends RetrievalModel {

	private double mu;
	private double lambda;
	private DocLengthStore dls;
	HashMap<String, Integer> totalIndexInfo = new HashMap<String, Integer>();

	@Override
	public boolean setParameter(String parameterName, double value) {
		if (parameterName.equalsIgnoreCase("mu")) {
			setMu(value);
			return true;
		} else if (parameterName.equalsIgnoreCase("lambda")) {
			setLambda(value);
			return true;
		}
		return false;
	}

	public double calculateScore(InvList invl, int i) throws IOException {

		String field = invl.field;

		double lengthC = QryEval.READER.getSumTotalTermFreq(invl.field);// QryEval.READER.getSumTotalTermFreq(invl.field);
		double tf_qd = invl.getTf(i);
		double P_qc = (double) (invl.ctf) / lengthC;
		int DocID = invl.getDocid(i);

		double docLength = dls.getDocLength(field, DocID);// dls.getDocLength(invl.field,
															// DocID);
		double first = lambda * (tf_qd + mu * P_qc) / (docLength + mu);
		double second = (1 - lambda) * P_qc;
		// System.out.println(lengthC + "\t" + P_qc + "\t" + tf_qd + "\t" +
		// docLength
		// + "\t" + (first + second));
		return first + second;
	}

	public double getDefaultScore(String field, int ctf, int docID)
			throws IOException {

		double lengthC = QryEval.READER.getSumTotalTermFreq(field);// QryEval.READER.getSumTotalTermFreq(invl.field);
		double P_qc = ctf / lengthC;
		double docLength = dls.getDocLength(field, docID);// dls.getDocLength(field,
															// docID);
		return lambda * mu * P_qc / ((double) docLength + mu) + (1 - lambda)
				* P_qc;

	}

	@Override
	public boolean setParameter(String parameterName, String value) {
		// TODO Auto-generated method stub
		return false;
	}

	public DocLengthStore getDls() {
		return dls;
	}

	public void setDls(DocLengthStore dls) {
		this.dls = dls;
	}

	public double getMu() {
		return mu;
	}

	public void setMu(double mu) {
		this.mu = mu;
	}

	public double getLambda() {
		return lambda;
	}

	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

}
