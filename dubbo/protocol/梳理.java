ServiceConfig中得到的protocol实例为ExtensionLoader.getExtensionLoader(Protocol.class).getAdaptiveExtension();
该Protocol是在原始的DubboProtocol上wrap了一层层的其他protocol，其中包括ProtocolListenerWrapper
ProtocolListenerWrapper通过检测URL是否为"registry://"类型，如果是，则交由RegistryProtocol来export；否则交由下次protocol处理。
注册中心也是通过Protocol实现来注册上去的



Protocol封装：
ReferenceConfig.refprotocol = Protocol$Adaptive{
	slot_5(ProtocolListenerWrapper):{
		protocol(ProtocolFilterWrapper):{
			protocol(RegistryProtocol):{
				protocol(Protocol$Adaptive)
			}
		}
	}
}