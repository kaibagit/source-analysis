http://localhost:8080/snaker-web/snaker/flow/all?processId=3748f012caad44a38750351f375a5c08&processName=borrow
http://localhost:8080/snaker-web/snaker/flow/all?processId=0d6c084f5725458c85523a981f4583eb&processName=leave


FlowController.all		启动流程开始界面，还没正式启动


BorrowController.applySave	提交借款数据，正式启动流程
	Order order = engine.startInstanceById(processId, operator, args);

// 流转到审批，查询task任务列表，然后根据task的form显示页面，等待操作

FlowController.doApproval	提交审批
	manager.save(model);	保存业务数据
	Map<String, Object> params = new HashMap<String, Object>();
    params.put("result", model.getResult());
	engine.executeTask(taskId, operator, args);		启动工作流流转
