package it.keypartner.bpms;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JumpHandler implements WorkItemHandler {

	private static final Logger logger=LoggerFactory.getLogger(JumpHandler.class);


	//@Override
	public void executeWorkItem(WorkItem workItem, WorkItemManager workItemManager) {
		JumpData jumpData=(JumpData)workItem.getParameter("JumpData");
		Map<String, Object> results = new HashMap<String, Object>();
		Map<String, String>[] mappings=(Map<String, String>[])workItem.getParameter("NodeMappings");
		List<NodeInformation> availableNodes=(List<NodeInformation>)workItem.getParameter("AvailableNodes");
		List<NodeInformation> activeNodes=(List<NodeInformation>)workItem.getParameter("ActiveNodes");
		Map<NodeInformation,NodeInformation> jumpMappings= new HashMap<NodeInformation, NodeInformation>();
		
	    logger.info("Mappings: "+mappings + " size: "+mappings.length);
		for( Map<String, String> map :mappings){
			String sourceNodeId=map.get("nodeMapping_sourceNodeId");
			String targetNodeId=map.get("nodeMapping_targetNodeId");
			NodeInformation source=JumpManager.retrieveNodeByUniqueId(sourceNodeId, activeNodes);
			NodeInformation target= JumpManager.retrieveNodeByUniqueId(targetNodeId, availableNodes);
			//NodeInformation source=JumpManager.retrieveNodeById(Long.parseLong(sourceNodeId), availableNodes);
			//NodeInformation target= JumpManager.retrieveNodeById(Long.parseLong(targetNodeId), availableNodes);
			if(source!=null && target!= null)
				jumpMappings.put(source, target);
			else
				logger.warn("NULL MAPPING: "+ sourceNodeId +" - " + targetNodeId);
				
		}
		String result=JumpManager.jump(jumpData, jumpMappings);
		results.put("Outcome", result);
		workItemManager.completeWorkItem(workItem.getId(), results);
	}
	
	
	//@Override
	public void abortWorkItem(WorkItem workItem, WorkItemManager workItemManager) {
		workItemManager.completeWorkItem(workItem.getId(), null);
	}

}
