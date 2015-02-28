package com.jecstar.etm.processor.mdb;

import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

public class SchemaGenerator {

	public static void main(String[] args) throws JAXBException, IOException {
		JAXBContext jaxbContext = JAXBContext.newInstance(XmlTelemetryEvent.class);
		SchemaOutputResolver sor = new SchemaOutputResolver() {
			
			@Override
			public Result createOutput(String namespaceUri, String suggestedFileName) throws IOException {
				StreamResult streamResult = new StreamResult(System.out);
				streamResult.setSystemId(namespaceUri);
				return streamResult;
			}
		};
		jaxbContext.generateSchema(sor);
    }
}
