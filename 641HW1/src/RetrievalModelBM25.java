import java.io.IOException;
import java.util.HashMap;

public class RetrievalModelBM25 extends RetrievalModel {

	private double k_1;
	private double b;
	private double k_3;
	private DocLengthStore dls;
	public HashMap<String, Double> aveLength = new HashMap<String, Double>();
	public HashMap<String, Integer> docCount = new HashMap<String, Integer>();

	public double calculateScore(InvList inv, int j) throws IOException {
		int docID = inv.getDocid(j);
		int tf_td = inv.getTf(j);
		int tf_t = inv.ctf;
		double k_1 = this.getK_1();
		double b = this.getB();

		int df_t = inv.df;
		double doc_len = 0.0;
		if (!docCount.containsKey(inv.field))
			docCount.put(inv.field, QryEval.READER.getDocCount(inv.field));
		int N = docCount.get(inv.field);
		if (!aveLength.containsKey(inv.field))
			aveLength.put(inv.field,
					(double) QryEval.READER.getSumTotalTermFreq(inv.field) / N);

		double avgLen = aveLength.get(inv.field);

		try {
			doc_len = this.getDls().getDocLength(inv.field, docID);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		double idf = Math.log((N - df_t + 0.5) / (df_t + 0.5));
		double tf_weight = tf_td
				/ (tf_t + k_1 * ((1 - b) + b * (doc_len / avgLen)));

		return idf * tf_weight;
		// * user_weight;
	}

	@Override
	public boolean setParameter(String parameterName, double value) {
		if (parameterName.equals("k_1"))
			setK_1(value);
		else if (parameterName.equals("b"))
			setB(value);
		else if (parameterName.equals("k_3"))
			setK_3(value);
		else
			return false;
		return true;
	}

	@Override
	public boolean setParameter(String parameterName, String value) {
		// TODO Auto-generated method stub
		if (parameterName.equals("k_1"))
			setK_1(Double.parseDouble(value));
		else if (parameterName.equals("b"))
			setB(Double.parseDouble(value));
		else if (parameterName.equals("k_3"))
			setK_3(Double.parseDouble(value));
		else
			return false;
		return true;
	}

	public double getK_1() {
		return k_1;
	}

	public void setK_1(double k_1) {
		this.k_1 = k_1;
	}

	public double getK_3() {
		return k_3;
	}

	public void setK_3(double k_3) {
		this.k_3 = k_3;
	}

	public double getB() {
		return b;
	}

	public void setB(double b) {
		this.b = b;
	}

	public DocLengthStore getDls() {
		return dls;
	}

	public void setDls(DocLengthStore dls) {
		this.dls = dls;
	}

}
