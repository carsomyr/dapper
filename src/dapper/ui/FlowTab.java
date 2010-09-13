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
    final protected static String[] DotExecArgsPNG;

    /**
     * An array of operating system dependent arguments to execute Dot with SVG export.
     */
    final protected static String[] DotExecArgsSVG;

    static {

        DotExecArgsPNG = new String[] { "dot", "-T", "png" };
        DotExecArgsSVG = new String[] { "dot", "-T", "svg" };
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
                    DotExecArgsPNG);

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
                    DotExecArgsSVG);

            Control.transfer(new ByteArrayInputStream(out.toByteArray()), f);

        } catch (Exception e) {

            FlowManager.getLog().info("Failed to execute Dot.", e);
        }
    }

    /**
     * Purges the underlying {@link Flow}.
     */
    @Override
    public void close() {

        try {

            this.fp.purge();

        } catch (Exception e) {

            // Ah well.
        }
    }
}
