# Overview

Aalto XML processor is an ultra-high performance next generation Stax XML processor implementation, implementing both
basic Stax API (`javax.xml.stream`) and Stax2 API extension (`org.codehaus.woodstox.stax2`).
In addition, it also implements SAX2 API.

In additional to standard Java XML interfaces, one unique feature not implemented by any other Java XML parser
that we are aware is so-called non-blocking (asynchronous) XML parsing: ability to parse XML without using
blocking I/O, necessary for fully asynchronous processing such as those with Akka framework.
Aalto non-blocking API is a minimalistic extension above Stax/Stax2 API to allow indication of "not yet available" token (EVENT_INCOMPLETE) as well as feeding of input (since InputStream can not be used as it blocks)

Aalto is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

## Status

[![Build Status](https://travis-ci.org/FasterXML/aalto-xml.svg)](https://travis-ci.org/FasterXML/aalto-xml)
[![Javadoc](https://javadoc-emblem.rhcloud.com/doc/com.fasterxml/aalto-xml/badge.svg)](http://www.javadoc.io/doc/com.fasterxml/aalto-xml)
[![Tidelift](https://tidelift.com/badges/package/maven/com.fasterxml:aalto-xml)](https://tidelift.com/subscription/pkg/maven-com-fasterxml-aalto-xml?utm_source=maven-com-fasterxml-aalto-xml&utm_medium=referral&utm_campaign=readme)

## Support

There are 2 types of support available:

* Community support via mailing lists: [aalto-xml-interest](aalto-xml-interest@yahoogroups.com)
* Optional Commercial Support is available via [Tidelift Subscriptions](https://tidelift.com/subscription/pkg/maven-com-fasterxml-aalto-xml?utm_source=maven-com-fasterxml-aalto-xml&utm_medium=referral&utm_campaign=readme)

## Contributing

For simple bug reports and fixes, and feature requests, please simply use projects
[Issue Tracker](../../issues), with exception of security-related issues for which
we recommend filing a
[Tidelift security contact](https://tidelift.com/security) (NOTE: you do NOT have to be
a subscriber to do this).

## Documentation

* [Project Wiki](https://github.com/FasterXML/aalto-xml/wiki) (docs, downloads)
* [Aalto tutorial](https://www.studytrails.com/2016/09/12/java-xml-aalto-introduction/) (by [StudyTrails](http://www.studytrails.com))
* Cowtown blog:
    * [Non-blocking XML parsing with Aalto 0.9.7](http://www.cowtowncoder.com/blog/archives/2011/03/entry_451.html) (note: minor changes to API since then)

## JDK Compatibility

Aalto 1.x:

* Can be _used_ on JDK versions 6 (1.6) and up
    * needs JDK 8 or higher to _build_ as of Aalto `1.2.0`
* Contains Java 9 Module definition (`module-info.class`) starting with version `1.2.0`

## Usage

### Dependency

Aalto dependency is usually added via Maven repository, so something like:

```xml
<dependency>
    <groupId>com.fasterxml</groupId>
    <artifactId>aalto-xml</artifactId>
    <version>1.3.1</version>
</dependency>
```

### Blocking XML parsing (Stax, SAX)

Blocking XML parsing is done using one of standard interfaces:

* Stax (javax.xml.stream) interface -- countless tutorials exist.
    * Also implements [Stax2](../../../stax2-api)  extension
    * [StaxMate](../../../StaxMate) is a good companion library for more convenient access

### Non-blocking ("async") XML parsing

Non-blocking parsing interface is extension of basic Stax (and Stax2) API, with extensions defined in 'com.fasterxml.aalto' package:

* `AsyncXMLInputFactory` offers factory methods for creating non-blocking parsers
* `AsyncXMLStreamReader` is extended type that non-blocking parsers implement
    * `AsyncXMLStreamReader.EVENT_INCOMPLETE` (value 257; just outside range reserved by Stax API) is used to denote "not yet available" (without more data)
    * Method `getInputFeeder()` is used to access object of type 'AsyncInputFeeder' used to feed input non-blocking way
* `AsyncInputFeeder` contains methods for feeding input.

Typical usage pattern is one where block of input is fed to parser, and zero or more complete events are read using basic 'XMLStreamReader.next()' method; and once 'EVENT_INCOMPLETE' is returned, more input needs to be given. AsyncXMLStreamReader itself does not buffer input beyond a single block; caller is responsible for additional buffering, if any.
See [Async parsing](Code-sample:-Async-parsing) for details.

Construction of `AsyncXMLInputFactory` is simple; instance may be constructed with or without initial content to parse:

```java
AsyncXMLInputFactory f = new InputFactoryImpl();
// IF there is content use this:
AsyncXMLStreamReader<AsyncByteArrayFeeder> parser = f.createAsyncFor(byteArray);
// If NO CONTENT yet available, just use this:
AsyncXMLStreamReader<AsyncByteArrayFeeder> parser = f.createAsyncForByteArray();
```

and more content is feed via `AsyncInputFeeder` when getting `EVENT_INCOMPLETE` via `parser.next()`:

```java
parser.getInputFeeder().feedInput(b, offset, dataLength);
```

or, if no more input available, indicate end-of-content with

```java
parser.getInputFeeder().endOfInput();
```

See the [Full non-blocking parsing example](../../wiki/Code-sample:-Async-parsing) on Wiki for more details.

### Aalto Design goals

* Ultra-high performance parsing by making the Common Case Fast (similar to original RISC manifesto). This may mean limiting functionality, but never compromising correctness. XML 1.0 compliancy is not sacrificed for speed.
* Allow non-block, asynchronous parsing: it should be possible to "feed" more input and incrementally get more XML events out, without forcing the current thread to block on I/O read operation. 

### Related

* [Utility library based on Aalto](https://github.com/skjolber/async-stax-utils)
* [Tool for obfuscating XML (uses Aalto for parsing)](https://github.com/adamretter/duplicitous)
