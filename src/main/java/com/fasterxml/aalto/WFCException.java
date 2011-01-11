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

package com.fasterxml.aalto;

import javax.xml.stream.Location;

import com.fasterxml.aalto.impl.StreamExceptionBase;

/**
 * Base class for reader-side Well-Formedness Constraint violation
 * (fatal error) exceptions.
 */
@SuppressWarnings("serial")
public class WFCException
    extends StreamExceptionBase
{
    public WFCException(String msg, Location loc)
    {
        super(msg, loc);
    }
}
