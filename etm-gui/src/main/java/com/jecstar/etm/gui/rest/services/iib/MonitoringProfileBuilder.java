package com.jecstar.etm.gui.rest.services.iib;

import java.util.ArrayList;
import java.util.List;

public class MonitoringProfileBuilder {
	
	
	private static final String inputTemplate = "	<profile:eventSource profile:eventSourceAddress=\"<nodename>.<terminal>\" profile:enabled=\"true\">\r\n" + 
			"		<profile:eventPointDataQuery>\r\n" + 
			"			<profile:eventIdentity>\r\n" + 
			"				<profile:eventName profile:literal=\"<eventname>\" />\r\n" + 
			"			</profile:eventIdentity>\r\n" + 
			"			<profile:eventCorrelation>\r\n" + 
			"				<profile:localTransactionId profile:sourceOfId=\"automatic\" />\r\n" + 
			"				<profile:parentTransactionId profile:sourceOfId=\"automatic\" />\r\n" + 
			"				<profile:globalTransactionId profile:sourceOfId=\"automatic\" />\r\n" + 
			"			</profile:eventCorrelation>\r\n" + 
			"			<profile:eventFilter profile:queryText=\"true()\" />\r\n" + 
			"			<profile:eventUOW profile:unitOfWork=\"none\" />\r\n" + 
			"		</profile:eventPointDataQuery>\r\n" + 
			"		<profile:applicationDataQuery>\r\n" + 
			"			<profile:complexContent>\r\n" + 
			"				<profile:payloadQuery profile:queryText=\"$Root/MQMD/Encoding\" />\r\n" + 
			"			</profile:complexContent>\r\n" + 
			"			<profile:complexContent>\r\n" + 
			"				<profile:payloadQuery profile:queryText=\"$Root/MQMD/CodedCharSetId\" />\r\n" + 
			"			</profile:complexContent>\r\n" + 
			"			<profile:complexContent>\r\n" + 
			"				<profile:payloadQuery profile:queryText=\"$LocalEnvironment/Destination/SOAP/Reply/ReplyIdentifier\" />\r\n" + 
			"			</profile:complexContent>\r\n" + 
			"		</profile:applicationDataQuery>\r\n" + 
			"		<profile:bitstreamDataQuery profile:bitstreamContent=\"all\" profile:encoding=\"base64Binary\" />\r\n" + 
			"	</profile:eventSource>";
	
	private static final String outputTemplate = "	<profile:eventSource profile:eventSourceAddress=\"<nodename>.terminal.out\" profile:enabled=\"true\">\r\n" + 
			"		<profile:eventPointDataQuery>\r\n" + 
			"			<profile:eventIdentity>\r\n" + 
			"				<profile:eventName profile:literal=\"<eventname>\" />\r\n" + 
			"			</profile:eventIdentity>\r\n" + 
			"			<profile:eventCorrelation>\r\n" + 
			"				<profile:localTransactionId profile:sourceOfId=\"automatic\" />\r\n" + 
			"				<profile:parentTransactionId profile:sourceOfId=\"automatic\" />\r\n" + 
			"				<profile:globalTransactionId profile:sourceOfId=\"automatic\" />\r\n" + 
			"			</profile:eventCorrelation>\r\n" + 
			"			<profile:eventFilter profile:queryText=\"true()\" />\r\n" + 
			"			<profile:eventUOW profile:unitOfWork=\"none\" />\r\n" + 
			"		</profile:eventPointDataQuery>\r\n" + 
			"		<profile:applicationDataQuery>\r\n" + 
			"			<profile:complexContent>\r\n" + 
			"				<profile:payloadQuery profile:queryText=\"$Root/MQMD/Encoding\" />\r\n" + 
			"			</profile:complexContent>\r\n" + 
			"			<profile:complexContent>\r\n" + 
			"				<profile:payloadQuery profile:queryText=\"$Root/MQMD/CodedCharSetId\" />\r\n" + 
			"			</profile:complexContent>\r\n" + 
			"			<profile:complexContent>\r\n" + 
			"				<profile:payloadQuery profile:queryText=\"$Root/Properties/Topic\" />\r\n" + 
			"			</profile:complexContent>\r\n" + 
			"			<profile:complexContent>\r\n" + 
			"				<profile:payloadQuery profile:queryText=\"$LocalEnvironment/WrittenDestination\" />\r\n" + 
			"			</profile:complexContent>\r\n" + 
			"			<profile:complexContent>\r\n" + 
			"				<profile:payloadQuery profile:queryText=\"$LocalEnvironment/Destination/SOAP/Reply/ReplyIdentifier\" />\r\n" + 
			"			</profile:complexContent>\r\n" + 
			"		</profile:applicationDataQuery>\r\n" + 
			"		<profile:bitstreamDataQuery profile:bitstreamContent=\"all\" profile:encoding=\"base64Binary\" />\r\n" + 
			"	</profile:eventSource>";	
	
	private List<Node> nodes = new ArrayList<>();

	
	public void addNode(String name, String nodeType, String containerName, String containerVersion) {
		this.nodes.add(new Node(name, nodeType, containerName, containerVersion));
	}
	
	
	public String build() {
		StringBuilder profile = new StringBuilder();
		profile.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><profile:monitoringProfile xmlns:profile=\"http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0.3/monitoring/profile\" profile:version=\"2.0\" profile:enabled=\"true\">");
		for (Node node : nodes) {
			if (node.getNodeType().endsWith("InputNode") 
				|| "ComIbmMQGetNode".equals(node.getNodeType()) ) {
				profile.append(
						inputTemplate.replaceAll("<nodename>", node.getName())
						.replaceAll("<terminal>", "transaction.Start")
						.replaceAll("<eventname>", node.getEventName())
				);
			} else if (node.getNodeType().contains("Response") 
				|| node.getNodeType().contains("Reply") 
				|| "ComIbmMQOutputNode".equals(node.getNodeType()) 
				|| "ComIbmPublication".equals(node.getNodeType()) ) {
				profile.append(
						outputTemplate.replaceAll("<nodename>", node.getName())
						.replaceAll("<eventname>", node.getEventName())
				);
			}
		}
		profile.append("</profile:monitoringProfile>");
		return profile.toString();		
	}
	
	private class Node {
		
		private String name;
		private String nodeType;
		private String containerName;
		private String containerVersion;
		
		private Node(String name, String nodeType, String containerName, String containerVersion) {
			this.name = name;
			this.nodeType = nodeType;
			this.containerName = containerName;
			this.containerVersion = containerVersion;
		}

		public String getName() {
			return this.name;
		}
		
		public String getNodeType() {
			return this.nodeType;
		}
		
		public String getEventName() {
			if (containerName == null) {
				return "_unknown_";
			}
			if (containerVersion != null && containerVersion.trim().length() > 0) {
				return containerName.replace(",", "\\,") + "," + containerVersion.replace(",", "\\,");
			} 
			return containerName.replace(",", "\\,");
		}
		
	}

}
