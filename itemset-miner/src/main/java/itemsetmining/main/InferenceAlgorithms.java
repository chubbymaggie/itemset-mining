package itemsetmining.main;

import itemsetmining.itemset.Itemset;
import itemsetmining.transaction.Transaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

/** Container class for Inference Algorithms */
public class InferenceAlgorithms {

	/** Interface for the different inference algorithms */
	public interface InferenceAlgorithm {
		public HashSet<Itemset> infer(final Transaction transaction);
	}

	/**
	 * Infer ML parameters to explain transaction using greedy algorithm and
	 * store in covering.
	 * <p>
	 * This is an O(log(n))-approximation algorithm where n is the number of
	 * elements in the transaction.
	 */
	public static class InferGreedy implements InferenceAlgorithm, Serializable {
		private static final long serialVersionUID = 9173178089235828142L;

		@Override
		public HashSet<Itemset> infer(final Transaction transaction) {

			final HashSet<Itemset> covering = new HashSet<>();
			final int transactionSize = transaction.size();
			final Transaction coveredItems = new Transaction();

			final HashMap<Itemset, Double> cachedItemsets = transaction
					.getCachedItemsets();
			while (coveredItems.size() != transactionSize) {

				double minCostPerItem = Double.POSITIVE_INFINITY;
				Itemset bestSet = null;

				for (final Entry<Itemset, Double> entry : cachedItemsets
						.entrySet()) {

					final int notCovered = coveredItems.countUnion(entry
							.getKey()) - coveredItems.size();

					final double cost = -Math.log(entry.getValue());
					final double costPerItem = cost / notCovered;

					if (costPerItem < minCostPerItem) {
						minCostPerItem = costPerItem;
						bestSet = entry.getKey();
					}

				}

				if (bestSet != null) {
					covering.add(bestSet);
					coveredItems.add(bestSet);
				} else { // Allow incomplete coverings
					break;
				}

			}
			return covering;
		}

	}

	/**
	 * Infer ML parameters to explain transaction using Primal-Dual
	 * approximation and store in covering.
	 * <p>
	 * This is an O(mn) run-time f-approximation algorithm, where m is the no.
	 * elements to cover, n is the number of sets and f is the frequency of the
	 * most frequent element in the sets.
	 */
	public static class InferPrimalDual implements InferenceAlgorithm {

		@Override
		public HashSet<Itemset> infer(final Transaction transaction) {

			final HashSet<Itemset> covering = new HashSet<>();
			final Random rand = new Random();
			final List<Integer> notCoveredItems = new ArrayList<>(transaction);

			final HashMap<Itemset, Double> cachedItemsets = transaction
					.getCachedItemsets();

			// Calculate costs
			final HashMap<Itemset, Double> costs = new HashMap<>();
			for (final Entry<Itemset, Double> entry : cachedItemsets.entrySet()) {
				costs.put(entry.getKey(), -Math.log(entry.getValue()));
			}

			while (!notCoveredItems.isEmpty()) {

				double minCost = Double.POSITIVE_INFINITY;
				Itemset bestSet = null;

				// Pick random element
				final int index = rand.nextInt(notCoveredItems.size());
				final Integer element = notCoveredItems.get(index);

				// Increase dual of element as much as possible
				for (final Entry<Itemset, Double> entry : costs.entrySet()) {

					if (entry.getKey().contains(element)) {

						final double cost = entry.getValue();
						if (cost < minCost) {
							minCost = cost;
							bestSet = entry.getKey();
						}

					}
				}

				if (bestSet != null) {
					covering.add(bestSet);
					notCoveredItems.removeAll(bestSet);
				} else { // Allow incomplete coverings
					break;
				}

				// Make dual of element binding
				for (final Entry<Itemset, Double> entry : costs.entrySet()) {
					final Itemset set = entry.getKey();
					if (set.contains(element)) {
						final double cost = entry.getValue();
						costs.put(set, cost - minCost);
					}
				}

			}
			return covering;
		}

	}

	private InferenceAlgorithms() {

	}

}
