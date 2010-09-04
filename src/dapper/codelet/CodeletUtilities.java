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

package dapper.codelet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import shared.util.Control;

/**
 * A static utility class for {@link Codelet} execution.
 * 
 * @apiviz.owns dapper.codelet.DataService
 * @author Roy Liu
 */
abstract public class CodeletUtilities {

    /**
     * A global request counter for {@link #createStem()}.
     */
    final protected static AtomicInteger RequestCounter = new AtomicInteger();

    /**
     * A local thread variable for storing the {@link DataService} available to the {@link Codelet} being executed.
     */
    final protected static ThreadLocal<DataService> DSLocal = new ThreadLocal<DataService>();

    /**
     * Requests a stem from the server that is unique over the server's lifetime.
     */
    final public static String createStem() {
        return new String(DSLocal.get().getData(String.format("id:%08x", RequestCounter.getAndIncrement())));
    }

    /**
     * Sets the local thread's {@link DataService}.
     */
    final public static void setDataService(DataService ds) {
        DSLocal.set(ds);
    }

    /**
     * Groups a list of {@link Nameable} elements by their names.
     */
    final public static <T extends Nameable> Map<String, List<T>> groupByName(List<T> elts) {

        Map<String, List<T>> eltsMap = new HashMap<String, List<T>>();

        for (T elt : elts) {

            String name = elt.getName();
            List<T> acc = eltsMap.get(name);

            if (acc == null) {

                acc = new ArrayList<T>();
                eltsMap.put(name, acc);
            }

            acc.add(elt);
        }

        return eltsMap;
    }

    /**
     * Filters a list of {@link Nameable} elements by name and superclass.
     */
    final public static <S extends Nameable, T extends S> List<T> filter(List<S> elts, String name, Class<T> clazz) {

        List<T> eltsList = new ArrayList<T>();

        for (S elt : elts) {

            if (elt.getName().equals(name) && clazz.isInstance(elt)) {
                eltsList.add(clazz.cast(elt));
            }
        }

        return eltsList;
    }

    /**
     * Filters a list of {@link Nameable} elements by superclass.
     */
    final public static <S extends Nameable, T extends S> List<T> filter(List<S> elts, Class<T> clazz) {

        List<T> eltsList = new ArrayList<T>();

        for (S elt : elts) {

            if (clazz.isInstance(elt)) {
                eltsList.add(clazz.cast(elt));
            }
        }

        return eltsList;
    }

    /**
     * Filters a list of {@link Nameable} elements by name.
     */
    final public static <T extends Nameable> List<T> filter(List<T> elts, String name) {

        List<T> eltsList = new ArrayList<T>();

        for (T elt : elts) {

            if (elt.getName().equals(name)) {
                eltsList.add(elt);
            }
        }

        return eltsList;
    }

    /**
     * Creates a DOM {@link Element} sandwiched by "&lt;parameters&gt;&lt;/parameters&gt;" tags whose internals are
     * given by a string.
     */
    final public static Element createElement(String content) {
        return Control.createDocument(new StringBuilder() //
                .append("<parameters>") //
                .append(content) //
                .append("</parameters>").toString().getBytes()).getDocumentElement();
    }

    // Dummy constructor.
    CodeletUtilities() {
    }
}
