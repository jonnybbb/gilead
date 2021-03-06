/**
 * 
 */
package net.sf.gilead.core.beanlib.merge;

import java.io.Serializable;
import java.util.Map;

import net.sf.beanlib.hibernate3.Hibernate3MapReplicator;
import net.sf.beanlib.spi.BeanTransformerSpi;
import net.sf.beanlib.spi.replicator.MapReplicatorSpi;
import net.sf.gilead.core.IPersistenceUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulation of the collection replicator
 * 
 * @author bruno.marchesson
 */
public class MergeMapReplicator extends Hibernate3MapReplicator {

	// ----
	// Factory
	// ----
	public static final Factory factory = new Factory();

	/**
	 * Factory for {@link MergeClassBeanReplicator}
	 * 
	 * @author bruno.marchesson
	 */
	private static class Factory implements MapReplicatorSpi.Factory {
		private Factory() {}

		@Override
		public Hibernate3MapReplicator newMapReplicatable(BeanTransformerSpi beanTransformer) {
			return new MergeMapReplicator(beanTransformer);
		}
	}

	public static Hibernate3MapReplicator newMapReplicatable(BeanTransformerSpi beanTransformer) {
		return factory.newMapReplicatable(beanTransformer);
	}

	// ----
	// Attributes
	// ----
	/**
	 * Logger channel
	 */
	private static Logger _log = LoggerFactory.getLogger(MergeMapReplicator.class);

	/**
	 * The associated persistence util
	 */
	private IPersistenceUtil _persistenceUtil;

	// ----
	// Properties
	// ----
	/**
	 * @return the _persistenceUtil
	 */
	public IPersistenceUtil getPersistenceUtil() {
		return _persistenceUtil;
	}

	/**
	 * @param util the _persistenceUtil to set
	 */
	public void setPersistenceUtil(IPersistenceUtil util) {
		_persistenceUtil = util;
	}

	// ----
	// Constructor
	// ----
	/**
	 * Constructor
	 * 
	 * @param beanTransformer
	 */
	protected MergeMapReplicator(BeanTransformerSpi beanTransformer) {
		super(beanTransformer);
	}

	// ----
	// Overrides
	// ----
	@Override
	protected Object replicate(Object from) {
		// Reset bean local
		//
		BeanlibThreadLocal.setProxyInformations(null);

		return super.replicate(from);
	}

	/**
	 * Map replication override
	 */
	@SuppressWarnings("unchecked")
	@Override
	public <K, V, T> T replicateMap(Map<K, V> from, Class<T> toClass) {
		if (_log.isDebugEnabled()) {
			_log.debug("Merge map from " + from + " to class " + toClass);
		}

		// Get and reset persistent collection class if any
		//
		Map<String, Serializable> proxyInformations = BeanlibThreadLocal.getProxyInformations();
		BeanlibThreadLocal.setProxyInformations(null);

		// AS Object BlazeDS map handling
		//
		if (from.getClass().getName().endsWith(".ASObject")) {
			// Turn it into regular hash map
			//
			String type = null;
			try {
				type = (String) from.getClass().getMethod("getType", (Class<?>[]) null).invoke(from, (Object[]) null);
			} catch (Exception e) {
				type = e.getMessage();
			}
			_log.warn("Invalid map : " + from.toString() + " for type " + type);
			toClass = (Class<T>) java.util.HashMap.class;
		}

		// Clone map
		//
		T map = super.replicateMap(from, toClass);

		// Turn into persistent map if needed
		//
		if (proxyInformations != null) {
			Object parent = BeanlibThreadLocal.getToBeanStack().peek();
			return (T) _persistenceUtil.createPersistentMap(parent, proxyInformations, (Map) map);
		} else {
			return map;
		}
	}
}
