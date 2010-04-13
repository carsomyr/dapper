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

package dapper.codelet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Element;

import shared.util.Control;

/**
 * A class of convenient static methods for {@link Codelet} execution.
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
