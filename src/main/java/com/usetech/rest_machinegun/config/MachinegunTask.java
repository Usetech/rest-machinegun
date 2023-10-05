package com.usetech.rest_machinegun.config;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Collections;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MachinegunTask {
	private int idx;
	private String name;
	@JsonAlias("interval-ms")
	private String intervalMs;
	@JsonAlias("cron-expr")
	private String cronExpr;
	private Map<String, String> env;
	private MachinegunTaskRequest request;
	private MachinegunTaskAuthorization authorization;
	private String repeats;
	private String id;

	public int getIdx() {
		return idx;
	}

	public void setIdx(int idx) {
		this.idx = idx;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getIntervalMs() {
		return intervalMs;
	}

	public void setIntervalMs(String intervalMs) {
		this.intervalMs = intervalMs;
	}

	public String getCronExpr() {
		return cronExpr;
	}

	public void setCronExpr(String cronExpr) {
		this.cronExpr = cronExpr;
	}

	public Map<String, String> getEnv() {
		return env != null ? env : Collections.emptyMap();
	}

	public void setEnv(Map<String, String> env) {
		this.env = env;
	}

	public MachinegunTaskRequest getRequest() {
		return request;
	}

	public void setRequest(MachinegunTaskRequest request) {
		this.request = request;
	}

	public MachinegunTaskAuthorization getAuthorization() {
		return authorization;
	}

	public void setAuthorization(MachinegunTaskAuthorization authorization) {
		this.authorization = authorization;
	}

	public String getRepeats() {
		return repeats;
	}

	public void setRepeats(String repeats) {
		this.repeats = repeats;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
}
