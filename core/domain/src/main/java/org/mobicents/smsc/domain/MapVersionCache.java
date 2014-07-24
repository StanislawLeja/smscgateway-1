/**
 * 
 */
package org.mobicents.smsc.domain;

import javolution.util.FastMap;

import org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion;

/**
 * @author Amit Bhayani
 * 
 */
public class MapVersionCache implements MapVersionCacheMBean {

	private FastMap<String, MAPApplicationContextVersion> cache = new FastMap<String, MAPApplicationContextVersion>()
			.shared();

	private final String name;

	private static MapVersionCache instance;

	/**
	 * 
	 */
	private MapVersionCache(String name) {
		this.name = name;
	}

	public static MapVersionCache getInstance(String name) {
		if (instance == null) {
			instance = new MapVersionCache(name);
		}
		return instance;
	}

	public static MapVersionCache getInstance() {
		return instance;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.MapVersionCacheMBean#getMAPApplicationContextVersion
	 * (java.lang.String)
	 */
	@Override
	public MAPApplicationContextVersion getMAPApplicationContextVersion(String globalTitleDigits) {
		return this.cache.get(globalTitleDigits);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.mobicents.smsc.smpp.MapVersionCacheMBean#setMAPApplicationContextVersion
	 * (java.lang.String,
	 * org.mobicents.protocols.ss7.map.api.MAPApplicationContextVersion)
	 */
	@Override
	public void setMAPApplicationContextVersion(String globalTitleDigits, MAPApplicationContextVersion version) {
		this.cache.put(globalTitleDigits, version);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.mobicents.smsc.smpp.MapVersionCacheMBean#
	 * getMAPApplicationContextVersionCache()
	 */
	@Override
	public FastMap<String, MAPApplicationContextVersion> getMAPApplicationContextVersionCache() {
		return this.cache;
	}

	@Override
	public void forceClear() {
		this.cache.clear();
	}

}
