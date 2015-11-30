package itemsetmining.main;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.collect.Multiset;

import itemsetmining.itemset.Itemset;
import itemsetmining.main.InferenceAlgorithms.InferenceAlgorithm;
import itemsetmining.transaction.Transaction;
import itemsetmining.transaction.TransactionDatabase;
import scala.Tuple2;

/** Class to hold the various transaction EM Steps */
public class EMStep {

	/** Initialize cached itemsets */
	static void initializeCachedItemsets(final TransactionDatabase transactions, final Multiset<Integer> singletons) {
		final long noTransactions = transactions.size();
		transactions.getTransactionList().parallelStream()
				.forEach(t -> t.initializeCachedItemsets(singletons, noTransactions));
	}

	/** EM-step for hard EM */
	static Map<Itemset, Double> hardEMStep(final List<Transaction> transactions,
			final InferenceAlgorithm inferenceAlgorithm) {
		final double noTransactions = transactions.size();

		// E-step
		final Map<Itemset, Long> coveringWithCounts = transactions.parallelStream().map(t -> {
			final HashSet<Itemset> covering = inferenceAlgorithm.infer(t);
			t.setCachedCovering(covering);
			return covering;
		}).flatMap(HashSet::stream).collect(groupingBy(identity(), counting()));

		// M-step
		final Map<Itemset, Double> newItemsets = coveringWithCounts.entrySet().parallelStream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() / noTransactions));

		// Update cached itemsets
		transactions.parallelStream().forEach(t -> t.updateCachedItemsets(newItemsets));

		return newItemsets;
	}

	/** Get average cost of last EM-step */
	static void calculateAndSetAverageCost(final TransactionDatabase transactions) {
		final double noTransactions = transactions.size();
		final double averageCost = transactions.getTransactionList().parallelStream()
				.mapToDouble(Transaction::getCachedCost).sum() / noTransactions;
		transactions.setAverageCost(averageCost);
	}

	/** EM-step for structural EM */
	static Tuple2<Double, Double> structuralEMStep(final TransactionDatabase transactions,
			final InferenceAlgorithm inferenceAlgorithm, final Itemset candidate) {
		final double noTransactions = transactions.size();

		// E-step (adding candidate to transactions that support it)
		final Map<Itemset, Long> coveringWithCounts = transactions.getTransactionList().parallelStream().map(t -> {
			if (t.contains(candidate)) {
				t.addItemsetCache(candidate, 1.0);
				final HashSet<Itemset> covering = inferenceAlgorithm.infer(t);
				t.setTempCachedCovering(covering);
				return covering;
			}
			return t.getCachedCovering();
		}).flatMap(HashSet::stream).collect(groupingBy(identity(), counting()));

		// M-step
		final Map<Itemset, Double> newItemsets = coveringWithCounts.entrySet().parallelStream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() / noTransactions));

		// Get average cost (removing candidate from supported transactions)
		final double averageCost = transactions.getTransactionList().parallelStream().mapToDouble(t -> {
			double cost;
			if (t.contains(candidate))
				cost = t.getTempCachedCost(newItemsets);
			else
				cost = t.getCachedCost(newItemsets);
			t.removeItemsetCache(candidate);
			return cost;
		}).sum() / noTransactions;

		// Get candidate prob
		Double prob = newItemsets.get(candidate);
		if (prob == null)
			prob = 0.;

		return new Tuple2<Double, Double>(averageCost, prob);
	}

	/** Add accepted candidate itemset to cache */
	static Map<Itemset, Double> addAcceptedCandidateCache(final TransactionDatabase transactions,
			final Itemset candidate, final double prob) {
		final double noTransactions = transactions.size();

		// Cached E-step (adding candidate to transactions that support it)
		final Map<Itemset, Long> coveringWithCounts = transactions.getTransactionList().parallelStream().map(t -> {
			if (t.contains(candidate)) {
				t.addItemsetCache(candidate, prob);
				final HashSet<Itemset> covering = t.getTempCachedCovering();
				t.setCachedCovering(covering);
				return covering;
			}
			return t.getCachedCovering();
		}).flatMap(HashSet::stream).collect(groupingBy(identity(), counting()));

		// M-step
		final Map<Itemset, Double> newItemsets = coveringWithCounts.entrySet().parallelStream()
				.collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue() / noTransactions));

		// Update cached itemsets
		transactions.getTransactionList().parallelStream().forEach(t -> t.updateCachedItemsets(newItemsets));

		return newItemsets;
	}

	private EMStep() {
	}

}
