class SnakerEngineImpl {

	// 开始流程
	public Order startInstanceById(String id, String operator, Map<String, Object> args) {
		..
		Process process = process().getProcessById(id);
		..
		return startProcess(process, operator, args);
	}

	private Order startProcess(Process process, String operator, Map<String, Object> args) {
		Execution execution = execute(process, operator, args, null, null);
		if(process.getModel() != null) {
			StartModel start = process.getModel().getStart();
			..
			start.execute(execution);
		}

		return execution.getOrder();
	}

	/**
	 * 创建流程实例，并返回执行对象
	 * @param process 流程定义
	 * @param operator 操作人
	 * @param args 参数列表
	 * @param parentId 父流程实例id
	 * @param parentNodeName 启动子流程的父流程节点名称
	 * @return Execution
	 */
	private Execution execute(Process process, String operator, Map<String, Object> args, 
			String parentId, String parentNodeName) {
		Order order = order().createOrder(process, operator, args, parentId, parentNodeName);
		..
		Execution current = new Execution(this, process, order, args);
		current.setOperator(operator);
		return current;
	}

	/**
	 * 根据任务主键ID，操作人ID，参数列表完成任务，并且构造执行对象
	 * @param taskId 任务id
	 * @param operator 操作人
	 * @param args 参数列表
	 * @return Execution
	 */
	private Execution execute(String taskId, String operator, Map<String, Object> args) {
		..
		Task task = task().complete(taskId, operator, args);
		..
		Order order = query().getOrder(task.getOrderId());
		..
		order.setLastUpdator(operator);
		order.setLastUpdateTime(DateHelper.getTime());
		order().updateOrder(order);
		//协办任务完成不产生执行对象
		if(!task.isMajor()) {
			return null;
		}
		Map<String, Object> orderMaps = order.getVariableMap();
		if(orderMaps != null) {
			for(Map.Entry<String, Object> entry : orderMaps.entrySet()) {
				if(args.containsKey(entry.getKey())) {
					continue;
				}
				args.put(entry.getKey(), entry.getValue());
			}
		}
		Process process = process().getProcessById(order.getProcessId());
		Execution execution = new Execution(this, process, order, args);
		execution.setOperator(operator);
		execution.setTask(task);
		return execution;
	}


	// 继续执行流程
	public List<Task> executeTask(String taskId, String operator, Map<String, Object> args) {
		//完成任务，并且构造执行对象
		Execution execution = execute(taskId, operator, args);
		..
		ProcessModel model = execution.getProcess().getModel();
		if(model != null) {
			NodeModel nodeModel = model.getNode(execution.getTask().getTaskName());
			//将执行对象交给该任务对应的节点模型执行
			nodeModel.execute(execution);
		}
		return execution.getTasks();
	}
}

class Execution {

}

abstract class NodeModel {

	// 每个NodeModel之间，通过TransitionModel相互关联，从而形成有向图

	/**
	 * 输入变迁集合
	 */
	private List<TransitionModel> inputs = new ArrayList<TransitionModel>();
	/**
	 * 输出变迁集合
	 */
	private List<TransitionModel> outputs = new ArrayList<TransitionModel>();


	/**
	 * 对执行逻辑增加前置、后置拦截处理
	 * @param execution 执行对象
	 */
	public void execute(Execution execution) {
		intercept(preInterceptorList, execution);
		exec(execution);
		intercept(postInterceptorList, execution);
	}

	protected void runOutTransition(Execution execution) {
		for (TransitionModel tm : getOutputs()) {
			tm.setEnabled(true);
			tm.execute(execution);
		}
	}
}

 class StartModel extends NodeModel {
 	protected void exec(Execution execution) {
		runOutTransition(execution);
	}
 }

 class TaskModel extends WorkModel extends NodeModel {

 	/**
	 * 分配参与者处理对象
	 */
	private AssignmentHandler assignmentHandlerObject;



 	public boolean isPerformAny() {
		return "ANY".equalsIgnoreCase(this.performType);
	}
	
	public boolean isPerformAll() {
		return "ALL".equalsIgnoreCase(this.performType);
	}
	
 }

 class DecisionModel extends NodeModel {

 	public void exec(Execution execution) {
		..
		String next = null;
		if(StringHelper.isNotEmpty(expr)) {
			next = expression.eval(String.class, expr, execution.getArgs());
		} else if(decide != null) {
			next = decide.decide(execution);
		}
		..
		boolean isfound = false;
		for(TransitionModel tm : getOutputs()) {
			if(StringHelper.isEmpty(next)) {
				String expr = tm.getExpr();
				if(StringHelper.isNotEmpty(expr) && expression.eval(Boolean.class, expr, execution.getArgs())) {
					tm.setEnabled(true);
					tm.execute(execution);
					isfound = true;
				}
			} else {
				if(tm.getName().equals(next)) {
					tm.setEnabled(true);
					tm.execute(execution);
					isfound = true;
				}
			}
		}
		if(!isfound) throw new SnakerException(execution.getOrder().getId() + "->decision节点无法确定下一步执行路线");
	}

 }


 class TransitionModel extends BaseModel {

 	/**
	 * 变迁的源节点引用
	 */
	private NodeModel source;
	/**
	 * 变迁的目标节点引用
	 */
	private NodeModel target;

 	public void execute(Execution execution) {
		if(!enabled) return;
		if(target instanceof TaskModel) {
			//如果目标节点模型为TaskModel，则创建task
			fire(new CreateTaskHandler((TaskModel)target), execution);
		} else if(target instanceof SubProcessModel) {
			//如果目标节点模型为SubProcessModel，则启动子流程
			fire(new StartSubProcessHandler((SubProcessModel)target), execution);
		} else {
			//如果目标节点模型为其它控制类型，则继续由目标节点执行
			target.execute(execution);
		}
	}

	// 继承自BaseModel
	protected void fire(IHandler handler, Execution execution) {
		handler.handle(execution);
	}
 }


