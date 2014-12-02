import java.util.ArrayList;

public class LtoRFeature {
	private int score;
	private int qID;
	private int numOfFeatures;
	private ArrayList<Double> features;
	private String docName;

	public LtoRFeature(int s, int id, int n, String name) {
		this.score = s;
		this.qID = id;
		this.numOfFeatures = n;
		this.docName = name;
		this.features = new ArrayList<Double>();
	}

	// public void normalize() {
	// double maxValue = Integer.MIN_VALUE;
	// double minValue = Integer.MIN_VALUE;
	// for (int i = 0; i < features.size(); i++) {
	// if (features.get(i) == 0) {
	// continue;
	// }
	// if (features.get(i) > maxValue) {
	// maxValue = features.get(i);
	// }
	// if (features.get(i) < minValue) {
	// minValue = features.get(i);
	// }
	// }
	// }

	public double getFeature(int i) {
		return features.get(i);
	}

	public void setFeature(int i, double d) {
		features.set(i, d);
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(score + " " + "qid:" + qID);
		for (int i = 0; i < features.size(); i++) {
			sb.append(" " + (i + 1) + ":" + features.get(i));
		}
		sb.append(" # " + docName + "\n");
		return sb.toString();
	}

	public void addFeature(double t) {
		features.add(t);
	}

	static public void getSpamScore() {
		// to do: read score from index
	}

	static public void getUrlDepth() {
		// £¿
	}

	static public void getWikiScore() {

	}
}