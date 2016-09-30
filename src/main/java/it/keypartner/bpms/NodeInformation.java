package it.keypartner.bpms;

import java.io.Serializable;

public class NodeInformation implements Serializable {

	@Override
	public String toString() {
		
		return taskType +" - "+name +" - "+uniqueId + " - "+nodeId +" - "+workItemId;
	}
	private static final long serialVersionUID = 4384427856830159874L;

	private String name;
	private String uniqueId;
	private Long nodeId;
	private String taskType;
	private Long workItemId;
	
	public String getTaskType() {
		return taskType;
	}

	public void setTaskType(String taskType) {
		this.taskType = taskType;
	}

	public Long getWorkItemId() {
		return workItemId;
	}

	public void setWorkItemId(Long workItemId) {
		this.workItemId = workItemId;
	}

	public NodeInformation() {
		
	}
	
	public NodeInformation(String taskType, String name, String uniqueId, Long nodeId,Long workItemId ) {
		this.taskType=taskType;
		this.name = name;
		this.uniqueId = uniqueId;
		this.nodeId = nodeId;
		this.workItemId=workItemId;
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUniqueId() {
		return uniqueId;
	}
	public void setUniqueId(String uniqueId) {
		this.uniqueId = uniqueId;
	}
	public Long getNodeId() {
		return nodeId;
	}
	public void setNodeId(Long nodeId) {
		this.nodeId = nodeId;
	}
}
