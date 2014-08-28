/* Aalto XML processor
 *
 * Copyright (c) 2014- Norman Maurer, norman.maurer@googlemail.com
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
package com.fasterxml.aalto.async;

import com.fasterxml.aalto.in.ReaderConfig;

import java.nio.ByteBuffer;

public class AsyncUtfArrayScanner extends AsyncUtfScanner {

    /**
     * This buffer is actually provided by caller
     */
    private byte[] _inputBuffer;

    public AsyncUtfArrayScanner(ReaderConfig cfg) {
        super(cfg);
    }

    @Override
    protected void setInput(byte[] buf) {
        _inputBuffer = buf;
    }

    @Override
    protected void setInput(ByteBuffer buf) {
        if (buf.hasArray()) {
            // backed by an array so we can just make direct use of it without any memory copies.
            _inputBuffer = buf.array();
            int offset = buf.arrayOffset();
            if (offset > 0) {
                // Need to adjust ptr by arrayOffset
                _inputPtr += offset;
                _inputEnd += offset;
            }
        } else {
            // Copy content into new byte[]
            _inputBuffer = new byte[buf.remaining()];
            buf.get(_inputBuffer);
        }
    }

    @Override
    protected byte byteAt(int index) {
        return _inputBuffer[index];
    }
}
