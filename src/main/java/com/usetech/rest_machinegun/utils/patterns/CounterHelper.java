package com.usetech.rest_machinegun.utils.patterns;

import io.vertx.core.Vertx;
import io.vertx.core.shareddata.LocalMap;

public class CounterHelper {
	private final Vertx vertx;

	public static final String COUNTERS_NAME = "counters";
	public static final String GLOBAL_COUNTER_CODE = "____global";

	public CounterHelper(Vertx vertx) {
		this.vertx = vertx;
	}

	private LocalMap<String, Long> getCounters() {
		return vertx.sharedData().getLocalMap(COUNTERS_NAME);
	}

	private Long getCounter(String name) {
		return getCounters().computeIfAbsent(name, s -> 0L);
	}

	public long getCounterValue(String name) {
		LocalMap<String, Long> counters = vertx.sharedData().getLocalMap(COUNTERS_NAME);
		return counters.computeIfAbsent(name, s -> 0L);
	}
	public long incrementAndGetCounterValue(String name) {
		LocalMap<String, Long> counters = vertx.sharedData().getLocalMap(COUNTERS_NAME);
		return counters.compute(name, (k, v) -> (v == null ? 0 : v) + 1);
	}

	public long getGlobalCounterValue() {
		return getCounterValue(GLOBAL_COUNTER_CODE);
	}
	public long incrementAndGetGlobalCounterValue() {
		return incrementAndGetCounterValue(GLOBAL_COUNTER_CODE);
	}
}
