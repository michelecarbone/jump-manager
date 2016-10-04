package it.keypartner.bpms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;


import org.jbpm.workflow.instance.WorkflowProcessInstanceUpgrader;
import org.drools.core.command.impl.CommandBasedStatefulKnowledgeSession;
import org.drools.core.command.impl.KnowledgeCommandContext;
import org.jbpm.process.audit.JPAAuditLogService;
import org.jbpm.process.audit.NodeInstanceLog;
import org.jbpm.process.audit.ProcessInstanceLog;
import org.jbpm.runtime.manager.impl.jpa.EntityManagerFactoryManager;
import org.jbpm.workflow.core.NodeContainer;
import org.jbpm.workflow.core.impl.NodeImpl;
import org.jbpm.workflow.core.node.CompositeNode;
import org.jbpm.workflow.instance.NodeInstanceContainer;
import org.jbpm.workflow.instance.impl.NodeInstanceImpl;
import org.jbpm.workflow.instance.impl.WorkflowProcessInstanceImpl;
import org.kie.api.definition.process.Node;
import org.kie.api.definition.process.Process;
import org.kie.api.runtime.KieRuntime;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.process.NodeInstance;
import org.kie.api.runtime.process.ProcessInstance;
import org.kie.internal.persistence.jpa.JPAKnowledgeService;
import org.kie.internal.runtime.manager.InternalRuntimeManager;
import org.kie.internal.runtime.manager.RuntimeManagerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JumpManager {
	
	private static final Logger logger=LoggerFactory.getLogger(JumpManager.class);
	
	
	public static String jump(JumpData data,Map<NodeInformation,NodeInformation> nodeMapping){
		StringBuffer outcomeBuffer = new StringBuffer();
		try{
			InternalRuntimeManager manager = (InternalRuntimeManager) RuntimeManagerRegistry.get().getManager(data.getDeploymentId());	
			Process process = manager.getEnvironment().getKieBase().getProcess(data.getProcessId());
	

			String auditPu = manager.getDeploymentDescriptor().getAuditPersistenceUnit();
			
			EntityManagerFactory emf = EntityManagerFactoryManager.get().getOrCreate(auditPu);
			EntityManager em = emf.createEntityManager();
	
			String mainPu=manager.getDeploymentDescriptor().getPersistenceUnit();
			EntityManagerFactory mainEmf = EntityManagerFactoryManager.get().getOrCreate(mainPu);
			EntityManager mainEm = mainEmf.createEntityManager();			
			
			KieSession current = JPAKnowledgeService.newStatefulKnowledgeSession(manager.getEnvironment().getKieBase(), null, manager.getEnvironment().getEnvironment());
			KieRuntime runtime=extractIfNeeded(current);
			WorkflowProcessInstanceImpl processInstance = (WorkflowProcessInstanceImpl) runtime.getProcessInstance(data.getInstanceId());
			if (processInstance == null) {
		            throw new IllegalArgumentException("Could not find process instance " + data.getInstanceId());
		        }
			//processInstance.disconnect();
			
			updateNodeInstances(data.getInstanceId(),processInstance, nodeMapping, (NodeContainer) process,  mainEm);
			//processInstance.reconnect();
			
			mainEm.flush();
			mainEm.clear();
			mainEm.close();
			
			em.flush();
			em.clear();
			em.close();
			
			current.destroy();
			
		}
		catch (Exception e) {
			outcomeBuffer.append("Jump of process instance (" + data.getInstanceId() + ") failed due to " + e.getMessage());
			logger.error("Jump of process instance ({}) failed", data.getInstanceId(), e);
		}
		
		return outcomeBuffer.toString();
	}
	
	
	
	
	public static Boolean validate(JumpData data){
		if (data == null) {
			return false;
		}
		if (isEmpty(data.getDeploymentId())) {
			logger.error("No deployment id set");
			return false;
		}
		
		if (data.getInstanceId() == null) {
			logger.error("No process instance id set"); 
			return false;
		}
		
		if (!RuntimeManagerRegistry.get().isRegistered(data.getDeploymentId())) {
			logger.error("No deployment found for {}", data.getDeploymentId());
			
			return false;
		}
		
		
		
		InternalRuntimeManager manager = (InternalRuntimeManager) RuntimeManagerRegistry.get().getManager(data.getDeploymentId());
		if (manager.getEnvironment().getKieBase().getProcess(data.getProcessId()) == null) {
			logger.error("No process found for {} in deployment {}", data.getProcessId(), data.getDeploymentId());
			
			return false;
		}
		
		String auditPu = manager.getDeploymentDescriptor().getAuditPersistenceUnit();
		
		EntityManagerFactory emf = EntityManagerFactoryManager.get().getOrCreate(auditPu);

		JPAAuditLogService auditService = new JPAAuditLogService(emf);
		ProcessInstanceLog log = auditService.findProcessInstance(data.getInstanceId());
		if (log == null || log.getStatus() != ProcessInstance.STATE_ACTIVE) {
			logger.error("No process instance found or it is not active (id {} in status {}",
					data.getInstanceId(), (log == null?"-1":log.getStatus()));
			
			return false;
		}
		auditService.dispose();

		return true;
	}
	
	
	
	
	
	
	
    
    
  
    
    private static KieRuntime extractIfNeeded(KieSession ksession) {
    	if (ksession instanceof CommandBasedStatefulKnowledgeSession) {
    		return ((KnowledgeCommandContext)((CommandBasedStatefulKnowledgeSession) ksession).getCommandService().getContext()).getKieSession();
    	}
    	
    	return ksession;
    }
    
    private static void collectAvailableNodeInformation(NodeContainer container, List<NodeInformation> nodes) {
    	for (Node node : container.getNodes()) {
    		logger.info("collectNodeInformation, node: "+node.getName());
			if (node instanceof CompositeNode) {
				collectAvailableNodeInformation(((CompositeNode) node), nodes);
			} else {
				if (node.getName() == null || node.getName().trim().isEmpty()) {
					continue;
				}
				
				nodes.add(new NodeInformation( node.getClass().getSimpleName()  , node.getName(), (String) node.getMetaData().get("UniqueId"), node.getId(),null));
			}
		}
    }
    
    private static void collectActiveNodeInformation(List<NodeInstanceLog> logs, List<NodeInformation> activeNodes) {
    	Map<String, NodeInstanceLog> nodeInstances = new HashMap<String, NodeInstanceLog>();
    	for (NodeInstanceLog nodeInstance: logs) {
			if (nodeInstance.getType() == NodeInstanceLog.TYPE_ENTER) {
				nodeInstances.put(nodeInstance.getNodeInstanceId(), nodeInstance);
			} else {
				nodeInstances.remove(nodeInstance.getNodeInstanceId());
			}
		}
		if (!nodeInstances.isEmpty()) {
			for (NodeInstanceLog node : nodeInstances.values()) {
				activeNodes.add(new NodeInformation(node.getNodeType(), node.getNodeName(),
						node.getNodeId() ,Long.parseLong(node.getNodeInstanceId()), node.getWorkItemId()));
				//note: nodeinstancelogs has string for nodeid; better use other search
			}
		}
    }
	
	private static boolean isEmpty(String value) {
		if (value == null || value.isEmpty()) {
			return true;
		}
		
		return false;
	}
	public static Map<String, List<NodeInformation>> collectNodeInfo(JumpData data) {
		
		logger.info("CollectNodeInfo, JumpData: "+data);
		Map<String, List<NodeInformation>> result = new HashMap<String, List<NodeInformation>>();
		List<NodeInformation> activeInstances = new ArrayList<NodeInformation>();
		List<NodeInformation> newProcessNodes = new ArrayList<NodeInformation>();
		
		result.put("active", activeInstances);
		result.put("available", newProcessNodes);		
		
		InternalRuntimeManager manager = (InternalRuntimeManager) RuntimeManagerRegistry.get().getManager(data.getDeploymentId());		
		if(manager == null){
			logger.error("RuntimeManager null");
			return null;
		}
		Process process = manager.getEnvironment().getKieBase().getProcess(data.getProcessId());
		if(process == null){
			logger.error("Process null");
			return null;
		}
		
		collectAvailableNodeInformation((NodeContainer) process, newProcessNodes);
		
		String auditPu = manager.getDeploymentDescriptor().getAuditPersistenceUnit();
		
		EntityManagerFactory emf = EntityManagerFactoryManager.get().getOrCreate(auditPu);
		
		JPAAuditLogService auditService = new JPAAuditLogService(emf);
		
		List<NodeInstanceLog> logs = auditService.findNodeInstances(data.getInstanceId());
		collectActiveNodeInformation(logs, activeInstances);
		
		auditService.dispose();
		return result;
	}
	
	public static NodeInformation retrieveNodeByUniqueId(String uniqueId, List<NodeInformation> nodes){
		
		for(NodeInformation node:nodes){
			if(node.getUniqueId().equals(uniqueId)){
				return node;
			}
		}
		
		return null;
		
	}
	
	public static NodeInformation retrieveNodeById(Long id, List<NodeInformation> nodes){
		
		for(NodeInformation node:nodes){
			if(node.getNodeId().equals(id)){
				return node;
			}
		}
		
		return null;
		
	}




	private static void updateNodeInstances(Long processInstanceId,NodeInstanceContainer nodeInstanceContainer, 
			Map<NodeInformation, NodeInformation> nodeMapping, NodeContainer nodeContainer, EntityManager em) {
		logger.info("NODE INSTANCES");
		if (nodeMapping == null || nodeMapping.isEmpty()) {
			return;
		}
		for (Entry<NodeInformation, NodeInformation> entry:nodeMapping.entrySet()){
			NodeInformation from=entry.getKey();
			NodeInformation to=entry.getValue();
			logger.info("Mapping: "+entry.getKey().getName()+" " +entry.getValue().getName());
			//change nodeid
			
			//find node
			Collection<NodeInstance> c=nodeInstanceContainer.getNodeInstances();
			for(NodeInstance nInstance:c){
				logger.info("Node Instance: "+nInstance.getId() + " - " + nInstance.getNodeId() +  " - " + nInstance.getNodeName());
			}
			
			
			
			NodeInstance currentNode=nodeInstanceContainer.getNodeInstance(from.getNodeId());
			if(currentNode!=null)
			{
				((NodeInstanceImpl)currentNode).cancel();
				((NodeInstanceImpl)currentNode).setNodeId(to.getNodeId());
				((NodeInstanceImpl)currentNode).retrigger(true);
			}
			else
				logger.warn("CurrentNode Null");
			
			 Map<String, Integer> iterLevels = ((WorkflowProcessInstanceImpl) currentNode.getProcessInstance()).getIterationLevels();
	            String uniqueId = (String) ((NodeImpl) currentNode.getNode()).getMetaData("UniqueId");
	            iterLevels.remove(uniqueId);
			
			
			//update data
			int updates=0;
			//BAMTASKSUMMARY
			Query bamtaskqry=em.createQuery("update BAMTaskSummaryImpl set taskName=:taskName where processInstanceId=:processInstanceId");
			bamtaskqry.setParameter("taskName", to.getName());
			bamtaskqry.setParameter("processInstanceId", processInstanceId);
			updates=bamtaskqry.executeUpdate();
			logger.info("Update BamTaskSummay: "+ updates);
			//AUDITTASKIMPL
			Query audittask= em.createQuery("update AuditTaskImpl set name=:taskName where processInstanceId=:processInstanceId");
			audittask.setParameter("taskName", to.getName());
			audittask.setParameter("processInstanceId", processInstanceId);
			updates=audittask.executeUpdate();
			logger.info("Update AuditTaskImpl: "+ updates); 
			//TASK
			Query task= em.createQuery("update TaskImpl set name=:taskName, formName=:taskName where processInstanceId=:processInstanceId and name=:oldName");
			task.setParameter("taskName", to.getName());
			task.setParameter("processInstanceId", processInstanceId);
			task.setParameter("oldName", from.getName());
			updates=task.executeUpdate();
			logger.info("Update Task: "+ updates); 	
			//NODEINSTANCELOG
			Query nodelog= em.createQuery("update NodeInstanceLog set nodeName=:taskName,nodeId=:nodeId, nodeType=:nodeType  where processInstanceId=:processInstanceId and nodeid=:oldNodeId");
			nodelog.setParameter("taskName", to.getName());
			nodelog.setParameter("nodeId", to.getUniqueId());        	
			nodelog.setParameter("processInstanceId", processInstanceId);
			nodelog.setParameter("oldNodeId", from.getUniqueId());
			nodelog.setParameter("nodeType", to.getTaskType());
			updates=nodelog.executeUpdate();
			logger.info("Update NodeInstanceLog: "+ updates);
			
			//workitem content
			Query wid=em.createQuery("update WorkItemInfo set name=:nodeType where processInstanceId=:processInstanceId and workItemId=:wid ");
			wid.setParameter("processInstanceId", processInstanceId);
			wid.setParameter("wid", from.getWorkItemId());
			wid.setParameter("nodeType", to.getTaskType());
			updates=wid.executeUpdate();
			logger.info("Update WorkItemInfo: "+ updates);
			
			/*Query wid=em.createQuery("select workItemByteArray from WorkItemInfo where processInstanceId=:processInstanceId");
			wid.setParameter("processInstanceId", processInstanceId);
			List results=wid.getResultList();
			for(Object result:results){
				byte[] ba=(byte[])result;
				System.out.println(ba +" size " + ba.length );
				String res= new String(ba);
				System.out.println(res);
				
			}*/
			
			
			//traverse
			 if (currentNode instanceof NodeInstanceContainer) {
	         	updateNodeInstances(processInstanceId,(NodeInstanceContainer) currentNode, nodeMapping, nodeContainer,em);
	         }
		}
	
		
	}
}
