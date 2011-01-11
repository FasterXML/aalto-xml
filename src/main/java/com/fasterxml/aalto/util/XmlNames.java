/* Woodstox Lite ("wool") XML processor
 *
 * Copyright (c) 2006- Tatu Saloranta, tatu.saloranta@iki.fi
 *
 * Licensed under the License specified in the file LICENSE which is
 * included with the source code.
 * You may not use this file except in compliance with the License.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fasterxml.aalto.util;


/**
 * Simple utility class used for checking validity of xml names.
 */
public final class XmlNames
{
    /**
     * Method that can be used to verify whether given String is
     * a valid xml name or not.
     *
     * @return Index of the first character in given String that is
     *   not a valid xml name character, if any; -1 if string is
     *   a valid xml name
     */
    public static int findIllegalNameChar(String name, boolean xml11)
    {
        int ptr = 0;
        char c = name.charAt(ptr);
        int len = name.length();
        if (c < 0xD800 || c >= 0xE000) {
            if (xml11) {
                if (!XmlChars.is11NameStartChar(c)) {
                    return ptr;
                }
            } else {
                if (!XmlChars.is10NameStartChar(c)) {
                    return ptr;
                }
            }
        } else {
            if (len < 2) {
                return ptr;
            }
            ++ptr;
            // Only returns if ok; throws exception otherwise
            if (!validSurrogateNameChar(c, name.charAt(ptr))) {
                return ptr;
            }
        }
        ++ptr;

        if (xml11) {
            for (; ptr < len; ++ptr) {
                c = name.charAt(ptr);
                if (c < 0xD800 || c >= 0xE000) {
                    if (!XmlChars.is11NameChar(c)) {
                        return ptr;
                    }
                } else {
                    if ((ptr+1) >= len) { // unpaired surrogate
                        return ptr;
                    }
                    if (!validSurrogateNameChar(c, name.charAt(ptr+1))) {
                        return ptr;
                    }
                    ++ptr;
                }
            }
        } else {
            for (; ptr < len; ++ptr) {
                c = name.charAt(ptr);
                if (c < 0xD800 || c >= 0xE000) {
                    if (!XmlChars.is10NameChar(c)) {
                        return ptr;
                    }
                } else {
                    if ((ptr+1) >= len) { // unpaired surrogate
                        return ptr;
                    }
                    if (!validSurrogateNameChar(c, name.charAt(ptr+1))) {
                        return ptr;
                    }
                    ++ptr;
                }
            }
        }
        return -1;
    }

    private static boolean validSurrogateNameChar(char firstChar, char sec)
    {
        if (firstChar >= 0xDC00) {
            return false;
        }
        if (sec < 0xDC00 || sec >= 0xE000) {
            return false;
        }
        // And the composite, is it ok?
        int val = ((firstChar - 0xD800) << 10) + 0x10000;
        if (val > XmlConsts.MAX_UNICODE_CHAR) {
            return false;
        }
        // !!! TODO: xml 1.1 vs 1.0 rules: none valid for 1.0, many for 1.1
        return false;

        /*
        if (true) {
            return false;
        }
        return true;
        */
    }

}
