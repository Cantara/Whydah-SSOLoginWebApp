package net.whydah.sso;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class HazelcastTest {
    public static void main(String[] args) {
        Config config = new Config();
        config.setClusterName("dev");
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(false);
        config.getNetworkConfig().getJoin().getAwsConfig().setEnabled(false);
        config.setProperty("hazelcast.logging.type", "slf4j");
        try {
            HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);
            System.out.println("Hazelcast started: " + instance.getName());
            instance.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}