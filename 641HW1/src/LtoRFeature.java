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

	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(score + " " + "qid:" + qID);
		for (int i = 0; i < numOfFeatures; i++) {
			sb.append(" " + (i + 1) + ":" + features.get(i));
		}
		sb.append(" # " + docName);
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