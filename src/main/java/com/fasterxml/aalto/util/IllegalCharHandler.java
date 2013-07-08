package com.fasterxml.aalto.util;

import com.fasterxml.aalto.WFCException;

public interface IllegalCharHandler {

	char convertIllegalChar(int invalidChar) throws WFCException;
	
	
	
	public static class ReplacingIllegalCharHandler implements IllegalCharHandler, XmlConsts {

		private final char replacedChar;
		
		public ReplacingIllegalCharHandler(final char replacedChar) {
			this.replacedChar = replacedChar;
		}
		
		
		@Override
		public char convertIllegalChar(int invalidChar) throws WFCException {
			return replacedChar;
		}
		
	}
}
