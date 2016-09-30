package it.keypartner.bpms;

import java.io.Serializable;

public class JumpData implements Serializable{
	@Override
	public String toString() {
		return deploymentId +" - " + processId + " - " + instanceId;
	}
	/**
	 * 
	 */
	private static final long serialVersionUID = 2884226643637195227L;
	private String deploymentId;
	private String processId;
	private Long instanceId;
	public String getDeploymentId() {
		return deploymentId;
	}
	public void setDeploymentId(String deploymentId) {
		this.deploymentId = deploymentId;
	}
	public String getProcessId() {
		return processId;
	}
	public void setProcessId(String processId) {
		this.processId = processId;
	}
	public Long getInstanceId() {
		return instanceId;
	}
	public void setInstanceId(Long instanceId) {
		this.instanceId = instanceId;
	}

}
