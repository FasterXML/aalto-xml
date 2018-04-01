package async;

import static org.junit.Assert.assertEquals;

import java.nio.charset.StandardCharsets;

import javax.xml.stream.XMLStreamException;

import org.junit.Test;

import com.fasterxml.aalto.AsyncByteArrayFeeder;
import com.fasterxml.aalto.AsyncXMLInputFactory;
import com.fasterxml.aalto.AsyncXMLStreamReader;
import com.fasterxml.aalto.stax.InputFactoryImpl;

/*
 * According to the XML spec, an hexadecimal character reference can start with a [A-Fa-f].
 * In the current state of the code, it is not possible.
 * 
 *  https://www.w3.org/TR/xml/#NT-Attribute
 *  Attribute	   ::=   	Name Eq AttValue 
 *  
 *  https://www.w3.org/TR/xml/#NT-AttValue
 *  AttValue	   ::=   	'"' ([^<&"] | Reference)* '"'
 *                       |  "'" ([^<&'] | Reference)* "'"
 *
 *  https://www.w3.org/TR/xml/#NT-Reference
 *  Reference	   ::=   	EntityRef | CharRef
 *  
 *  https://www.w3.org/TR/xml/#NT-CharRef
 *  CharRef	   ::=   	'&#' [0-9]+ ';'
 *                    | '&#x' [0-9a-fA-F]+ ';'  <-- here
 *  
 */
public class Issue40Test extends AsyncTestBase
{
	static AsyncXMLInputFactory FACTORY = new InputFactoryImpl();
	static String HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><root att=\"";
	static String FOOTER = "\"></root>";

	public void testHexEntitiesInAttributes() throws XMLStreamException
	{
		// non-regression of the fix 
		testHexEntityInAttribute("&#x0A;", "\n" /*expected string value*/);
		testHexEntityInAttribute("&#x0a;", "\n" /*expected string value*/);
		testHexEntityInAttribute("a&#x0A;a", "a\na" /*expected string value*/);
		testHexEntityInAttribute("a&#x0a;a", "a\na" /*expected string value*/);
		
		// unsupported without the fix
		testHexEntityInAttribute("&#xA;", "\n" /*expected string value*/);
		testHexEntityInAttribute("&#xa;", "\n" /*expected string value*/);
		testHexEntityInAttribute("&#xD;&#xA;", "\r\n" /*expected string value*/);
		testHexEntityInAttribute("&#xd;&#xa;", "\r\n" /*expected string value*/);
		testHexEntityInAttribute("a&#xA;a", "a\na" /*expected string value*/);
		testHexEntityInAttribute("a&#xa;a", "a\na" /*expected string value*/);
		testHexEntityInAttribute("a&#xD;&#xA;a", "a\r\na" /*expected string value*/);
		testHexEntityInAttribute("a&#xd;&#xa;a", "a\r\na" /*expected string value*/);
		
	}

	private void testHexEntityInAttribute(String entity, String expectedStringValue)
			throws XMLStreamException
	{
		AsyncXMLStreamReader<AsyncByteArrayFeeder> parser = FACTORY.createAsyncFor((HEADER + entity + FOOTER).getBytes(StandardCharsets.UTF_8));
		assertEquals(AsyncXMLStreamReader.START_DOCUMENT, parser.next());
		assertEquals(AsyncXMLStreamReader.START_ELEMENT, parser.next());
		assertEquals("root", parser.getName().getLocalPart());
		assertEquals(expectedStringValue, parser.getAttributeValue(0));
		assertEquals(AsyncXMLStreamReader.END_ELEMENT, parser.next());
		assertEquals("root", parser.getName().getLocalPart());
	}
}
