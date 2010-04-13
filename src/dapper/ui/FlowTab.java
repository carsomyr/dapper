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

package dapper.ui;

import static shared.util.Control.NullOutputStream;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import shared.util.Control;
import dapper.server.ServerProcessor.FlowProxy;
import dapper.server.flow.Flow;

/**
 * A subclass of {@link JLabel} associated with {@link Flow} information.
 * 
 * @author Roy Liu
 */
@SuppressWarnings("serial")
public class FlowTab extends JLabel implements Closeable {

    /**
     * An array of operating system dependent arguments to execute Dot with PNG export.
     */
    final protected static String[] DotExecArgs_PNG;

    /**
     * An array of operating system dependent arguments to execute Dot with SVG export.
     */
    final protected static String[] DotExecArgs_SVG;

    static {

        boolean isWindows = System.getProperty("os.name").contains("Windows");

        DotExecArgs_PNG = isWindows ? new String[] { "cmd", "/C", "dot", "-Tpng" } //
                : new String[] { "dot", "-Tpng" };

        DotExecArgs_SVG = isWindows ? new String[] { "cmd", "/C", "dot", "-Tsvg" } //
                : new String[] { "dot", "-Tsvg" };
    }

    final FlowProxy fp;

    /**
     * Default constructor.
     */
    public FlowTab(FlowProxy fp) {
        super(new ImageIcon());

        this.fp = fp;
    }

    /**
     * Gets the {@link FlowProxy}.
     */
    protected FlowProxy getFlowProxy() {
        return this.fp;
    }

    /**
     * Renders the {@link Flow} as a {@link BufferedImage}.
     */
    protected void renderImage() {

        try {

            this.fp.refresh();

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Control.execAndWaitFor(new ByteArrayInputStream(this.fp.toString().getBytes()), //
                    out, NullOutputStream, //
                    DotExecArgs_PNG);

            ((ImageIcon) getIcon()).setImage(ImageIO.read(new ByteArrayInputStream(out.toByteArray())));

        } catch (Exception e) {

            FlowManager.getLog().info("Failed to execute Dot.", e);
        }
    }

    /**
     * Renders the {@link Flow} as an SVG.
     */
    protected void renderSVG(File f) {

        try {

            this.fp.refresh();

            ByteArrayOutputStream out = new ByteArrayOutputStream();

            Control.execAndWaitFor(new ByteArrayInputStream(this.fp.toString().getBytes()), //
                    out, NullOutputStream, //
                    DotExecArgs_SVG);

            Control.transfer(new ByteArrayInputStream(out.toByteArray()), f);

        } catch (Exception e) {

            FlowManager.getLog().info("Failed to execute Dot.", e);
        }
    }

    /**
     * Purges the underlying {@link Flow}.
     */
    public void close() {

        try {

            this.fp.purge();

        } catch (Exception e) {

            // Ah well.
        }
    }
}
