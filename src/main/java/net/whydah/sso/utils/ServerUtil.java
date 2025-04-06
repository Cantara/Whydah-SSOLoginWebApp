package net.whydah.sso.utils;

import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServerUtil {

	public static boolean isServerOnline(String ip, int port) {
		boolean b = true;
		try{
			InetSocketAddress sa = new InetSocketAddress(ip, port);
			Socket ss = new Socket();
			ss.connect(sa, 1);
			ss.close();
		}catch(Exception e) {
			b = false;
		}
		return b;
	}
	
	public static boolean isServerOnline(String uri){
		Pattern p = Pattern.compile("(https?://)([^:^/]*)(:\\d*)?(.*)?");
		if(!p.matcher(uri).find()){
			uri ="http://" + uri; 
		}
		Matcher m = p.matcher(uri);
		if (m.matches()) {
			String host=m.group(2).replaceFirst("www.", "");
			String port=m.group(3)!=null?m.group(3):"80"; //default 80
			return isServerOnline(host, Integer.valueOf(port));
		}
		return true;
	}

	public static boolean compare(String url1, String url2){
        if (url1 == null) {
            return false;
        }
        try {
			url1 =  URLDecoder.decode(url1, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		}
        if (url1.contains("?")) {
            url1 = url1.substring(0, url1.indexOf("?"));
        }
		if (url2 == null) {
			return false;
		}
		 try {
				url2 =  URLDecoder.decode(url2, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return false;
			}
		if (url2.contains("?")) {
			url2 = url2.substring(0, url2.indexOf("?"));
		}
		if(url1!=null&&url2!=null){
			Pattern p = Pattern.compile("(https?://)([^:^/]*)(:\\d*)?(.*)?");
			if(!p.matcher(url1).find()){
				url1 ="http://" + url1; 
			}
			if(!p.matcher(url2).find()){
				url2 ="http://" + url2; 
			}
			Matcher m1 = p.matcher(url1);
			Matcher m2 = p.matcher(url2);
			String host1="host1",host2="host2";
			String port1="",port2="";
			String sub1="", sub2="";
			if (m1.matches()) {
				host1=m1.group(2).replaceFirst("www.", "");
				port1=m1.group(3)!=null?m1.group(3):"";
				sub1=m1.group(4)!=null?m1.group(4).replaceFirst("/$", ""):"";
			}
			if(m2.matches()){
				host2=m2.group(2).replaceFirst("www.", "");
				port2=m2.group(3)!=null?m2.group(3):"";
				sub2=m2.group(4)!=null?m2.group(4).replaceFirst("/$", ""):"";
			}
            System.out.println("url1: " + host1 + (port1.equals("") ? "" : ":" + port1) +  sub1 +", url2: " + host2 + (port2.equals("") ? "" : ":" + port2) + sub2);
            return host1.equalsIgnoreCase(host2) && port1.equalsIgnoreCase(port2) && sub1.equalsIgnoreCase(sub2);
		}  else {
			return false;
		}
	}

}
