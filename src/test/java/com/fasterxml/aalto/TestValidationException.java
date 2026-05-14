package com.fasterxml.aalto;

import javax.xml.stream.Location;

import org.codehaus.stax2.validation.XMLValidationProblem;

import com.fasterxml.aalto.impl.LocationImpl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TestValidationException
    extends base.BaseTestCase
{
    @Test
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

    @Test
    public void testCreateWithLocation()
    {
        Location loc = new LocationImpl("pubId", "sysId", 17, 3, 9);
        XMLValidationProblem prob = new XMLValidationProblem(loc, "broken");
        ValidationException e = ValidationException.create(prob);

        // With a location, getMessage formats "<msg>\n at <loc.toString()>".
        String msg = e.getMessage();
        assertTrue(msg.contains("broken"), "should include problem message, got: " + msg);
        assertTrue(msg.contains(" at "), "should include 'at ' marker, got: " + msg);
        assertTrue(msg.contains(loc.toString()), "should include location string, got: " + msg);

        // Location is preserved on the exception itself.
        assertEquals(loc.getLineNumber(), e.getLocation().getLineNumber());
        assertEquals(loc.getColumnNumber(), e.getLocation().getColumnNumber());
    }

    @Test
    public void testToStringAlwaysIncludesMessage()
    {
        Location loc = new LocationImpl("p", "s", 0, 1, 1);
        XMLValidationProblem prob = new XMLValidationProblem(loc, "msg-text");
        ValidationException e = ValidationException.create(prob);

        String s = e.toString();
        assertTrue(s.startsWith(ValidationException.class.getName() + ": "), "toString must start with class name, got: " + s);
        assertTrue(s.contains("msg-text"), "toString must include message text, got: " + s);
    }
}
