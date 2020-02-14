/*
 * Licensed to Jecstar Innovation under one or more contributor
 * license agreements. Jecstar Innovation licenses this file to you
 * under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied. See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package com.jecstar.etm.gui.rest.services.iib;

import java.util.ArrayList;
import java.util.List;

class MonitoringProfileBuilder {

    private static final String CORRELATION_PLACEHOLDER = "<applicationData>";

    private static final String SOAP_CORRELATION_IDENTIFIER =
            "			<profile:complexContent>\r\n" +
                    "				<profile:payloadQuery profile:queryText=\"$LocalEnvironment/Destination/SOAP/Reply/ReplyIdentifier\" />\r\n" +
                    "			</profile:complexContent>\r\n";

    private static final String HTTP_CORRELATION_IDENTIFIER =
            "			<profile:complexContent>\r\n" +
                    "				<profile:payloadQuery profile:queryText=\"$LocalEnvironment/Destination/HTTP/RequestIdentifier\" />\r\n" +
                    "			</profile:complexContent>\r\n";

    private static final String MQ_WRITTEN_DESTINATION =
            "			<profile:complexContent>\r\n" +
                    "				<profile:payloadQuery profile:queryText=\"$LocalEnvironment/WrittenDestination/MQ/DestinationData\" />\r\n" +
                    "			</profile:complexContent>\r\n";
    private static final String MQ_TOPIC =
            "			<profile:complexContent>\r\n" +
                    "				<profile:payloadQuery profile:queryText=\"$Root/Properties/Topic\" />\r\n" +
                    "			</profile:complexContent>\r\n";

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
            CORRELATION_PLACEHOLDER +
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
            CORRELATION_PLACEHOLDER +
            "		</profile:applicationDataQuery>\r\n" +
            "		<profile:bitstreamDataQuery profile:bitstreamContent=\"all\" profile:encoding=\"base64Binary\" />\r\n" +
            "	</profile:eventSource>";

    private final List<Node> nodes = new ArrayList<>();


    public void addNode(String name, String nodeType, String containerName, String containerVersion) {
        this.nodes.add(new Node(name, nodeType, containerName, containerVersion));
    }


    public String build() {
        StringBuilder profile = new StringBuilder();
        profile.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?><profile:monitoringProfile xmlns:profile=\"http://www.ibm.com/xmlns/prod/websphere/messagebroker/6.1.0.3/monitoring/profile\" profile:version=\"2.0\" profile:enabled=\"true\">");
        for (Node node : nodes) {
            if (node.isInputNode()) {
                // All nodes that act as an input node.
                String monitoringProfile = inputTemplate.replaceAll("<nodename>", node.getName())
                        .replaceAll("<terminal>", "transaction.Start")
                        .replaceAll("<eventname>", node.getEventName());

                if (node.isHttpNode()) {
                    monitoringProfile = monitoringProfile.replace(CORRELATION_PLACEHOLDER, HTTP_CORRELATION_IDENTIFIER);
                } else if (node.isWebServiceNode()) {
                    monitoringProfile = monitoringProfile.replace(CORRELATION_PLACEHOLDER, SOAP_CORRELATION_IDENTIFIER);
                } else {
                    monitoringProfile = monitoringProfile.replace(CORRELATION_PLACEHOLDER, "");
                }
                profile.append(monitoringProfile);
            } else if (node.isOutputNode()) {
                String monitoringProfile = outputTemplate.replaceAll("<nodename>", node.getName())
                        .replaceAll("<eventname>", node.getEventName());
                if (node.isHttpNode()) {
                    monitoringProfile = monitoringProfile.replace(CORRELATION_PLACEHOLDER, HTTP_CORRELATION_IDENTIFIER);
                } else if (node.isWebServiceNode()) {
                    monitoringProfile = monitoringProfile.replace(CORRELATION_PLACEHOLDER, SOAP_CORRELATION_IDENTIFIER);
                } else if (node.isTopicPublicationNode()) {
                    monitoringProfile = monitoringProfile.replace(CORRELATION_PLACEHOLDER, MQ_TOPIC);
                } else if (node.isMqNode()) {
                    monitoringProfile = monitoringProfile.replace(CORRELATION_PLACEHOLDER, MQ_WRITTEN_DESTINATION);
                } else {
                    monitoringProfile = monitoringProfile.replace(CORRELATION_PLACEHOLDER, "");
                }
                profile.append(monitoringProfile);
            }
        }
        profile.append("</profile:monitoringProfile>");
        return profile.toString();
    }

    private class Node {

        private final String name;
        private final String nodeType;
        private final String containerName;
        private final String containerVersion;

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

        public boolean isInputNode() {
            return getNodeType().endsWith("InputNode")
                    || "ComIbmMQGetNode".equals(getNodeType())
                    ;
        }

        public boolean isOutputNode() {
            return getNodeType().contains("Response")
                    || getNodeType().contains("Reply")
                    || "ComIbmMQOutputNode".equals(getNodeType())
                    || "ComIbmPublication".equals(getNodeType())
                    ;
        }

        public boolean isHttpNode() {
            return getNodeType().startsWith("ComIbmHTTP")
                    || getNodeType().startsWith("ComIbmWS")
                    ;
        }

        public boolean isWebServiceNode() {
            return getNodeType().startsWith("ComIbmSOAP");
        }

        public boolean isTopicPublicationNode() {
            return "ComIbmPublication".equals(getNodeType());
        }

        public boolean isMqNode() {
            return getNodeType().startsWith("ComIbmMQ");
        }
    }

}
