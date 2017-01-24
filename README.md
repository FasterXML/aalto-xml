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

## Documentation

* [Project Wiki](https://github.com/FasterXML/aalto-xml/wiki) (docs, downloads)
* [FasterXML Aalto Wiki](http://wiki.fasterxml.com/AaltoHome)
* [Aalto tutorial](http://www.studytrails.com/java/xml/aalto/java-xml-aalto-introduction.jsp) (by [StudyTrails](http://www.studytrails.com))
* Cowtown blog:
    * [Non-blocking XML parsing with Aalto 0.9.7](http://www.cowtowncoder.com/blog/archives/2011/03/entry_451.html) (note: minor changes to API since then)

## Usage

### Blocking XML parsing (Stax, SAX)

Blocking XML parsing is done using one of standard interfaces:

* Stax (javax.xml.stream) interface -- countless tutorials exist.
    * Also implements Stax2 extension (http://wiki.fasterxml.com/WoodstoxStax2)
    * StaxMate (http://wiki.fasterxml.com/StaxMateHome) is a good companion library for more convenient access

### Non-blocking ("async") XML parsing

Non-blocking parsing interface is extension of basic Stax (and Stax2) API, with extensions defined in 'com.fasterxml.aalto' package::

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
AsyncXMLStreamReader<AsyncByteArrayFeeder> parser = f.createAsyncFor(byteArray);
```

and more content is feed via `AsyncInputFeeder` when getting `EVENT_INCOMPLETE` via `parser.next()`:

```java
parser.getInputFeeder().feedInput(b, offset, dataLength);
```

or, if no more input available, indicate end-of-content with

```java
parser.getInputFeeder().endOfInput();
```

### Aalto Design goals

* Ultra-high performance parsing by making the Common Case Fast (similar to original RISC manifesto). This may mean limiting functionality, but never compromising correctness. XML 1.0 compliancy is not sacrificed for speed.
* Allow non-block, asynchronous parsing: it should be possible to "feed" more input and incrementally get more XML events out, without forcing the current thread to block on I/O read operation. 

### Dependency

Aalto dependency is usually added via Maven repository, so something like:

```xml
<dependency>
    <groupId>com.fasterxml</groupId>
    <artifactId>aalto-xml</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Related

* [Utility library based on Aalto](https://github.com/skjolber/async-stax-utils)
