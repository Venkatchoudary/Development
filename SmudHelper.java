package com.spacetimeinsight.app.sample;

import java.util.SortedSet;
import java.util.TreeSet;

import ch.qos.logback.classic.Logger;

import com.google.common.collect.Sets;
import com.spacetimeinsight.si.dataobject.IDataObject;


public final class SmudHelper {
	
	// primary aka main
	static final SortedSet<Integer> primary = new TreeSet<>(Sets.newHashSet(6,7,8,9,10,11));
	// secondary aka local
	static final SortedSet<Integer> secondary = new TreeSet<>(Sets.newHashSet(64,65,66,67,68,69));
	static final SortedSet<Integer> overhead = new TreeSet<>(Sets.newHashSet(6, 7, 8, 64, 65, 66));
	static final SortedSet<Integer> underground = new TreeSet<>(Sets.newHashSet(9, 10, 11, 67 ,68 ,69));

	private SmudHelper() {
		
	}
	
	static public String getConductorLinePattern(Long conductorType) {
		//if (overhead.contains(conductorType)) return "Solid";
		//if (underground.contains(conductorType)) return "Dash";
		// unknown
		return "Dot";
	}
	
	// basic example for testing
	static public String getStringExample(String string) {
		return "string";
	}
}

	/*
	static public String getConductorLinePattern(IDataObject dataObject) {
//		Random r = new Random();
//		return r.nextInt(2) == 0 ? "Dash" : "Solid";
//		//return EEsriLinePattern.DASH;
//		return "Dash";
		return "DashDot";
	}
	*/