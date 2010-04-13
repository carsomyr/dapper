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

package dapper.client;

import shared.log.Logging;
import shared.metaclass.Loader;
import shared.metaclass.Loader.EntryPoint;
import shared.metaclass.Loader.LoadableResources;

/**
 * A driver class for the Dapper client.
 * 
 * @apiviz.owns dapper.client.Client
 * @author Roy Liu
 */
@LoadableResources(resources = {
//
        "jar:lib.commons-cli", //
        "jar:lib.commons-codec", //
        "jar:lib.log4j", //
        "jar:lib.slf4j-api", //
        "jar:lib.slf4j-log4j12", //
        //
        "jar:lib.sst-base", //
        "jar:lib.sst-commons", //
        "jar:lib.sst-net" //
}, //
//
packages = {
//
"dapper" //
})
public class ClientDriver {

    /**
     * The program main method that delegates to {@link Loader#run(String, Object)}.
     * 
     * @throws Exception
     *             when something goes awry.
     */
    public static void main(String[] args) throws Exception {
        Loader.run(ClientDriver.class.getName(), args);
    }

    /**
     * The program entry point.
     * 
     * @throws Exception
     *             when something goes awry.
     */
    @EntryPoint
    public static void entryPoint(String[] args) throws Exception {

        Logging.configureLog4J("shared/log4j.xml");
        Logging.configureLog4J("shared/net/log4j.xml");
        Logging.configureLog4J("dapper/client/log4j.xml");

        Client.createClient(args);
    }

    // Dummy constructor.
    ClientDriver() {
    }
}
