/**
 * Copyright (c) 2018-2019 Siemens AG
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1) Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2) Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package at.ac.tuwien.kr.alpha.solver;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Offers methods to estimate the effect of propagating binary nogoods.
 */
public interface BinaryNoGoodPropagationEstimation {

	/**
	 * Uses {@code strategy} to estimate the number of direct consequences of propagating binary nogoods after assigning
	 * {@code truth} to {@code atom}.
	 *
	 * If {@code strategy} is {@link Strategy#BinaryNoGoodPropagation}, {@code truth} is assigned to {@code atom},
	 * only binary nogoods are propagated, a backtrack is executed, and the number of atoms that have been assigned
	 * additionally during this process is returned.
	 *
	 * If {@code strategy} is {@link Strategy#CountBinaryWatches}, on the other hand, the number of binary watches on
	 * the literal given by {@code atom} and {@code truth} is returned.
	 *
	 * @param atom the atom to estimate effects for
	 * @param truth gives, together with {@code atom}, a literal to estimate effects for
	 * @param strategy the strategy to use for estimation. If {@link Strategy#BinaryNoGoodPropagation} is given but
	 *                 no binary nogoods exist, {@link Strategy#CountBinaryWatches} will be used instead.
	 * @return an estimate on the effects of propagating binary nogoods after assigning {@code truth} to {@code atom}.
	 */
	int estimate(int atom, boolean truth, Strategy strategy);

	/**
	 * Strategies to estimate the amount of influence of a literal.
	 */
	public enum Strategy {
		/**
		 * Counts binary watches involving the literal under consideration
		 */
		CountBinaryWatches,

		/**
		 * Assigns true to the literal under consideration, then does propagation only on binary nogoods
		 * and counts how many other atoms are assigned during this process, then backtracks
		 */
		BinaryNoGoodPropagation;

		/**
		 * @return a comma-separated list of names of known heuristics
		 */
		public static String listAllowedValues() {
			return Arrays.stream(values()).map(Strategy::toString).collect(Collectors.joining(", "));
		}
	}

}
