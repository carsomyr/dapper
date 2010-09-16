/**
 * <p>
 * Copyright (c) 2008 The Regents of the University of California<br>
 * All rights reserved.
 * </p>
 * <p>
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 * </p>
 * <ul>
 * <li>Redistributions of source code must retain the above copyright notice, this list of conditions and the following
 * disclaimer.</li>
 * <li>Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 * following disclaimer in the documentation and/or other materials provided with the distribution.</li>
 * <li>Neither the name of the author nor the names of any contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.</li>
 * </ul>
 * <p>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * </p>
 */

package dapper;

import java.util.Timer;
import java.util.regex.Pattern;

import dapper.codelet.Codelet;

/**
 * Contains Dapper constant values.
 * 
 * @author Roy Liu
 */
public class Constants {

    /**
     * The default server listening port.
     */
    final public static int DEFAULT_SERVER_PORT = 10101;

    /**
     * The maximum allowable control message size.
     */
    final public static int MAX_CONTROL_MESSAGE_SIZE = 1 << 26;

    /**
     * The default transfer buffer size.
     */
    final public static int DEFAULT_BUFFER_SIZE = 1 << 20;

    /**
     * The request timeout in milliseconds.
     */
    final public static long REQUEST_TIMEOUT_MILLIS = 10000;

    /**
     * The client timeout in milliseconds.
     */
    final public static long CLIENT_TIMEOUT_MILLIS = 60000;

    /**
     * The maximum allowable number of pending accepts.
     */
    final public static int MAX_PENDING_ACCEPTS = 16;

    /**
     * The {@link Codelet} execution time limit in milliseconds.
     */
    final public static long CODELET_TIMEOUT_MILLIS = 86400000;

    /**
     * The maximum allowable number of {@link Codelet} execution retries.
     */
    final public static int MAX_CODELET_RETRIES = 8;

    /**
     * A domain {@link Pattern} that exactly matches the string "local".
     */
    final public static String LOCAL = "^local$";

    /**
     * A domain {@link Pattern} that exactly matches the string "remote".
     */
    final public static String REMOTE = "^remote$";

    /**
     * The {@link Timer#purge()} interval in milliseconds.
     */
    final public static long TIMER_PURGE_INTERVAL_MILLIS = 120000;

    /**
     * The maximum allowable size of the server's internal event queues.
     */
    final public static int MAX_INTERNAL_QUEUE_SIZE = 128;

    // Declare some colors.

    /**
     * A dark orange color.
     */
    final public static String DARK_ORANGE = "FF8C00";

    /**
     * A dark blue color.
     */
    final public static String DARK_BLUE = "00008B";

    /**
     * A dark red color.
     */
    final public static String DARK_RED = "8B0000";

    /**
     * A dark green color.
     */
    final public static String DARK_GREEN = "228B22";

    /**
     * A light blue color.
     */
    final public static String LIGHT_BLUE = "ADD8E6";

    /**
     * A light gray color.
     */
    final public static String LIGHT_GRAY = "D3D3D3";

    /**
     * A gray color.
     */
    final public static String GRAY = "808080";

    /**
     * The black color.
     */
    final public static String BLACK = "000000";

    // Dummy constructor.
    Constants() {
    }
}
