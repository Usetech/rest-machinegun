package com.usetech.rest_machinegun.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MachinegunConfig {
	private List<MachinegunTask> tasks;
	private Integer vertices;

	public List<MachinegunTask> getTasks() {
		return tasks;
	}

	public void setTasks(List<MachinegunTask> tasks) {
		this.tasks = tasks;
		for (int i = 0; i < this.tasks.size(); i++)
			this.tasks.get(i).setIdx(i);
	}

	public Integer getVertices() {
		return vertices;
	}

	public void setVertices(Integer vertices) {
		this.vertices = vertices;
	}
}
