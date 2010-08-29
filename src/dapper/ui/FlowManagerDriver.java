/**
 * <p>
 * Copyright (C) 2008 The Regents of the University of California<br />
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

package dapper.ui;

import javax.swing.SwingUtilities;

import shared.log.Logging;
import shared.metaclass.Loader;
import shared.metaclass.Loader.EntryPoint;
import shared.metaclass.Loader.LoadableResources;

/**
 * A driver class for {@link FlowManager}.
 * 
 * @apiviz.owns dapper.ui.FlowManager
 * @author Roy Liu
 */
@LoadableResources(resources = {
//
        "jar:lib.collections-generic", //
        "jar:lib.colt", //
        "jar:lib.commons-cli", //
        "jar:lib.commons-codec", //
        "jar:lib.concurrent", //
        "jar:lib.jung-algorithms", //
        "jar:lib.jung-api", //
        "jar:lib.jung-graph-impl", //
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
public class FlowManagerDriver {

    /**
     * The program main method that delegates to {@link Loader#run(String, Object)}.
     * 
     * @throws Exception
     *             when something goes awry.
     */
    public static void main(String[] args) throws Exception {
        Loader.run(FlowManagerDriver.class.getName(), args);
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
        Logging.configureLog4J("dapper/server/log4j.xml");

        SwingUtilities.invokeAndWait(new Runnable() {

            @Override
            public void run() {

                try {

                    FlowManager.createUI(args);

                } catch (Exception e) {

                    throw new RuntimeException(e);
                }
            }
        });
    }

    // Dummy constructor.
    FlowManagerDriver() {
    }
}
