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

public class AsyncUtfByteBufferScanner extends AsyncUtfScanner {

    /**
     * This buffer is actually provided by caller
     */
    private ByteBuffer _inputBuffer;

    public AsyncUtfByteBufferScanner(ReaderConfig cfg) {
        super(cfg);
    }

    @Override
    protected void setInput(byte[] buf) {
        _inputBuffer = ByteBuffer.wrap(buf);
    }


    @Override
    protected void setInput(ByteBuffer buf) {
        _inputBuffer = buf;
    }

    @Override
    protected byte byteAt(int index) {
        return _inputBuffer.get(index);
    }
}
