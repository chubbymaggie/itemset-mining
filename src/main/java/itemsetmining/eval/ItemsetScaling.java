package itemsetmining.eval;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferGreedy;
import itemsetmining.main.ItemsetMining;
import itemsetmining.main.ItemsetMiningCore;
import itemsetmining.transaction.TransactionGenerator;
import itemsetmining.util.Logging;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.io.output.TeeOutputStream;

public class ItemsetScaling {

	/** Main Settings */
	private static final File dbFile = new File(
			"/disk/data1/jfowkes/itemset.txt");
	private static final File saveDir = new File("/disk/data1/jfowkes/logs/");

	/** Set of mined itemsets to use for background */
	private static final String name = "plants-based";
	private static final File itemsetLog = new File(
			"/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/Logs/plants-20.10.2014-11:12:45.log");

	/** Spark Settings */
	private static final boolean useSpark = false;
	private static final Level LOG_LEVEL = Level.FINE;
	private static final long MAX_RUNTIME = 6 * 60; // 6hrs
	private static final int maxStructureSteps = 100_000;
	private static final int maxEMIterations = 100;

	public static void main(final String[] args) throws IOException,
			InterruptedException {

		// Run
		scalingTransactions(64, new int[] { 1_000, 10_000, 100_000, 1_000_000,
				10_000_000, 100_000_000 });

		// generateSyntheticDatabase(
		// 34781,
		// new File(
		// "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/Itemsets/plants_synthetic.dat"));
	}

	public static void scalingTransactions(final int noCores, final int[] trans)
			throws IOException, InterruptedException {

		final double[] time = new double[trans.length];
		final DecimalFormat formatter = new DecimalFormat("0.0E0");

		// Save to file
		String prefix = "";
		if (useSpark)
			prefix = "spark_";
		final FileOutputStream outFile = new FileOutputStream(saveDir + "/"
				+ prefix + name + "_scaling_" + noCores + ".txt");
		final TeeOutputStream out = new TeeOutputStream(System.out, outFile);
		final PrintStream ps = new PrintStream(out);
		System.setOut(ps);

		// Read in previously mined itemsets
		final Map<Itemset, Double> itemsets = ItemsetMiningCore
				.readIIMItemsets(itemsetLog);
		System.out.print("\n============= ACTUAL ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n",
					entry.getKey(), entry.getValue()));
		}
		System.out.println("\nNo itemsets: " + itemsets.size());
		System.out.println("No items: " + countNoItems(itemsets.keySet()));

		transloop: for (int i = 0; i < trans.length; i++) {

			final int tran = trans[i];
			System.out.println("\n========= " + formatter.format(tran)
					+ " Transactions");

			// Generate transaction database
			TransactionGenerator.generateTransactionDatabase(itemsets, tran,
					dbFile);
			printTransactionDBStats(dbFile);

			// Mine itemsets
			final File logFile = Logging.getLogFileName("IIM", true, saveDir,
					dbFile);
			final long startTime = System.currentTimeMillis();
			if (useSpark)
				runSpark(noCores);
			else
				ItemsetMining.mineItemsets(dbFile, new InferGreedy(),
						maxStructureSteps, maxEMIterations, logFile);

			final long endTime = System.currentTimeMillis();
			final double tim = (endTime - startTime) / (double) 1000;
			time[i] += tim;

			System.out.printf("Time (s): %.2f%n", tim);

			if (tim > MAX_RUNTIME * 60)
				break transloop;

		}

		// Print time
		System.out.println("\n========" + name + "========");
		System.out.println("Transactions:" + Arrays.toString(trans));
		System.out.println("Time: " + Arrays.toString(time));

		// and save to file
		out.close();
	}

	private static void runSpark(final int noCores) {
		final String cmd[] = new String[8];
		cmd[0] = "/afs/inf.ed.ac.uk/user/j/jfowkes/Code/git/itemset-mining/run-spark.sh";
		cmd[1] = "-f " + dbFile;
		cmd[2] = " -s " + maxStructureSteps;
		cmd[3] = " -i " + maxEMIterations;
		cmd[4] = " -c " + noCores;
		cmd[5] = " -l " + LOG_LEVEL;
		cmd[6] = " -r " + MAX_RUNTIME;
		cmd[7] = " -t true";
		MTVItemsetMining.runScript(cmd);
	}

	public static void generateSyntheticDatabase(final int noTransactions,
			final File dbPath) throws IOException {

		final Map<Itemset, Double> itemsets = ItemsetMiningCore
				.readIIMItemsets(itemsetLog);
		System.out.print("\n============= ACTUAL ITEMSETS =============\n");
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			System.out.print(String.format("%s\tprob: %1.5f %n",
					entry.getKey(), entry.getValue()));
		}

		System.out.print("\n");
		System.out.println("No itemsets: " + itemsets.size());
		TransactionGenerator.generateTransactionDatabase(itemsets,
				noTransactions, dbPath);
		printTransactionDBStats(dbPath);

	}

	/**
	 * Count the number of items in the itemsets (itemsets need not be
	 * independent)
	 */
	public static int countNoItems(final Set<Itemset> itemsets) {
		final Set<Integer> items = new HashSet<>();
		for (final Itemset itemset : itemsets)
			items.addAll(itemset);
		return items.size();
	}

	/** Print useful statistics for the transaction database */
	public static void printTransactionDBStats(final File dbFile)
			throws IOException {

		int noTransactions = 0;
		double sparsity = 0;
		final Set<Integer> singletons = new HashSet<>();
		final LineIterator it = FileUtils.lineIterator(dbFile, "UTF-8");
		while (it.hasNext()) {
			final String[] items = it.nextLine().trim().split(" ");
			for (final String item : items)
				singletons.add(Integer.parseInt(item));
			sparsity += items.length;
			noTransactions++;
		}
		LineIterator.closeQuietly(it);

		System.out.println("\nDatabase: " + dbFile);
		System.out.println("Items: " + singletons.size());
		System.out.println("Transactions: " + noTransactions);
		System.out.println("Avg. items per transaction: " + sparsity
				/ noTransactions + "\n");

	}

}
