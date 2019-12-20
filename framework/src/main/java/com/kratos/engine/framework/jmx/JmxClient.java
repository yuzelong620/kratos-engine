package com.kratos.engine.framework.jmx;

import org.junit.Test;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.util.HashMap;
import java.util.Map;

public class JmxClient {

	/**
	 * 该工具类简直无敌，试想一下，在生产环境，直接用一个ide就可以链接服务器的jmx接口， 从而可以查看服务器的内存数据，或者执行任意代码，想想都激动不已
	 * ^_^
	 * 
	 */
	@Test
	public void getServerState() throws Exception {
		// 如果执行的groovy脚本内容过长
		// 则可以把脚本写在一个文件里，然后使用jmx client 动态调用mbean接口方法
		String user = "root";
		String pwd = "root";

		// 如果生产环境需要账号验证的话
		String[] account = new String[] { user, pwd };
		Map<String, String[]> props = new HashMap<String, String[]>();
//		props.put("jmx.remote.credentials", account);

		// 见启动脚本的vm参数，
		// -Dcom.sun.management.jmxremote.port=9090
		// -Dcom.sun.management.jmxremote.authenticate=false
		// -Dcom.sun.management.jmxremote.ssl=false
		JMXServiceURL address = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:9090/jmxrmi");
		JMXConnector connector = JMXConnectorFactory.connect(address, props);
		MBeanServerConnection mBeanConnection = connector.getMBeanServerConnection();

		connector.connect();
		ObjectName objectName = new ObjectName("GameMXBean:name=gameMonitor");
		final GameMonitorMBean mBean = JMX.newMBeanProxy(mBeanConnection, objectName, GameMonitorMBean.class);
		System.err.println(mBean.printServerState());
		System.err.println(mBean.getOnlinePlayerSum());
//		String script = FileUtil.readText("hotswap/groovy.txt");
//		System.err.println("script:\n" + script);

		System.err.println(mBean.execGroovyScript("def now = new Date1();"));
	}

}
