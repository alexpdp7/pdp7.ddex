package net.pdp7.ddex.utils;

import java.io.File;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.pdp7.ddex.utils.jaxb.NewReleaseMessage;

public class DdexToTableConverter {

	public Map<String, Object> convert(File file) throws JAXBException {
		return convert((NewReleaseMessage) createUnmarshaller().unmarshal(file));
	}

	public Map<String, Object> convert(NewReleaseMessage release) {
		return null;
	}

	protected Unmarshaller createUnmarshaller() throws JAXBException {
		return JAXBContext.newInstance(NewReleaseMessage.class).createUnmarshaller();
	}

}
