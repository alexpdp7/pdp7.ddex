package net.pdp7.ddex.utils;

import java.io.File;

import javax.xml.bind.JAXBContext;

import junit.framework.TestCase;
import net.pdp7.ddex.utils.jaxb.NewReleaseMessage;

public class UnmarshallTest extends TestCase {
	public void test() throws Exception {
		NewReleaseMessage release = (NewReleaseMessage) JAXBContext.newInstance(NewReleaseMessage.class).createUnmarshaller().unmarshal(new File("src/test/resources/8421597103035.xml"));
		assertNotNull(release);
	}
}
