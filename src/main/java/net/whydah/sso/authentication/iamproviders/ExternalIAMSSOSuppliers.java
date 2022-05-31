package net.whydah.sso.authentication.iamproviders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalIAMSSOSuppliers {

    private final static Logger log = LoggerFactory.getLogger(ExternalIAMSSOSuppliers.class);

    private static HashSet<String> iAMSupplierDomains = new HashSet<>();
    static String template = "./externaldomainconfig/configured_domain-config.properties";

    static {
        try {
            File folder = new File("./externaldomainconfig");
            File[] listOfFiles = folder.listFiles();

            for (File file : listOfFiles) {
                if (file.isFile()) {
                    String filename = file.getName();
                    String domainName = filename.replace("-config.properties", "");
                    log.info("Located filename:" + file.getName() + " mapped to domain:" + domainName);
                    iAMSupplierDomains.add(domainName.toLowerCase());
                }
            }
        } catch (Exception e) {
            log.warn("Unable to scan filesystem for AzureAD config files, reverting to hardcoded fallback ");
        }
    }
    
    
    public static void saveValidAzureAdConfig(InputStream configInputStream, String domain) throws IOException {
    	
    	Properties p = new Properties();
    	p.load(configInputStream);
    	
    	String secretKey = p.getProperty("aad.secretKey");
    	String clientId = p.getProperty("aad.clientId");
    	
    	if(secretKey!=null && clientId!=null) {
    		String fileName = template.replace("configured_domain", domain);
    		Properties prop = getProperties(fileName);
    		prop.put("aad.secretKey", secretKey);
			prop.put("aad.clientId", clientId);
			storeProperties(prop, fileName);
			iAMSupplierDomains.add(domain);
    	}
    }
    
    public static void saveValidGoogleConfig(InputStream configInputStream, String domain) throws IOException {
    	
    	Properties p = new Properties();
    	p.load(configInputStream);
    	
    	String secretKey = p.getProperty("google.secretKey");
    	String clientId = p.getProperty("google.clientId");
    	
    	if(secretKey!=null && clientId!=null) {
    		String fileName = template.replace("configured_domain", domain);
    		Properties prop = getProperties(fileName);
    		prop.put("google.secretKey", secretKey);
			prop.put("google.clientId", clientId);
			storeProperties(prop, fileName);
			iAMSupplierDomains.add(domain);
    	}
    }
    
    public static Properties getProperties(String fileName) {
    	if(!new File(fileName).exists()) {
    		return new Properties();
    	}
    	try (InputStream input = new FileInputStream(fileName)) {
			Properties prop = new Properties();
			prop.load(input);
			return prop;
		} catch (IOException ex) {
			log.warn("Unable to locate property config for using:" + fileName, ex);
		}
		return new Properties();
    }
    
    public static void storeProperties(Properties prop, String fileName) {
    	try (OutputStream output = new FileOutputStream(fileName)) {
			prop.store(output, null);
		} catch (IOException ex) {
			log.warn("Unable to save property config for using:" + fileName, ex);
		}
		
    }

    public static Set<String> getIAMSupplierDomainMatchers() {
        return iAMSupplierDomains;
    }


    public static Properties configurationForDomain(String domain) {
        for (String iamdomain : iAMSupplierDomains) {
            if (iamdomain.equalsIgnoreCase(domain)) {
                String filename = template.replace("configured_domain", iamdomain.toLowerCase());
                return getProperties(filename);
            }
        }
        return null;
    }
}
