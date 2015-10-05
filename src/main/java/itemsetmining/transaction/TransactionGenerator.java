package itemsetmining.transaction;

import itemsetmining.itemset.Itemset;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import org.apache.commons.io.LineIterator;
import org.apache.commons.math3.distribution.GeometricDistribution;
import org.apache.commons.math3.distribution.LogNormalDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import org.apache.commons.math3.random.Well19937c;

import com.google.common.collect.Maps;

public class TransactionGenerator {

	private static final boolean VERBOSE = false;

	/**
	 * Create interesting itemsets that highlight problems
	 *
	 * @param difficultyLevel
	 *            An integer between 0 and 10
	 *
	 * @param noInstances
	 *            The number of example itemset instances
	 */
	public static HashMap<Itemset, Double> generateExampleItemsets(
			final String name, final int noInstances, final int difficultyLevel) {

		final HashMap<Itemset, Double> itemsets = Maps.newHashMap();

		// Difficulty scaling (times 10^0 to 10^-1)
		final double scaling = Math.pow(10, -difficultyLevel / 10.);

		int maxElement = 80;
		for (int j = 0; j < noInstances; j++) {

			// Here [1, 2] is the champagne & caviar problem
			// (not retrieved when support is too high)
			if (name.equals("caviar")) {

				// Champagne & Caviar
				final Itemset s12 = new Itemset(maxElement + 1, maxElement + 2);
				final double p12 = 0.05 * scaling;
				itemsets.put(s12, p12);
				maxElement += 2;

			}
			// Here [B, 1] would be seen as a frequent itemset
			// if both [B] and [1] have sufficiently high support
			else if (name.equals("freerider")) {

				final Itemset s1 = new Itemset(maxElement + 1);
				final double p1 = 0.5 * scaling;
				itemsets.put(s1, p1);
				maxElement += 1;

			}
			// Here [1, B] is known as a cross-support pattern
			// if [1] has high support and [B] low support
			// (spuriously generated when support is too low)
			else if (name.equals("cross-supp")) {

				final Itemset s1 = new Itemset(maxElement + 1);
				final double p1 = 0.95 * scaling;
				itemsets.put(s1, p1);
				maxElement += 1;

			} else
				throw new IllegalArgumentException("Incorrect problem name.");

		}

		return itemsets;
	}

	/** Generate some disjoint itemsets as background noise */
	public static HashMap<Itemset, Double> generateBackgroundItemsets(
			final int noItemsets, final double p, final int noItems,
			final double mu, final double sigma) {

		final HashMap<Itemset, Double> backgroundItemsets = Maps.newHashMap();

		final GeometricDistribution sizeDist = new GeometricDistribution(
				new Well19937c(1), p);
		final UniformIntegerDistribution itemDist = new UniformIntegerDistribution(
				20, 20 + noItems - 1);
		final LogNormalDistribution probDist = new LogNormalDistribution(
				new Well19937c(1), mu, sigma);

		while (backgroundItemsets.size() < noItemsets) {
			final int len = sizeDist.sample() + 1; // use shifted geometric
			final Itemset set = new Itemset();
			for (int i = 0; i < len; i++) {
				final int samp = itemDist.sample();
				set.add(samp);
			}
			final double num = probDist.sample();
			backgroundItemsets.put(set, num);
		}

		return backgroundItemsets;
	}

	/**
	 * Generate transactions from set of interesting itemsets
	 *
	 * @return set of itemsets added to transaction
	 */
	public static HashMap<Itemset, Double> generateTransactionDatabase(
			final Map<Itemset, Double> itemsets, final int noTransactions,
			final File outFile) throws IOException {

		// Set random number seed
		final Random random = new Random(1);

		// Storage for itemsets actually added
		final HashMap<Itemset, Double> addedItemsets = Maps.newHashMap();

		// Set output file
		final PrintWriter out = new PrintWriter(outFile, "UTF-8");

		// Generate transaction database
		int count = 0;
		while (count < noTransactions) {

			// Generate transaction from distribution
			final Transaction transaction = sampleFromDistribution(random,
					itemsets, addedItemsets);
			for (final int item : transaction) {
				out.print(item + " ");
			}
			if (!transaction.isEmpty()) {
				out.println();
				count++;
			}

		}
		out.close();

		// Print file to screen
		if (VERBOSE) {
			final FileReader reader = new FileReader(outFile);
			final LineIterator it = new LineIterator(reader);
			while (it.hasNext()) {
				System.out.println(it.nextLine());
			}
			LineIterator.closeQuietly(it);
		}

		return addedItemsets;
	}

	/** Randomly generate itemset with its probability */
	private static Transaction sampleFromDistribution(final Random random,
			final Map<Itemset, Double> itemsets,
			final HashMap<Itemset, Double> addedItemsets) {

		final Transaction transaction = new Transaction();
		for (final Entry<Itemset, Double> entry : itemsets.entrySet()) {
			if (random.nextDouble() < entry.getValue()) {
				transaction.add(entry.getKey());
				addedItemsets.put(entry.getKey(), entry.getValue());
			}
		}

		return transaction;
	}
}
