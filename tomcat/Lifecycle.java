// LifecycleBase生命周期
LifecycleBase：
NEW
start{
	init(){
		INITIALIZING
		initInternal()
		INITIALIZED
	}
	STARTING_PREP
	startInternal(){
		STARTING
	}
	STARTED
}
destroy{
	DESTROYING
	destroyInternal()
	DESTROYED
}
stop{
	STOPPING_PREP
	stopInternal(){
		STOPPING
	}
	STOPPED
}