/*
 *  Copyright 2016, 2017,2018, Mindtree Ltd., IBM, DTCC, Fujitsu Australia Software Technology, IBM - All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.blockchain.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hyperledger.fabric.sdk.NetworkConfig;
import org.hyperledger.fabric.sdk.helper.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * Config allows for a global config of the toolkit. Central location for all
 * toolkit configuration defaults.
 */

public class Config {

	private static final Log logger = LogFactory.getLog(Config.class);

	private static final String PROPBASE = "config.";
	private static String PATH = System.getProperty("user.dir");
	

	private static final String GOSSIPWAITTIME = PROPBASE + "GossipWaitTime";
	private static final String INVOKEWAITTIME = PROPBASE + "InvokeWaitTime";
	private static final String DEPLOYWAITTIME = PROPBASE + "DeployWaitTime";
	private static final String PROPOSALWAITTIME = PROPBASE + "ProposalWaitTime";

	private static final String ORGS = PROPBASE + "property.";
	private static final Pattern orgPat = Pattern.compile("^" + Pattern.quote(ORGS) + "([^\\.]+)\\.mspid$");

	private static final String BLOCKCHAINTLS = PROPBASE + "com.blockchain.tls";

	private static Config config;
	public static final Properties sdkProperties = new Properties();
	private final boolean runningTLS;
	private final boolean runningFabricCATLS;
	private final boolean runningFabricTLS;

	private Config() {

		try {

			/**
			 * All the properties will be obtained from config.properties file
			 */
			InputStream inputStream = Config.class.getResourceAsStream("/config.properties");

			sdkProperties.load(inputStream);

			

		} catch (IOException e) {
			// if not there no worries just use defaults
			logger.warn("Failed to load any configuration");
		} finally {

			// Default values

			defaultProperty(GOSSIPWAITTIME, "5000");
			defaultProperty(INVOKEWAITTIME, "100000");
			defaultProperty(DEPLOYWAITTIME, "120000");
			defaultProperty(PROPOSALWAITTIME, "120000");

			defaultProperty(ORGS + "peerOrg1.mspid", "Org1MSP");
			defaultProperty(ORGS + "peerOrg1.domname", "org1.example.com");
			defaultProperty(ORGS + "peerOrg1.ca_location", "http://localhost:7054");
			defaultProperty(ORGS + "peerOrg1.peer_locations",
					"peer0.org1.example.com@grpc://localhost:7051, peer1.org1.example.com@grpc://localhost:7056");
			defaultProperty(ORGS + "peerOrg1.orderer_locations", "orderer.example.com@grpc://localhost:7050");
			defaultProperty(ORGS + "peerOrg1.eventhub_locations",
					"peer0.org1.example.com@grpc://localhost:7053,peer1.org1.example.com@grpc://localhost:7058");

			defaultProperty(BLOCKCHAINTLS, null);
			runningTLS = null != sdkProperties.getProperty(BLOCKCHAINTLS, null);
			runningFabricCATLS = runningTLS;
			runningFabricTLS = runningTLS;

		}

	}

	private String grpcTLSify(String location) {
		location = location.trim();
		Exception e = Utils.checkGrpcUrl(location);
		if (e != null) {
			throw new RuntimeException(String.format("Bad  parameters for grpc url %s", location), e);
		}
		return runningFabricTLS ? location.replaceFirst("^grpc://", "grpcs://") : location;

	}

	private String httpTLSify(String location) {
		location = location.trim();

		return runningFabricCATLS ? location.replaceFirst("^http://", "https://") : location;
	}

	/**
	 * getConfig return back singleton for SDK configuration.
	 *
	 * @return Global configuration
	 */
	public static Config getConfig() {
		if (null == config) {
			config = new Config();
		}
		return config;

	}

	/**
	 * getProperty return back property for the given value.
	 *
	 * @param property
	 * @return String value for the property
	 */
	public String getProperty(String property) {

		String ret = sdkProperties.getProperty(property);

		if (null == ret) {
			logger.warn(String.format("No configuration value found for '%s'", property));
		}
		return ret;
	}

	private static void defaultProperty(String key, String value) {

		String ret = System.getProperty(key);
		if (ret != null) {
			sdkProperties.put(key, ret);
		} else {
			String envKey = key.toUpperCase().replaceAll("\\.", "_");
			ret = System.getenv(envKey);
			if (null != ret) {
				sdkProperties.put(key, ret);
			} else {
				if (null == sdkProperties.getProperty(key) && value != null) {
					sdkProperties.put(key, value);
				}

			}

		}
	}

