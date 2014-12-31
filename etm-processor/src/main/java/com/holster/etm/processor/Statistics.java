package com.holster.etm.processor;

import java.util.concurrent.atomic.AtomicLong;

public class Statistics {

	public static AtomicLong preprocessingTime = new AtomicLong();
	public static AtomicLong enhancingTime = new AtomicLong();
	public static AtomicLong indexingTime = new AtomicLong();
	public static AtomicLong persistingTime = new AtomicLong();
}
