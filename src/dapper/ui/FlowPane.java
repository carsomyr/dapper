/**
 * <p>
 * Copyright (c) 2008-2010 The Regents of the University of California<br>
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

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ContainerEvent;
import java.awt.event.ContainerListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.util.Observable;
import java.util.Observer;
import java.util.concurrent.BlockingQueue;

import javax.swing.JTabbedPane;
import javax.swing.Timer;

import shared.util.Control;
import shared.util.CoreThread;
import dapper.event.FlowEvent;
import dapper.server.Server;
import dapper.server.ServerProcessor.FlowBuildRequest;
import dapper.server.flow.Flow;
import dapper.server.flow.FlowStatus;

/**
 * A subclass of {@link JTabbedPane} that serves as an index of {@link FlowTab}s.
 * 
 * @apiviz.composedOf dapper.ui.CodeletTree
 * @apiviz.composedOf dapper.ui.FlowTab
 * @author Roy Liu
 */
@SuppressWarnings("serial")
public class FlowPane extends JTabbedPane implements Observer, ContainerListener, ActionListener, WindowListener {

    final Server server;

    int screenshotCounter;

    Timer timer;

    boolean removeFinished;
    boolean active;

    int flowFlags;

    /**
     * Default constructor.
     */
    public FlowPane(final Server server) {

        setBackground(Color.white);
        setForeground(Color.black);

        setTabLayoutPolicy(WRAP_TAB_LAYOUT);

        addContainerListener(this);

        this.screenshotCounter = 0;
        this.timer = null;
        this.removeFinished = false;
        this.active = true;
        this.flowFlags = FlowEvent.F_NONE;

        this.server = server;

        new CoreThread("Flow Event Logger") {

            @Override
            protected void runUnchecked() throws Exception {

                BlockingQueue<FlowEvent<Object, Object>> queue = server.createFlowEventQueue();

                for (FlowEvent<Object, Object> evt; (evt = queue.take()) != null;) {
                    FlowManager.Log.info(evt.toString(), evt.getError());
                }
            }

        }.start();
    }

    /**
     * Sets an option regulating whether {@link FlowTab}s corresponding to finished {@link Flow}s should be removed.
     */
    public void setRemoveFinished(boolean removeFinished) {
        this.removeFinished = removeFinished;
    }

    /**
     * Sets the idle client autoclose option.
     */
    public void setAutoCloseIdle(boolean autoCloseIdle) {

        try {

            this.server.setAutoCloseIdle(autoCloseIdle);

        } catch (Exception e) {

            FlowManager.getLog().info("Failed to set client auto-close option.", e);
        }
    }

    /**
     * Sets the bit vector of {@link FlowEvent} interest flags.
     */
    protected void setFlowFlags(int flowFlags) {
        this.flowFlags = flowFlags;
    }

    /**
     * Takes a screenshot of the underlying {@link Flow} of the current {@link FlowTab}.
     */
    public void takeScreenshot() {

        FlowTab ft = (FlowTab) getSelectedComponent();

        if (ft != null) {
            ft.renderSVG(new File(String.format("screenshot-%d.svg", this.screenshotCounter++)));
        }
    }

    /**
     * Purges the underlying {@link Flow} of the current {@link FlowTab}.
     */
    public void purgeCurrent() {

        FlowTab ft = (FlowTab) getSelectedComponent();

        if (ft != null) {
            remove(ft);
        }
    }

    /**
     * Observes {@link Flow} creation requests.
     */
    @Override
    public void update(Observable o, Object arg) {

        try {

            FlowBuildRequest fbr = (FlowBuildRequest) arg;
            addTab(fbr.flowBuilder.toString(), //
                    new FlowTab(this.server.createFlow(fbr.flowBuilder, fbr.classLoader, this.flowFlags)));

        } catch (Exception e) {

            FlowManager.getLog().info("Failed to create flow.", e);
        }
    }

    /**
     * Renders the underlying {@link Flow} of the current {@link FlowTab}.
     */
    @Override
    public void actionPerformed(ActionEvent e) {

        FlowTab ft = (FlowTab) getSelectedComponent();

        ft.renderImage();
        ft.repaint();

        // Force the containing scroll pane to adjust to the new image size accordingly.
        revalidate();

        FlowStatus flowStatus = ft.getFlowProxy().getFlow().getStatus();

        if (this.removeFinished && (flowStatus == FlowStatus.FINISHED || flowStatus == FlowStatus.FAILED)) {
            remove(ft);
        }
    }

    /**
     * Tries to start the rendering timer.
     */
    @Override
    public void componentAdded(ContainerEvent e) {
        tryStartTimer();
    }

    /**
     * Tries to stop the rendering timer. Purges the underlying {@link Flow} of the removed {@link FlowTab}.
     */
    @Override
    public void componentRemoved(ContainerEvent e) {

        tryStopTimer();

        Control.close((FlowTab) e.getChild());
    }

    /**
     * Tries to start the rendering timer.
     */
    @Override
    public void windowActivated(WindowEvent e) {

        this.active = true;
        tryStartTimer();
    }

    /**
     * Tries to stop the rendering timer.
     */
    @Override
    public void windowDeactivated(WindowEvent e) {

        this.active = false;
        tryStopTimer();
    }

    /**
     * Destroys the underlying {@link Server} instance.
     */
    @Override
    public void windowClosed(WindowEvent e) {
        Control.close(this.server);
    }

    /**
     * Tries to start the rendering timer.
     */
    protected void tryStartTimer() {

        // If no timer exists, start one.
        if (this.timer == null && !(getTabCount() == 0 || !this.active)) {

            this.timer = new Timer(0, this);
            this.timer.setInitialDelay(0);
            this.timer.setDelay(2500);
            this.timer.start();
        }
    }

    /**
     * Tries to stop the rendering timer.
     */
    protected void tryStopTimer() {

        // If no more tabs, stop the timer and null out its reference.
        if (this.timer != null && (getTabCount() == 0 || !this.active)) {

            this.timer.stop();
            this.timer = null;
        }
    }

    //

    /**
     * Does nothing.
     */
    @Override
    public void windowClosing(WindowEvent e) {
        // Do nothing.
    }

    /**
     * Does nothing.
     */
    @Override
    public void windowDeiconified(WindowEvent e) {
        // Do nothing.
    }

    /**
     * Does nothing.
     */
    @Override
    public void windowIconified(WindowEvent e) {
        // Do nothing.
    }

    /**
     * Does nothing.
     */
    @Override
    public void windowOpened(WindowEvent e) {
        // Do nothing.
    }
}
