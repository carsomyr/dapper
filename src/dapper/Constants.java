/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2008 The Regents of the University of California <br />
 * <br />
 * This library is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General
 * Public License as published by the Free Software Foundation, either version 2.1 of the License, or (at your option)
 * any later version. <br />
 * <br />
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details. <br />
 * <br />
 * You should have received a copy of the GNU Lesser General Public License along with this library. If not, see <a
 * href="http://www.gnu.org/licenses/">http://www.gnu.org/licenses/</a>.
 */

package dapper;

import java.net.ServerSocket;
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
     * The server's listening port.
     */
    final public static int PORT = 10101;

    /**
     * The size of {@link ServerSocket} backlogs.
     */
    final public static int BACKLOG = 128;

    /**
     * The maximum allowable control message size.
     */
    final public static int MAXIMUM_CONTROL_MESSAGE_SIZE = 1 << 26;

    /**
     * The transfer buffer size.
     */
    final public static int BUFFER_SIZE = 1 << 20;

    /**
     * The request timeout.
     */
    final public static long TIMEOUT = 10000;

    /**
     * The client timeout.
     */
    final public static long CLIENT_TIMEOUT = 60000;

    /**
     * The maximum allowable number of pending accepts.
     */
    final public static int MAX_PENDING_ACCEPTS = 16;

    /**
     * The default {@link Codelet} execution time limit.
     */
    final public static long CODELET_TIMEOUT = 86400000;

    /**
     * The default {@link Codelet} failed execution retry limit.
     */
    final public static int CODELET_RETRIES = 8;

    /**
     * A {@link Pattern} specification that exactly matches the string "local".
     */
    final public static String LOCAL = "^local$";

    /**
     * A {@link Pattern} specification that exactly matches the string "remote".
     */
    final public static String REMOTE = "^remote$";

    /**
     * The {@link Timer#purge()} interval in milliseconds.
     */
    final public static long TIMER_PURGE_INTERVAL = 120000;

    /**
     * The maximum allowable size of the server's internal event queues.
     */
    final public static int INTERNAL_EVENT_BACKLOG = 128;

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
