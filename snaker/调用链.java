// 开始流程
SnakerEngineImpl#startProcess(Process process, String operator, Map<String, Object> args)
	->SnakerEngineImpl#startProcess(Process process, String operator, Map<String, Object> args)
		->StartModel#exec(Execution execution)
			->NodeModel(StartModel)#runOutTransition(Execution execution)
				->TransitionModel#execute(Execution execution)
					->CreateTaskHandler#handle(Execution execution)
						->TaskService#createTask(TaskModel taskModel, Execution execution)
							->TaskService#saveTask(Task task, String... actors) 

// 当前task的执行者actor来源：
// 1、在xml定义task时，指定assignee参数，然后该参数会被流程启动时的args中的assignee参数值替换
// 2、在xml定义task时，指定assignmentHandlerObject参数，该参数是一个AssignmentHandler接口实现的className，由它来动态指定

// 继续流程
SnakerEngineImpl#executeTask(String taskId, String operator, Map<String, Object> args)
	->SnakerEngineImpl#execute(String taskId, String operator, Map<String, Object> args) 
		->TaskService#complete(String taskId, String operator, Map<String, Object> args)
		->NodeModel#execute(Execution execution)