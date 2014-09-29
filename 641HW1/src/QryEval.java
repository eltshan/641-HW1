/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

import javax.management.modelmbean.ModelMBean;
import javax.naming.spi.DirStateFactory.Result;
import javax.swing.text.html.parser.Entity;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QryEval {

	static String usage = "Usage:  java "
			+ System.getProperty("sun.java.command") + " paramFile\n\n";

	// The index file reader is accessible via a global variable. This
	// isn't great programming style, but the alternative is for every
	// query operator to store or pass this value, which creates its
	// own headaches.
	private static int topK = 100;
	// dummy output when no result is retrived
	private static String dummyOutput = "10 Q0 dummy 1 0 run-1";
	public static IndexReader READER;

	// Create and configure an English analyzer that will be used for
	// query parsing.

	public static EnglishAnalyzerConfigurable analyzer = new EnglishAnalyzerConfigurable(
			Version.LUCENE_43);
	static {
		analyzer.setLowercase(true);
		analyzer.setStopwordRemoval(true);
		analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
	}

	/**
	 * @param args
	 *            The only argument is the path to the parameter file.
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {

		// must supply parameter file
		if (args.length < 1) {
			System.err.println(usage);
			System.exit(1);
		}

		double startTime = System.currentTimeMillis();
		// read in the parameter file; one parameter per line in format of
		// key=value
		Map<String, String> params = new HashMap<String, String>();
		Scanner scan = new Scanner(new File(args[0]));
		String line = null;
		do {
			line = scan.nextLine();
			String[] pair = line.split("=");
			params.put(pair[0].trim(), pair[1].trim());
		} while (scan.hasNext());
		scan.close();

		// parameters required for this example to run
		if (!params.containsKey("indexPath")) {
			System.err.println("Error: Parameters were missing.");
			System.exit(1);
		}

		// open the index
		READER = DirectoryReader.open(FSDirectory.open(new File(params
				.get("indexPath"))));

		if (READER == null) {
			System.err.println(usage);
			System.exit(1);
		}

		QryReader qryReader = new QryReader(params.get("queryFilePath"));
		DocLengthStore s = new DocLengthStore(READER);

		// RetrievalModel model = new RetrievalModelUnrankedBoolean();
		RetrievalModel model = null;
		if (params.get("retrievalAlgorithm").equalsIgnoreCase("RankedBoolean"))
			model = new RetrievalModelRankedBoolean();
		else if (params.get("retrievalAlgorithm").equalsIgnoreCase(
				"UnrankedBoolean"))
			model = new RetrievalModelUnrankedBoolean();
		else if (params.get("retrievalAlgorithm").equalsIgnoreCase("BM25")) {
			// RetrievalModelBM25 r25 = (RetrievalModelBM25) r;
			RetrievalModelBM25 model25 = new RetrievalModelBM25();

			model25.setParameter("k_1", params.get("BM25:k_1"));
			model25.setParameter("b", params.get("BM25:b"));
			model25.setParameter("k_3", params.get("BM25:k_3"));
			model25.setDls(new DocLengthStore(READER));
			model = model25;
		} else if (params.get("retrievalAlgorithm").equalsIgnoreCase("indri")) {
			RetrievalModelIndri modelAndri = new RetrievalModelIndri();
			// modelAndri.setParameter("mu", params.get(arg0))
		}
		/**
		 * The index is open. Start evaluating queries. The examples below show
		 * query trees for two simple queries. These are meant to illustrate how
		 * query nodes are created and connected. However your software will not
		 * create queries like this. Your software will use a query parser. See
		 * parseQuery.
		 * 
		 * The general pattern is to tokenize the query term (so that it gets
		 * converted to lowercase, stopped, stemmed, etc), create a Term node to
		 * fetch the inverted list, create a Score node to convert an inverted
		 * list to a score list, evaluate the query, and print results.
		 * 
		 * Modify the software so that you read a query from a file, parse it,
		 * and form the query tree automatically.
		 */

		// String queryNow = "12:#sum( freak)";
		// String[] tmpa = queryNow.split(":");
		// int queryIDNow = Integer.parseInt(tmpa[0]);
		// queryNow = tmpa[1];
		// BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
		// params.get("trecEvalOutputPath"))));
		// // System.out.println("ID is:" + queryID + " query is:" + query);
		// Qryop qTree;
		// qTree = parseQuery(queryNow, model);
		// QryResult Result = qTree.evaluate(model);
		// writeResults(queryNow, qTree.evaluate(model), queryIDNow, writer,
		// model);
		BufferedWriter writer = new BufferedWriter(new FileWriter(new File(
				params.get("trecEvalOutputPath"))));
		// for each input query
		String query = null;
		while ((query = qryReader.nextQuery()) != null) {
			String[] tmp = query.split(":");
			int queryID = Integer.parseInt(tmp[0]);
			query = tmp[1];

			// System.out.println("ID is:" + queryID + " query is:" + query);
			Qryop qTree;
			qTree = parseQuery(query, model);
			// QryResult Result = qTree.evaluate(model);
			writeResults(query, qTree.evaluate(model), queryID, writer, model);
		}
		writer.close();

		printMemoryUsage(false);
		System.out.println(System.currentTimeMillis() - startTime);
	}

	/**
	 * Write an error message and exit. This can be done in other ways, but I
	 * wanted something that takes just one statement so that it is easy to
	 * insert checks without cluttering the code.
	 * 
	 * @param message
	 *            The error message to write before exiting.
	 * @return void
	 */
	static void fatalError(String message) {
		System.err.println(message);
		System.exit(1);
	}

	/**
	 * Get the external document id for a document specified by an internal
	 * document id. If the internal id doesn't exists, returns null.
	 * 
	 * @param iid
	 *            The internal document id of the document.
	 * @throws IOException
	 */
	static String getExternalDocid(int iid) throws IOException {
		Document d = QryEval.READER.document(iid);
		String eid = d.get("externalId");
		return eid;
	}

	/**
	 * Finds the internal document id for a document specified by its external
	 * id, e.g. clueweb09-enwp00-88-09710. If no such document exists, it throws
	 * an exception.
	 * 
	 * @param externalId
	 *            The external document id of a document.s
	 * @return An internal doc id suitable for finding document vectors etc.
	 * @throws Exception
	 */
	static int getInternalDocid(String externalId) throws Exception {
		Query q = new TermQuery(new Term("externalId", externalId));

		IndexSearcher searcher = new IndexSearcher(QryEval.READER);
		TopScoreDocCollector collector = TopScoreDocCollector.create(1, false);
		searcher.search(q, collector);
		ScoreDoc[] hits = collector.topDocs().scoreDocs;

		if (hits.length < 1) {
			throw new Exception("External id not found.");
		} else {
			return hits[0].doc;
		}
	}

	/**
	 * parseQuery converts a query string into a query tree.
	 * 
	 * @param qString
	 *            A string containing a query.
	 * @param qTree
	 *            A query tree
	 * @throws IOException
	 */
	static Qryop parseQuery(String qString, RetrievalModel r)
			throws IOException {

		Qryop currentOp = null;
		Stack<Qryop> stack = new Stack<Qryop>();
		HashSet<String> fields = new HashSet<String>();
		fields.add("url");
		fields.add("keywords");
		fields.add("title");
		fields.add("inlink");
		fields.add("body");
		// Add a default query operator to an unstructured query. This
		// is a tiny bit easier if unnecessary whitespace is removed.

		qString = qString.trim();

		if (qString.charAt(0) != '#') {
			if (r instanceof RetrievalModelBM25)
				qString = "#SUM(" + qString + ")";
			else
				qString = "#or(" + qString + ")";
		}

		qString = "#score(" + qString + ")";
		// Tokenize the query.

		StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()",
				true);
		String token = null;
		// Each pass of the loop processes one token. To improve
		// efficiency and clarity, the query operator on the top of the
		// stack is also stored in currentOp.
		boolean lastIsSum = false;
		while (tokens.hasMoreTokens()) {

			token = tokens.nextToken();
			if (token.matches("[ ,(\t\n\r]")) {
				// Ignore most delimiters.
			} else if (token.equalsIgnoreCase("#and")) {
				currentOp = new QryopSlAnd();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#syn")) {
				currentOp = new QryopIlSyn();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#or")) {
				currentOp = new QryopSIOr();
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#sum")) {
				currentOp = new QryopSlSum();
				stack.push(currentOp);
			} else if (token.length() > 6
					&& token.substring(0, 5).equalsIgnoreCase("#near")) {
				currentOp = new QryopIlNear(
						Integer.parseInt(token.substring(6)));
				// System.out.println(token.substring(6));
				stack.push(currentOp);
			} else if (token.equalsIgnoreCase("#score")) {
				currentOp = new QryopSlScore();
				stack.push(currentOp);
			}

			else if (token.startsWith(")")) { // Finish current query
												// operator.
				// If the current query operator is not an argument to
				// another query operator (i.e., the stack is empty when it
				// is removed), we're done (assuming correct syntax - see
				// below). Otherwise, add the current operator as an
				// argument to the higher-level operator, and shift
				// processing back to the higher-level operator.

				stack.pop();

				if (stack.empty())
					break;

				Qryop arg = currentOp;
				currentOp = stack.peek();
				currentOp.add(arg);
			} else {

				// NOTE: You should do lexical processing of the token before
				// creating the query term, and you should check to see whether
				// the token specifies a particular field (e.g., apple.title).

				// some times after lexical processing I get nothing. In this
				// case the term should be ignored
				String tmp;
				if (token.lastIndexOf(".") == -1)
					tmp = token;
				else
					tmp = token.substring(0, token.lastIndexOf("."));
				String tmps[] = tokenizeQuery(tmp);
				if (tmps == null || tmps.length == 0)
					continue;
				tmp = tmps[0];

				if (currentOp instanceof QryopSlSum) {
					String[] splits = tmp.split(".");

					if (splits.length > 1
							&& fields.contains(splits[splits.length - 1]))
						currentOp.add(new QryopSlScore(new QryopIlTerm(tmp,
								splits[splits.length - 1])));

					else
						currentOp.add(new QryopSlScore(new QryopIlTerm(tmp)));
				}
				// add field indicator
				else {
					if (token.endsWith(".url"))
						currentOp.add(new QryopIlTerm(tmp, "url"));
					else if (token.endsWith(".keywords"))
						currentOp.add(new QryopIlTerm(tmp, "keywords"));
					else if (token.endsWith(".title"))
						currentOp.add(new QryopIlTerm(tmp, "title"));
					else if (token.endsWith(".inlink"))
						currentOp.add(new QryopIlTerm(tmp, "inlink"));
					else
						currentOp.add(new QryopIlTerm(tmp));
				}
			}
		}

		// A broken structured query can leave unprocessed tokens on the
		// stack, so check for that.

		if (tokens.hasMoreTokens()) {
			System.err
					.println("Error:  Query syntax is incorrect.  " + qString);
			return null;
		}

		return currentOp;
	}

	/**
	 * Print a message indicating the amount of memory used. The caller can
	 * indicate whether garbage collection should be performed, which slows the
	 * program but reduces memory usage.
	 * 
	 * @param gc
	 *            If true, run the garbage collector before reporting.
	 * @return void
	 */
	public static void printMemoryUsage(boolean gc) {

		Runtime runtime = Runtime.getRuntime();

		if (gc) {
			runtime.gc();
		}

		System.out
				.println("Memory used:  "
						+ ((runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L))
						+ " MB");
	}

	/**
	 * write evaluate result to file
	 * 
	 * @param queryName
	 * @param result
	 * @param queryID
	 * @param writer
	 * @param model
	 * @throws IOException
	 */
	public static void writeResults(String queryName, QryResult result,
			int queryID, BufferedWriter writer, final RetrievalModel model)
			throws IOException {

		if (result.docScores.scores.size() < 1) {

			writer.write(dummyOutput);
		} else {
			// comparator for score list. score will be the first key and name
			// of external file is the second key
			Comparator<ScoreList.ScoreListEntry> scoreComparator = new Comparator<ScoreList.ScoreListEntry>() {

				@Override
				public int compare(ScoreList.ScoreListEntry o1,
						ScoreList.ScoreListEntry o2) {
					// TODO Auto-generated method stub
					// if (model instanceof RetrievalModelRankedBoolean || model
					// instanceof ) {
					if (o1.getScore() < o2.getScore())
						return -1;
					if (o1.getScore() > o2.getScore())
						return 1;
					// }
					String A = null;
					try {
						A = getExternalDocid(o1.getDocid());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					String B = null;
					try {
						B = getExternalDocid(o2.getDocid());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					return B.compareTo(A);

				};

			};
			// sort the whole score list will be intuituve way but it's slow.
			// this optimization of using a minHeap reduce time complexity from
			// O(nlogn) to O(nlogk)
			PriorityQueue<ScoreList.ScoreListEntry> minHeap = new PriorityQueue<ScoreList.ScoreListEntry>(
					topK, scoreComparator);

			for (ScoreList.ScoreListEntry entity : result.docScores.scores) {
				if (minHeap.size() < topK)
					minHeap.offer(entity);
				else if (scoreComparator.compare(minHeap.peek(), entity) < 0) {
					minHeap.remove();
					minHeap.offer(entity);
				}
			}

			int i = 0;
			ScoreList.ScoreListEntry entity = null;
			Stack<ScoreList.ScoreListEntry> stack = new Stack<ScoreList.ScoreListEntry>();
			while ((entity = minHeap.poll()) != null) {
				stack.push(entity);
			}
			if (model instanceof RetrievalModelRankedBoolean) {

				while (!stack.isEmpty()) {
					writer.write(queryID + " " + "Q0" + " "
							+ getExternalDocid(stack.peek().getDocid()) + " "
							+ (i + 1) + " " + stack.peek().getScore() + " "
							+ "run-1");
					writer.write("\n");
					stack.pop();
					i++;
				}
			}
			if (model instanceof RetrievalModelUnrankedBoolean) {
				while (!stack.isEmpty()) {
					writer.write(queryID + " " + "Q0" + " "
							+ getExternalDocid(stack.peek().getDocid()) + " "
							+ (i + 1) + " " + 1.0 + " " + "run-1");
					writer.write("\n");
					stack.pop();
					i++;

				}
			}
			if (model instanceof RetrievalModelBM25) {

				while (!stack.isEmpty()) {
					writer.write(queryID + " " + "Q0" + " "
							+ getExternalDocid(stack.peek().getDocid()) + " "
							+ (i + 1) + " " + stack.peek().getScore() + " "
							+ "run-1");
					writer.write("\n");
					stack.pop();
					i++;
				}
			}
		}
	}

	/**
	 * Print the query results.
	 * 
	 * THIS IS NOT THE CORRECT OUTPUT FORMAT. YOU MUST CHANGE THIS METHOD SO
	 * THAT IT OUTPUTS IN THE FORMAT SPECIFIED IN THE HOMEWORK PAGE, WHICH IS:
	 * 
	 * QueryID Q0 DocID Rank Score RunID
	 * 
	 * @param queryName
	 *            Original query.
	 * @param result
	 *            Result object generated by {@link Qryop#evaluate()}.
	 * @throws IOException
	 */
	static void printResults(String queryName, QryResult result, int queryID,
			final RetrievalModel model) throws IOException {
		HashMap<Integer, Integer> x = new HashMap<Integer, Integer>();
		// System.out.println(queryName + ":  ");
		if (result.docScores.scores.size() < 1) {
			System.out.println("\tNo results.");
		} else {

			Collections.sort(result.docScores.scores,
					new Comparator<ScoreList.ScoreListEntry>() {

						@Override
						public int compare(ScoreList.ScoreListEntry o1,
								ScoreList.ScoreListEntry o2) {
							// TODO Auto-generated method stub
							if (model instanceof RetrievalModelRankedBoolean) {
								if (o1.getScore() < o2.getScore())
									return 1;
								if (o1.getScore() > o2.getScore())
									return -1;
							}
							String A = null;
							try {
								A = getExternalDocid(o1.getDocid());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							String B = null;
							try {
								B = getExternalDocid(o2.getDocid());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							return A.compareTo(B);

						};

					});
			if (model instanceof RetrievalModelRankedBoolean) {

				for (int i = 0; i < Math.min(result.docScores.scores.size(),
						topK); i++) {
					System.out.println(queryID + "\t" + "Q0" + "\t"
							+ getExternalDocid(result.docScores.getDocid(i))
							+ "\t" + (i + 1) + "\t"
							+ result.docScores.getDocidScore(i) + "\t"
							+ "run-1");
				}
			}
			if (model instanceof RetrievalModelUnrankedBoolean) {
				for (int i = 0; i < Math.min(result.docScores.scores.size(),
						topK); i++) {
					System.out.println(queryID + "\t" + "Q0" + "\t"
							+ getExternalDocid(result.docScores.getDocid(i))
							+ "\t" + (i + 1) + "\t" + 1.0 + "\t" + "run-1");
				}
			}
			if (model instanceof RetrievalModelBM25) {

				for (int i = 0; i < Math.min(result.docScores.scores.size(),
						topK); i++) {
					System.out.println(queryID + "\t" + "Q0" + "\t"
							+ getExternalDocid(result.docScores.getDocid(i))
							+ "\t" + (i + 1) + "\t"
							+ result.docScores.getDocidScore(i) + "\t"
							+ "run-1");
				}
			}
		}
	}

	/**
	 * Given a query string, returns the terms one at a time with stopwords
	 * removed and the terms stemmed using the Krovetz stemmer.
	 * 
	 * Use this method to process raw query terms.
	 * 
	 * @param query
	 *            String containing query
	 * @return Array of query tokens
	 * @throws IOException
	 */
	static String[] tokenizeQuery(String query) throws IOException {

		TokenStreamComponents comp = analyzer.createComponents("dummy",
				new StringReader(query));
		TokenStream tokenStream = comp.getTokenStream();

		CharTermAttribute charTermAttribute = tokenStream
				.addAttribute(CharTermAttribute.class);
		tokenStream.reset();

		List<String> tokens = new ArrayList<String>();
		while (tokenStream.incrementToken()) {
			String term = charTermAttribute.toString();
			tokens.add(term);
		}
		return tokens.toArray(new String[tokens.size()]);
	}
}
