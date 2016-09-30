package it.keypartner.bpms;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JumpDataHandler implements WorkItemHandler {
	private static final Logger logger=LoggerFactory.getLogger(JumpDataHandler.class);
	
	

	//@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {

		
		JumpData jumpData=(JumpData)workItem.getParameter("JumpData");
		Map<String, Object> results = new HashMap<String, Object>();
		Boolean isValid=JumpManager.validate(jumpData);
		
		Map<String,List<NodeInformation>> nodeInfo=JumpManager.collectNodeInfo(jumpData);
		
		results.put("AvailableNodes",nodeInfo.get("available"));
		results.put("ActiveNodes",nodeInfo.get("active"));
		results.put("Valid", isValid);
		manager.completeWorkItem(workItem.getId(), results);
	}

	//@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager workItemManager) {
		workItemManager.completeWorkItem(workItem.getId(), null);
	}

}
