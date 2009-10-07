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

package dapper.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import shared.util.Control;

/**
 * A utility class built on {@link BipartiteMatcher} that matches {@link Requirement}s to satisfiers.
 * 
 * @apiviz.owns dapper.util.BipartiteMatcher
 * @apiviz.has dapper.util.Requirement - - - argument
 * @author Roy Liu
 */
public class RequirementsMatcher {

    /**
     * Matches {@link Requirement}s to satisfiers.
     * 
     * @param <R>
     *            the {@link Requirement} type.
     * @param <S>
     *            the satisfier type.
     */
    final public static <R extends Requirement<S>, S> Set<Entry<R, S>> match( //
            Set<R> requirementsSet, Set<S> satisfiersSet) {

        // Copy the set of satisfiers, which we'll modify.
        satisfiersSet = new HashSet<S>(satisfiersSet);

        Control.checkTrue(satisfiersSet.size() >= requirementsSet.size(), //
                "The number of satisfiers must be greater than or equal to the number of requirements");

        ArrayList<S> satisfiers = new ArrayList<S>(satisfiersSet);

        ArrayList<R> requirements = new ArrayList<R>();
        ArrayList<R> trivialRequirements = new ArrayList<R>();

        // Consider only nontrivial requirements for matching.
        for (R requirement : requirementsSet) {
            (!requirement.isTrivial() ? requirements : trivialRequirements).add(requirement);
        }

        Collections.shuffle(requirements);
        Collections.shuffle(trivialRequirements);

        int sSize = satisfiers.size();
        int rSize = requirements.size();

        BipartiteMatcher bpm = new BipartiteMatcher(Math.max(sSize, rSize));

        for (int i = 0; i < sSize; i++) {

            for (int j = 0; j < rSize; j++) {

                // Assign a unit edge weight if satisfied.
                if (requirements.get(j).isSatisfied(satisfiers.get(i))) {
                    bpm.setWeight(i, j, 1.0);
                }
            }
        }

        Map<R, S> res = new HashMap<R, S>();

        int[] matching = bpm.getMatching();

        loop: for (int i = 0; i < sSize; i++) {

            // Skip matches to dummy vertices.
            if (matching[i] >= rSize) {
                continue loop;
            }

            if (requirements.get(matching[i]).isSatisfied(satisfiers.get(i))) {

                res.put(requirements.get(matching[i]), satisfiers.get(i));
                satisfiersSet.remove(satisfiers.get(i));
            }
        }

        ArrayList<S> remainingSatisfiers = new ArrayList<S>(satisfiersSet);

        // Arbitrarily match up satisfiers to trivial requirements.
        for (int i = 0, n = trivialRequirements.size(); i < n; i++) {
            res.put(trivialRequirements.get(i), remainingSatisfiers.get(i));
        }

        return res.entrySet();
    }

    /**
     * Dummy constructor.
     */
    RequirementsMatcher() {
    }
}