	public int getTransactionWaitTime() {
		return Integer.parseInt(getProperty(INVOKEWAITTIME));
	}

	public int getDeployWaitTime() {
		return Integer.parseInt(getProperty(DEPLOYWAITTIME));
	}

	public int getGossipWaitTime() {
		return Integer.parseInt(getProperty(GOSSIPWAITTIME));
	}

	public long getProposalWaitTime() {
		return Integer.parseInt(getProperty(PROPOSALWAITTIME));
	}


	public Properties getPeerProperties(String name) {

		return getEndPointProperties("peer", name);

	}

	public Properties getOrdererProperties(String name) {

		return getEndPointProperties("orderer", name);

	}

	private Properties getEndPointProperties(final String type, final String name) {

		final String domainName = getDomainName(name);

		File cert = Paths.get(getChannelPath(), "crypto-config/ordererOrganizations".replace("orderer", type),
				domainName, type + "s", name, "tls/server.crt").toFile();
		if (!cert.exists()) {
			throw new RuntimeException(String.format("Missing cert file for: %s. Could not find at location: %s", name,
					cert.getAbsolutePath()));
		}

		File clientCertFile = Paths.get(getChannelPath(), "crypto-config/ordererOrganizations".replace("orderer", type),
				domainName, type + "s", name, "tls/server.crt").toFile();
		if (!clientCertFile.exists()) {
			throw new RuntimeException(String.format("Missing clientCertFile file for: %s. Could not find at location: %s", name,
					cert.getAbsolutePath()));
		}

		File clientKeyFile = Paths.get(getChannelPath(), "crypto-config/ordererOrganizations".replace("orderer", type),
				domainName, type + "s", name, "tls/server.key").toFile();
		if (!clientKeyFile.exists()) {
			throw new RuntimeException(String.format("Missing clientKeyFile file for: %s. Could not find at location: %s", name,
					cert.getAbsolutePath()));
		}

		Properties ret = new Properties();
		ret.setProperty("pemFile", cert.getAbsolutePath());
//
		ret.setProperty("hostnameOverride", name);
		ret.setProperty("sslProvider", "openSSL");
		ret.setProperty("negotiationType", "TLS");

		Properties testprops = new Properties();

//		testprops.setProperty("trustServerCertificate", "true");
//		testprops.setProperty("pemFile", System.getProperty("user.dir") + "/src/test/resources/keypair-signed.crt");
//		testprops.setProperty("sslProvider", "openSSL");
//		testprops.setProperty("hostnameOverride", "override");
//		testprops.setProperty("negotiationType", "TLS");
//		testprops.setProperty("clientKeyFile", System.getProperty("user.dir") + "/src/test/resources/tls-client.key");
//		testprops.setProperty("clientCertFile", System.getProperty("user.dir") + "/src/test/resources/tls-client.crt");
//		Endpoint endpoint = new Endpoint("grpcs://localhost:594", testprops);
//		Properties peerProperties = new Properties();
//		peerProperties.setProperty("pemFile", peerCert.getAbsolutePath());
//		if(type.equalsIgnoreCase("peer")){
//			ret.setProperty("clientCertFile", clientCertFile.getAbsolutePath());
//			ret.setProperty("clientKeyFile", clientKeyFile.getAbsolutePath());
//		}

//		ret.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", 9000000);
//		ret.put("org.hyperledger.fabric.sdk.proposal.wait.time", 200000);
		ret.setProperty("trustServerCertificate", "true");

//		 ret.setProperty("trustServerCertificate", "true"); //testing
		//TODO
		//peerProperties.setProperty("trustServerCertificate", "true");
//		peerProperties.setProperty("hostnameOverride", peers.getOrgDomainName());
//		peerProperties.setProperty("sslProvider", "openSSL");
//		peerProperties.setProperty("negotiationType", "TLS");
		// 在grpc的NettyChannelBuilder上设置特定选项
//		ret.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", 9000000);
//		ret.put("org.hyperledger.fabric.sdk.proposal.wait.time", 200000);

		return ret;
	}

	public Properties getEventHubProperties(String name) {

		return getEndPointProperties("peer", name); // uses same as named peer

	}

	public String getChannelPath() {

		/**
		 * for loading properties from hyperledger.properties file
		 */
		Properties hyperproperties = new Properties();
		try {
			hyperproperties.load(new FileInputStream("src/main/resources/hyperledger.properties"));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return PATH+"/artifacts/channel";

	}

	private String getDomainName(final String name) {
		int dot = name.indexOf(".");
		if (-1 == dot) {
			return null;
		} else {
			return name.substring(dot + 1);
		}

	}

}
