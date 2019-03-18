/**
 * This file is part of the Joana IFC project. It is developed at the
 * Programming Paradigms Group of the Karlsruhe Institute of Technology.
 *
 * For further details on licensing please read the information at
 * http://joana.ipd.kit.edu or contact the authors.
 */
package edu.kit.joana.wala.core.graphs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.jgrapht.DirectedGraph;
import org.jgrapht.EdgeFactory;
import org.jgrapht.alg.KosarajuStrongConnectivityInspector;
import org.jgrapht.graph.EdgeReversedGraph;

import edu.kit.joana.util.graph.AbstractJoanaGraph;
import edu.kit.joana.util.graph.IntegerIdentifiable;
import edu.kit.joana.util.graph.KnowsVertices;
import edu.kit.joana.util.graph.LeastCommonAncestor;

/**
 * TODO: @author Add your name here.
 */
public class SinkpathPostDominators {
	public static class ISinkdomEdge<V> implements KnowsVertices<V> {
		private V source;
		private V target;
		public ISinkdomEdge(V source, V target) {
			this.source = source;
			this.target = target;
		}
		
		@Override
		public String toString() {
			return "ISINKDOM";
		}
		
		@Override
		public V getSource() {
			return source;
		}
		
		@Override
		public V getTarget() {
			return target;
		}
	};

	public static class Node<V extends IntegerIdentifiable> implements LeastCommonAncestor.PseudoTreeNode<Node<V>>, IntegerIdentifiable {
		private final V v;
		
		private boolean processed;
		private boolean isSinkNode;
		private boolean isRelevant;
		
		private Node<V> next;
		private Node<V> representant;
		
		private List<Node<V>> successors;
		
		public Node(V v) {
			this.v = v;
			this.processed = false;
			this.isSinkNode = false;
			this.isRelevant = false;
			
			this.representant = this;
		}
		
		@Override
		public Node<V> getNext() {
			return next;
		}
		

		public V getV() {
			return v;
		}
		@Override
		public String toString() {
			return v.toString();
		}
		
		public Node<V> getRepresentant() {
			return representant;
		}
		@Override
		public int getId() {
			return v.getId();
		}
	}
	
	private static <V extends IntegerIdentifiable> void processed(DirectedGraph<Node<V>, ISinkdomEdge<Node<V>>> result, Node<V> x) {
		final GraphWalker<Node<V>, ISinkdomEdge<Node<V>>> rdfs = new GraphWalker<Node<V>, ISinkdomEdge<Node<V>>>(new EdgeReversedGraph<>(result)) {
			@Override
			public void discover(Node<V> node) {}

			@Override
			public void finish(Node<V> node) {
				node.processed = true;
			}
			
			@Override
			public boolean traverse(Node<V> node, ISinkdomEdge<Node<V>> edge) {
				return !node.processed;
			}
		};
		rdfs.traverseDFS(x);
	}
	
	private static <V extends IntegerIdentifiable> void newEdge(AbstractJoanaGraph<Node<V>, ISinkdomEdge<Node<V>>> result, Node<V> x, Node<V> z) {
		if (x.next != null) result.removeOutgoingEdgesOf(x);
		if (z != null) result.addEdgeUnsafe(x, z, new ISinkdomEdge<SinkpathPostDominators.Node<V>>(x, z));
		x.next = z;
	}
	
