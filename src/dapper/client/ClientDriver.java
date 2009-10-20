/**
 * This file is part of Dapper, the Distributed and Parallel Program Execution Runtime ("this library"). <br />
 * <br />
 * Copyright (C) 2008 Roy Liu, The Regents of the University of California <br />
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
import shared.util.LoadableResources;
import shared.util.Loader;
import shared.util.LoadableResources.Resource;
import shared.util.LoadableResources.ResourceType;
import shared.util.Loader.EntryPoint;

/**
 * A driver class for the Dapper client.
 * 
 * @apiviz.owns dapper.client.Client
 * @author Roy Liu
 */
@LoadableResources(resources = {
//
        @Resource(type = ResourceType.JAR, path = "lib", name = "commons-cli"), //
        @Resource(type = ResourceType.JAR, path = "lib", name = "commons-codec"), //
        @Resource(type = ResourceType.JAR, path = "lib", name = "log4j"), //
        @Resource(type = ResourceType.JAR, path = "lib", name = "slf4j-api"), //
        @Resource(type = ResourceType.JAR, path = "lib", name = "slf4j-log4j12"), //
        //
        @Resource(type = ResourceType.JAR, path = "lib", name = "sst-base"), //
        @Resource(type = ResourceType.JAR, path = "lib", name = "sst-commons"), //
        @Resource(type = ResourceType.JAR, path = "lib", name = "sst-net") //
}, //
//
packages = {
//
"dapper" //
})
public class ClientDriver {

    /**
     * The program main method that delegates to {@link Loader#start(String, Object)}.
     * 
     * @throws Exception
     *             when something goes awry.
     */
    public static void main(final String[] args) throws Exception {
        Loader.start(ClientDriver.class.getName(), args);
    }

    /**
     * The program entry point.
     * 
     * @throws Exception
     *             when something goes awry.
     */
    @EntryPoint
    public static void entryPoint(final String[] args) throws Exception {

        Logging.configureLog4J("shared/log4j.xml");
        Logging.configureLog4J("shared/net/log4j.xml");
        Logging.configureLog4J("dapper/client/log4j.xml");

        Client.createClient(args);
    }

    // Dummy constructor.
    ClientDriver() {
    }
}