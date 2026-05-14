package com.fasterxml.aalto;

import javax.xml.stream.Location;

import org.codehaus.stax2.validation.XMLValidationProblem;

import com.fasterxml.aalto.impl.LocationImpl;

public class TestValidationException
    extends base.BaseTestCase
{
    public void testCreateWithoutLocation()
    {
        XMLValidationProblem prob = new XMLValidationProblem(null, "bad-stuff");
        ValidationException e = ValidationException.create(prob);

        // Without a location the formatted message is just the problem message.
        assertEquals("bad-stuff", e.getMessage());
        // toString uses the FQ class name + ": " + message.
        assertEquals(ValidationException.class.getName() + ": bad-stuff", e.toString());

        // The original problem is reachable via the parent API.
        assertSame(prob, e.getValidationProblem());
    }

    public void testCreateWithLocation()
    {
        Location loc = new LocationImpl("pubId", "sysId", 17, 3, 9);
        XMLValidationProblem prob = new XMLValidationProblem(loc, "broken");
        ValidationException e = ValidationException.create(prob);

        // With a location, getMessage formats "<msg>\n at <loc.toString()>".
        String msg = e.getMessage();
        assertTrue("should include problem message, got: " + msg,
                msg.contains("broken"));
        assertTrue("should include 'at ' marker, got: " + msg,
                msg.contains(" at "));
        assertTrue("should include location string, got: " + msg,
                msg.contains(loc.toString()));

        // Location is preserved on the exception itself.
        assertEquals(loc.getLineNumber(), e.getLocation().getLineNumber());
        assertEquals(loc.getColumnNumber(), e.getLocation().getColumnNumber());
    }

    public void testToStringAlwaysIncludesMessage()
    {
        Location loc = new LocationImpl("p", "s", 0, 1, 1);
        XMLValidationProblem prob = new XMLValidationProblem(loc, "msg-text");
        ValidationException e = ValidationException.create(prob);

        String s = e.toString();
        assertTrue("toString must start with class name, got: " + s,
                s.startsWith(ValidationException.class.getName() + ": "));
        assertTrue("toString must include message text, got: " + s,
                s.contains("msg-text"));
    }
}