class CreateTaskHandler implements IHandler {
	/**
	 * 任务模型
	 */
	private TaskModel model;

	/**
	 * 调用者需要提供任务模型
	 * @param model 模型
	 */
	public CreateTaskHandler(TaskModel model) {
		this.model = model;
	}

	public void handle(Execution execution) {
		List<Task> tasks = execution.getEngine().task().createTask(model, execution);
		execution.addTasks(tasks);
		/**
		 * 从服务上下文中查找任务拦截器列表，依次对task集合进行拦截处理
		 */
		List<SnakerInterceptor> interceptors = ServiceContext.getContext().findList(SnakerInterceptor.class);
		try {
			for(SnakerInterceptor interceptor : interceptors) {
				interceptor.intercept(execution);
			}
		} catch(Exception e) {
			log.error("拦截器执行失败=" + e.getMessage());
			throw new SnakerException(e);
		}
	}
}

class TaskService {

	public List<Task> createTask(TaskModel taskModel, Execution execution) {
		List<Task> tasks = new ArrayList<Task>();
		
		Map<String, Object> args = execution.getArgs();
		..
		Date expireDate = DateHelper.processTime(args, taskModel.getExpireTime());
		Date remindDate = DateHelper.processTime(args, taskModel.getReminderTime());
		String form = (String)args.get(taskModel.getForm());
		String actionUrl = StringHelper.isEmpty(form) ? taskModel.getForm() : form;
		
		String[] actors = getTaskActors(taskModel, execution);
		args.put(Task.KEY_ACTOR, StringHelper.getStringByArray(actors));
		Task task = createTaskBase(taskModel, execution);
		task.setActionUrl(actionUrl);
		task.setExpireDate(expireDate);
		task.setExpireTime(DateHelper.parseTime(expireDate));
        task.setVariable(JsonHelper.toJson(args));
		
		if(taskModel.isPerformAny()) {
			//任务执行方式为参与者中任何一个执行即可驱动流程继续流转，该方法只产生一个task
			task = saveTask(task, actors);
			task.setRemindDate(remindDate);
			tasks.add(task);
		} else if(taskModel.isPerformAll()){
			//任务执行方式为参与者中每个都要执行完才可驱动流程继续流转，该方法根据参与者个数产生对应的task数量
			for(String actor : actors) {
                Task singleTask;
                try {
                    singleTask = (Task) task.clone();
                } catch (CloneNotSupportedException e) {
                    singleTask = task;
                }
                singleTask = saveTask(singleTask, actor);
                singleTask.setRemindDate(remindDate);
                tasks.add(singleTask);
			}
		}
		return tasks;
	}



	/**
	 * 完成指定任务
	 * 该方法仅仅结束活动任务，并不能驱动流程继续执行
	 * @see SnakerEngineImpl#executeTask(String, String, java.util.Map)
	 */
	public Task complete(String taskId, String operator, Map<String, Object> args) {
		Task task = access().getTask(taskId);
		..
		task.setVariable(JsonHelper.toJson(args));
		if(!isAllowed(task, operator)) {
			throw new SnakerException("当前参与者[" + operator + "]不允许执行任务[taskId=" + taskId + "]");
		}
		HistoryTask history = new HistoryTask(task);
		history.setFinishTime(DateHelper.getTime());
		history.setTaskState(STATE_FINISH);
		history.setOperator(operator);
		if(history.getActorIds() == null) {
			List<TaskActor> actors = access().getTaskActorsByTaskId(task.getId());
			String[] actorIds = new String[actors.size()];
			for(int i = 0; i < actors.size(); i++) {
				actorIds[i] = actors.get(i).getActorId();
			}
			history.setActorIds(actorIds);
		}
		access().saveHistory(history);
		access().deleteTask(task);
        Completion completion = getCompletion();
        if(completion != null) {
            completion.complete(history);
        }
		return task;
	}



	/**
	 * 根据Task模型的assignee、assignmentHandler属性以及运行时数据，确定参与者
	 * @param model 模型
	 * @param execution 执行对象
	 * @return 参与者数组
	 */
	private String[] getTaskActors(TaskModel model, Execution execution) {
		Object assigneeObject = null;
        AssignmentHandler handler = model.getAssignmentHandlerObject();
		if(StringHelper.isNotEmpty(model.getAssignee())) {
			assigneeObject = execution.getArgs().get(model.getAssignee());
		} else if(handler != null) {
            if(handler instanceof Assignment) {
                assigneeObject = ((Assignment)handler).assign(model, execution);
            } else {
                assigneeObject = handler.assign(execution);
            }
		}
		return getTaskActors(assigneeObject == null ? model.getAssignee() : assigneeObject);
	}

	// 将String、String[]、List<String>、数字等一系列actors表述方式，转化为String[]表达
	private String[] getTaskActors(Object actors) {
		..
	}




}

class Execution {

	/**
	 * 任务
	 */
	private Task task;
	/**
	 * 返回的任务列表
	 */
	private List<Task> tasks = new ArrayList<Task>();


}












