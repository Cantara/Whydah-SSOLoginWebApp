package net.whydah.sso.utils;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hazelcast.cluster.Member;
import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.listener.MapListener;

public class HazelcastMapHelper {

	private final static Logger log = LoggerFactory.getLogger(HazelcastMapHelper.class);
	private static Config hazelcastConfig;
	private static String tag ="";
	private static HazelcastInstance hazelcastInstance;

	static {
//		String xmlFileName = System.getProperty("hazelcast.config");
//		log.info("Loading hazelcast configuration from :" + xmlFileName);
//		hazelcastConfig = new Config();
//		if (xmlFileName != null && xmlFileName.length() > 10) {
//			try {
//				hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
//				log.info("Loading hazelcast configuration from :" + xmlFileName);
//			} catch (FileNotFoundException notFound) {
//				log.error("Error - not able to load hazelcast.xml configuration.  Using embedded as fallback");
//			}
//		}
//		hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");
//		
//		try {
//			hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
//		} catch(Exception ex) {
//			hazelcastInstance = Hazelcast.newHazelcastInstance();
//		}
		hazelcastInstance = Hazelcast.newHazelcastInstance();
		
		try {
			AwsConfig awsConfig = hazelcastInstance.getConfig().getNetworkConfig().getJoin().getAwsConfig();
			if(awsConfig!=null) {
				tag = awsConfig.getProperty("tag-value")!=null?awsConfig.getProperty("tag-value"):"";
			} else {
				tag = "local";
			}
			
		} catch(Exception ex) {

		}
	}

	public static IMap register(String name, MapListener listener) {
		log.info("Connectiong to map {}", tag + "_" + name);		
		IMap result = hazelcastInstance.getMap(tag + "_" + name);
		if(listener!=null) {
			result.addEntryListener(listener, true);
		}
		return result;

	}
	
	public static IMap register(String name) {
		return register(name, null);
	}
	
	public static String localMember() {
		try {
			return hazelcastInstance.getCluster().getLocalMember().getAddress().getInetAddress().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static boolean isLeader() {
		Iterator<HazelcastInstance> iter = Hazelcast.getAllHazelcastInstances().iterator();

		if (iter.hasNext()) { // cluster mode 
			HazelcastInstance instance = iter.next();
			return instance.getCluster().getMembers().iterator().next().localMember();
		} else {
			return true; // standalone mode
		}
	}

	public static Set<String> getClusterMembers() {
		Set<String> inetAddresses = new HashSet<>();
		Set<Member> members = hazelcastInstance.getCluster().getMembers();
		members.stream().forEach(member -> {
			try {
				inetAddresses.add(member.getAddress().getInetAddress().getHostAddress());
			} catch (UnknownHostException e) {
				log.error("Unable to gather IP address from hazelcast member", e);
			}
		});
		return inetAddresses;
	}

	public static HazelcastInstance getHazelcastInstance() {
		return hazelcastInstance;
	}
}