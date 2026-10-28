package com.group22.reportnow.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ProjectEvent extends TelemetryEvent {
    @JsonProperty("action")
	private final String action;
	
	@JsonProperty("ProjectName")
	private final String projectName;

	public ProjectEvent(long timestampsMs, String action, String projectName){
		super(timestampsMs);

		this.action = action;
		this.projectName = projectName;
	}

	@Override
	public String getType(){
		return "project";
	}
	
	public String getAction(){
		return action;
	}

	public String getProjectName(){
		return projectName;
	}

}
