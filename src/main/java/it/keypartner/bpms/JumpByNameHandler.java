package it.keypartner.bpms;

import java.util.HashMap;
import java.util.Map;

import org.kie.api.runtime.KieRuntime;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.WorkItem;
import org.kie.api.runtime.process.WorkItemHandler;
import org.kie.api.runtime.process.WorkItemManager;
import org.kie.internal.persistence.jpa.JPAKnowledgeService;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.internal.runtime.manager.RuntimeManagerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.drools.core.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.core.command.impl.KnowledgeCommandContext;
import org.jbpm.workflow.instance.WorkflowProcessInstanceUpgrader;


public class JumpByNameHandler implements WorkItemHandler {

	private static final Logger logger=LoggerFactory.getLogger(JumpByNameHandler.class);
	
	public void executeWorkItem(WorkItem workItem, WorkItemManager manager) {
		Map<String, Object> results = new HashMap<String, Object>();
		InternalRuntimeManager runtime = (InternalRuntimeManager) RuntimeManagerRegistry.get().getManager((String)workItem.getParameter("deploymentID"));	
		KieSession kSession = JPAKnowledgeService.newStatefulKnowledgeSession(runtime.getEnvironment().getKieBase(), null, runtime.getEnvironment().getEnvironment());
		KieRuntime kRuntime;
		if (kSession instanceof CommandBasedStatefulKnowledgeSession) {
			kRuntime=((KnowledgeCommandContext)((CommandBasedStatefulKnowledgeSession) kSession).getCommandService().getContext()).getKieSession();
		}
		else
			kRuntime=kSession;
	
		
		//Map<String,String> nodeNamesMapping= new HashMap<String,String>();
		
		//nodeNamesMapping.put((String)workItem.getParameter("fromTask"),(String) workItem.getParameter("toTask"));
		
		//WorkflowProcessInstanceUpgrader.upgradeProcessInstanceByNodeNames(kRuntime,	Integer.toUnsignedLong((Integer)workItem.getParameter("instanceID")), (String)workItem.getParameter("processID"), nodeNamesMapping);

		Map<String,Long> nodesMapping=new HashMap<String,Long>();
		
		
		WorkflowProcessInstanceUpgrader.upgradeProcessInstance(kRuntime, Integer.toUnsignedLong((Integer)workItem.getParameter("instanceID")), (String)workItem.getParameter("processID"), nodesMapping);
		
		
		
		
		manager.completeWorkItem(workItem.getId(), results);

	}

	public void abortWorkItem(WorkItem workItem, WorkItemManager manager) {
		manager.completeWorkItem(workItem.getId(), null);
	}

}
