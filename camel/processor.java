基于版本：2.22.0

// 处理器定义
class ProcessorDefinition{

	public Type process(Processor processor) {
        ProcessDefinition answer = new ProcessDefinition(processor);
        addOutput(answer);
        return (Type) this;
    }

    public Type setHeader(String name, Expression expression) {
        SetHeaderDefinition answer = new SetHeaderDefinition(name, expression);
        addOutput(answer);
        return (Type) this;
    }

    public ChoiceDefinition choice() {
        ChoiceDefinition answer = new ChoiceDefinition();
        addOutput(answer);
        return answer;
    }

    public ChoiceDefinition when(@AsPredicate Predicate predicate) {
        addClause(new WhenDefinition(predicate));
        return this;
    }

    public void addOutput(ProcessorDefinition<?> output) {
        if (!blocks.isEmpty()) {
            // let the Block deal with the output
            Block block = blocks.getLast();
            block.addOutput(output);
            return;
        }

        // validate that top-level is only added on the route (eg top level)
        boolean parentIsRoute = RouteDefinition.class.isAssignableFrom(this.getClass());
        if (output.isTopLevelOnly() && !parentIsRoute) {
            throw new IllegalArgumentException("The output must be added as top-level on the route. Try moving " + output + " to the top of route.");
        }

        output.setParent(this);
        configureChild(output);
        getOutputs().add(output);
    }
}

// 处理过程定义
class ProcessDefinition{
	Processor processor;
}

// 处理器
class Processor{

}


class RouteDefinition extends ProcessorDefinition<RouteDefinition>{
	List<ProcessorDefinition<?>> outputs = new ArrayList<>();
}