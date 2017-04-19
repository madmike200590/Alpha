/**
 * Copyright (c) 2017 Siemens AG
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
package at.ac.tuwien.kr.alpha.solver.heuristics.activity;

import org.apache.commons.collections4.MultiValuedMap;

import java.util.Map;

public final class BodyActivityProviderFactory {

	public enum BodyActivityType {
		DEFAULT, SUM, AVG, MAX, MIN
	}

	public static BodyActivityProvider getInstance(BodyActivityType type, MultiValuedMap<Integer, Integer> bodyToLiterals, Map<Integer, Double> activityCounters,
			double defaultActivity) {
		switch (type) {
		case DEFAULT:
			return new DefaultBodyActivityProvider(bodyToLiterals, activityCounters, defaultActivity);
		case SUM:
			return new SumBodyActivityProvider(bodyToLiterals, activityCounters, defaultActivity);
		case AVG:
			return new AvgBodyActivityProvider(bodyToLiterals, activityCounters, defaultActivity);
		case MAX:
			return new MaxBodyActivityProvider(bodyToLiterals, activityCounters, defaultActivity);
		case MIN:
			return new MinBodyActivityProvider(bodyToLiterals, activityCounters, defaultActivity);
		default:
			assert false : "Unknown BodyActivityType: " + type;
			return null;
		}
	}

}
