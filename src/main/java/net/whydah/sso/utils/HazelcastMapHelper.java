package net.whydah.sso.utils;

import com.hazelcast.cluster.Member;
import com.hazelcast.collection.ISet;
import com.hazelcast.collection.ItemListener;
import com.hazelcast.config.AwsConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.impl.proxy.MapProxyImpl;
import com.hazelcast.map.listener.MapListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;

public class HazelcastMapHelper {

	private final static Logger log = LoggerFactory.getLogger(HazelcastMapHelper.class);
	static Config hazelcastConfig;
	static String tag ="";
	static HazelcastInstance hazelcastInstance;

	static {
		String xmlFileName = System.getProperty("hazelcast.config");
		log.info("Loading hazelcast configuration from :" + xmlFileName);
		hazelcastConfig = new Config();
		if (xmlFileName != null && xmlFileName.length() > 10) {
			try {
				hazelcastConfig = new XmlConfigBuilder(xmlFileName).build();
				log.info("Loading hazelcast configuration from :" + xmlFileName);
			} catch (FileNotFoundException notFound) {
				log.error("Error - not able to load hazelcast.xml configuration.  Using embedded as fallback");
			}
		}
		hazelcastConfig.setProperty("hazelcast.logging.type", "slf4j");

		try {
			AwsConfig awsConfig = hazelcastConfig.getNetworkConfig().getJoin().getAwsConfig();
			tag = awsConfig.getProperty("tag-value")!=null?awsConfig.getProperty("tag-value"):"";

		} catch(Exception ex) {

		}

		try {
			hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
		} catch(Exception ex) {
			//hazelcastInstance = Hazelcast.newHazelcastInstance();//.bootstrappedInstance();//.newHazelcastInstance();
		}

	}

	public static IMap registerMap(String name, MapListener listener) {


		log.info("Connectiong to map {}", tag + "_" + name);



		IMap result = hazelcastInstance.getMap(tag + "_" + name);
		if(listener!=null) {
			result.addEntryListener(listener, true);
		}
		return result;

	}

	public static String localMember() {
		try {
			return hazelcastInstance.getCluster().getLocalMember().getAddress().getInetAddress().getHostAddress();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Set<String> getClusterMembers() {
		Set<String> inetAddresses = new LinkedHashSet<>();
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

	public static IMap registerMap(String name) {
		//IMap<String, String> result = new MapProxyImpl<>(name,null,null,null);
		try {
			return registerMap(name, null);
        } catch (Exception e){
			return null;
		}
	}

	public static ISet registerSet(String name, ItemListener listener) {

		HazelcastInstance hazelcastInstance;
		try {
			hazelcastInstance = Hazelcast.newHazelcastInstance(hazelcastConfig);
		} catch(Exception ex) {
			hazelcastInstance = Hazelcast.newHazelcastInstance();
		}

		log.info("Connectiong to map {}", tag + "_" + name);



		ISet result = hazelcastInstance.getSet(tag + "_" + name);
		if(listener!=null) {
			result.addItemListener(listener, true);
		}
		return result;

	}

	public static ISet registerSet(String name) {
		return registerSet(name, null);
	}
}
