package it.keypartner.bpms;


import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class NodeMapping implements Serializable {

	private static final long serialVersionUID = 1682833896996142469L;
	
	private String sourceNodeId;
	private String targetNodeId;
	
	public NodeMapping() {
		
	}
	
	public NodeMapping(String sourceNodeId, String targetNodeId) {
		this.sourceNodeId = sourceNodeId;
		this.targetNodeId = targetNodeId;
	}
	public String getSourceNodeId() {
		return sourceNodeId;
	}
	public void setSourceNodeId(String sourceNodeId) {
		this.sourceNodeId = sourceNodeId;
	}
	public String getTargetNodeId() {
		return targetNodeId;
	}
	public void setTargetNodeId(String targetNodeId) {
		this.targetNodeId = targetNodeId;
	}

	@Override
	public String toString() {
		
		
		return "From: "+sourceNodeId +" To: "+targetNodeId;
	}
	
	

}
