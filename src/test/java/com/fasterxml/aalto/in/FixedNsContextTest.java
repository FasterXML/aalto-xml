/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fasterxml.aalto.in;

import junit.framework.TestCase;
import org.junit.Assert;

import javax.xml.XMLConstants;

public class FixedNsContextTest extends TestCase {

    public void testFixedNsContextDefaultNsNull() {
        NsBinding defaultNs = NsBinding.createDefaultNs();

        NsDeclaration sut = new NsDeclaration(defaultNs, null, null, 0);
        sut = new NsDeclaration(new NsBinding("xyz"), "https://xyz.yzx", sut, 0);

        FixedNsContext fixedNsContext = FixedNsContext.EMPTY_CONTEXT.reuseOrCreate(sut);

        Assert.assertEquals(null, fixedNsContext.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));

        Assert.assertEquals("https://xyz.yzx", fixedNsContext.getNamespaceURI("xyz"));
        Assert.assertEquals("xyz", fixedNsContext.getPrefix("https://xyz.yzx"));
    }

    public void testFixedNsContextEmptyDefaultNsNotNull() {
        NsBinding defaultNs = NsBinding.createDefaultNs();

        NsDeclaration sut = new NsDeclaration(defaultNs, "http://localhost/", null, 0);
        sut = new NsDeclaration(new NsBinding("xyz"), "https://xyz.yzx", sut, 0);

        FixedNsContext fixedNsContext = FixedNsContext.EMPTY_CONTEXT.reuseOrCreate(sut);

        Assert.assertEquals("http://localhost/", fixedNsContext.getNamespaceURI(XMLConstants.DEFAULT_NS_PREFIX));
        Assert.assertEquals(XMLConstants.DEFAULT_NS_PREFIX, fixedNsContext.getPrefix("http://localhost/"));

        Assert.assertEquals("https://xyz.yzx", fixedNsContext.getNamespaceURI("xyz"));
        Assert.assertEquals("xyz", fixedNsContext.getPrefix("https://xyz.yzx"));
    }

}