	@SuppressWarnings("serial")
	public static <V extends IntegerIdentifiable, E extends KnowsVertices<V>> DirectedGraph<Node<V>, ISinkdomEdge<Node<V>>> compute(DirectedGraph<V, E> graph) {
		
		final Map<V, Node<V>> vToNode = new HashMap<>();
		final AbstractJoanaGraph<Node<V>, ISinkdomEdge<Node<V>>> result; {
			final ISinkdomEdge<Node<V>> dummy = new ISinkdomEdge<SinkpathPostDominators.Node<V>>(null, null);
			@SuppressWarnings("unchecked")
			final Class<ISinkdomEdge<Node<V>>> clazz = (Class<ISinkdomEdge<Node<V>>>) dummy.getClass();
			result = new AbstractJoanaGraph<Node<V>, ISinkdomEdge<Node<V>>>(
				new EdgeFactory<Node<V>, ISinkdomEdge<Node<V>>>() {
					@Override
					public ISinkdomEdge<Node<V>> createEdge(Node<V> sourceVertex, Node<V> targetVertex) {
						return new ISinkdomEdge<>(sourceVertex, targetVertex); 
					}
				},
				() -> new HashMap<>(graph.vertexSet().size()),
				clazz
			) {	};
		}
		
		for (V v : graph.vertexSet()) {
			final Node<V> n = new Node<V>(v);
			vToNode.put(v, n);
			result.addVertexUnsafe(n);
		}
		
		final KosarajuStrongConnectivityInspector<V, E> sccInspector = new KosarajuStrongConnectivityInspector<V, E>(graph);
		final List<Set<V>> sccs = sccInspector.stronglyConnectedSets();
		
		//final HashSet<Node<V>> relevant = new HashSet<>();
		for (Set<V> scc : sccs) {
			final boolean isSink = ! scc.stream().anyMatch(
				v -> graph.outgoingEdgesOf(v).stream().anyMatch(
					e -> !scc.contains(graph.getEdgeTarget(e))
				)
			);
			if (isSink) {
				Node<V> last = null;
				Node<V> first = null;
				for (V v : scc) {
					final Node<V> n = vToNode.get(v);
					if (last != null) {
						newEdge(result, last, n);
						processed(result, last);
						last = n;
					} else {
						last = n;
						first = n;
					}
					assert first != null;
					n.representant = first;
					n.isSinkNode = true;
				}
				if (last != first) newEdge(result, last, first); // TODO: pdf fixen
				processed(result, last);
			}
		}
		
		final LinkedList<Node<V>> workqueue = new LinkedList<>();

		for (Entry<V, Node<V>> entry : vToNode.entrySet()) {
			final V v = entry.getKey();
			final Node<V> x = entry.getValue();
			if (!x.isSinkNode) {
				final Set<E> successorEs = graph.outgoingEdgesOf(v);
				switch (successorEs.size()) {
					case 0: break;
					case 1: {
						final Node<V> z = vToNode.get(successorEs.iterator().next().getTarget());
						if (z != x) {
							newEdge(result, x, z.representant);
							if (z.processed) processed(result, x); // TODO: pdf fixen
						}
						break;
					}
					default: {
						x.isRelevant = true;
						final ArrayList<Node<V>> successors = new ArrayList<>(successorEs.size());
						for (E e : successorEs) {
							successors.add(vToNode.get(e.getTarget()));
						}
						x.successors = successors;
						workqueue.add(x);
					}
				}
			}
		}
		
		
		{
			while (!workqueue.isEmpty()) {
				final Node<V> x = workqueue.removeFirst();
				assert x.next == null && !x.processed;
				final List<Node<V>> successors = x.successors;
				final List<Node<V>> ys = successors.stream().filter(y -> y.processed).collect(Collectors.toList());
				final Node<V> z;
				if (ys.isEmpty()) {
					z = null;
				} else {
					final Node<V> a = LeastCommonAncestor.lca(ys);
					z = a == null ? ys.get(0) : a;
				}
				if (z != null) {
					newEdge(result, x, z.representant);
					processed(result, x);
				} else {
					workqueue.addLast(x);
				}
			}
		}
		{
			final Set<Node<V>> workset = new HashSet<>();
			for (Node<V> n : vToNode.values()) {
				if (n.next != null && n.isRelevant) workset.add(n);
			}
			
			while (!workset.isEmpty()) {
				final Node<V> x; {
					final Iterator<Node<V>> it = workset.iterator();
					x = it.next();
					it.remove();
				}
				final List<Node<V>> successors = x.successors;
				final Node<V> a = LeastCommonAncestor.lca(successors);
				final Node<V> z = a == null ? null : a.representant;
				assert x.next != null || z == null;
				if (z != x.next) {
					final GraphWalker<Node<V>, ISinkdomEdge<Node<V>>> rdfs = new GraphWalker<Node<V>, ISinkdomEdge<Node<V>>>(new EdgeReversedGraph<>(result)) {
						@Override
						public void discover(Node<V> node) {}

						@Override
						public void finish(Node<V> node) {
							for (E e : graph.incomingEdgesOf(node.v) ) {
								final V vn = e.getSource();
								final Node<V> n = vToNode.get(vn);
								if (n.isRelevant) {
									workset.add(n);
								}
							}
						}
						
						@Override
						public boolean traverse(Node<V> node, ISinkdomEdge<Node<V>> edge) {
							return true;
						}
					};
					rdfs.traverseDFS(x);
					newEdge(result, x, z);
				}
			}
		}
		
		return result;
	}
}
