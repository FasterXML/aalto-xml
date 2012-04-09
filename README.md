# Overview

Aalto XML processor is an ultra-high performance next generation Stax XML processor implementation. It also implements SAX2 API.

Additionally Aalto implements a non-blocking (asynchronous) Stax parser; non-blocking API is a minimalistic extension above Stax/Stax2 API to allow indication of "not yet available" token (EVENT_INCOMPLETE) as well as feeding of input (since InputStream can not be used as it blocks)

Aalto is licensed under [Apache License 2.0](http://www.apache.org/licenses/LICENSE-2.0.txt)

## Documentation

* [Project Wiki](https://github.com/FasterXML/aalto-xml/wiki) (docs, downloads)
* [FasterXML Aalto Wiki](http://wiki.fasterxml.com/AaltoHome)

## Usage

### Blocking XML parsing (Stax, SAX)

Blocking XML parsing is done using one of standard interfaces:

* Stax (javax.xml.stream) interface -- countless tutorials exist.
 * Also implements Stax2 extension (http://wiki.fasterxml.com/WoodstoxStax2)
 * StaxMate (http://wiki.fasterxml.com/StaxMateHome) is a good companion library for more convenient access

### Non-blocking ("async") XML parsing

Non-blocking parsing interface is extension of basic Stax (and Stax2) API, with extensions defined in 'com.fasterxml.aalto' package::

* AsyncXMLInputFactory offers factory methods for creating non-blocking parsers
* AsyncXMLStreamReader is extended type that non-blocking parsers implement
 * AsyncXMLStreamReader.EVENT_INCOMPLETE (value 257; just outside range reserved by Stax API) is used to denote "not yet available" (without more data)
 * Method "getInputFeeder()" is used to access object of type 'AsyncInputFeeder' used to feed input non-blocking way
* AsyncInputFeeder contains methods for feeding input.

Typical usage pattern is one where block of input is fed to parser, and zero or more complete events are read using basic 'XMLStreamReader.next()' method; and once 'EVENT_INCOMPLETE' is returned, more input needs to be given. AsyncXMLStreamReader itself does not buffer input beyond a single block; caller is responsible for additional buffering, if any.

### Aalto Design goals

* Ultra-high performance parsing by making the Common Case Fast (similar to original RISC manifesto). This may mean limiting functionality, but never compromising correctness. XML 1.0 compliancy is not sacrificed for speed.
* Allow non-block, asynchronous parsing: it should be possible to "feed" more input and incrementally get more XML events out, without forcing the current thread to block on I/O read operation. 


