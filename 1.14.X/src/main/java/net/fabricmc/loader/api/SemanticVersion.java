/*
 * Copyright 2016 FabricMC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*
 *  Copyright (C) 2020  FCWorkgroupMC
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.fabricmc.loader.api;

import java.util.Optional;

import net.fabricmc.loader.util.version.VersionDeserializer;

/**
 * Represents a <a href="https://semver.org/">Sematic Version</a>.
 *
 * <p>Compared to a regular {@link Version}, this type of version receives better support
 * for version comparisons in dependency notations, and is preferred.</p>
 *
 * @see Version
 */
public interface SemanticVersion extends Version, Comparable<SemanticVersion> {
	/**
	 * The value of {@linkplain #getVersionComponent(int) version component} that indicates
	 * a {@linkplain #hasWildcard() wildcard}.
	 */
	int COMPONENT_WILDCARD = Integer.MIN_VALUE;

	/**
	 * Returns the number of components in this version.
	 *
	 * <p>For example, {@code 1.3.x} has 3 components.</p>
	 *
	 * @return the number of components
	 */
	int getVersionComponentCount();

	/**
	 * Returns the version component at {@code pos}.
	 * 
	 * <p>May return {@link #COMPONENT_WILDCARD} to indicate a wildcard component.</p>
	 *
	 * <p>If the pos exceeds the number of components, returns {@link #COMPONENT_WILDCARD}
	 * if the version {@linkplain #hasWildcard() has wildcard}; otherwise returns {@code 0}.</p>
	 *
	 * @param pos the position to check
	 * @return the version component
	 */
	int getVersionComponent(int pos);

	/**
	 * Returns the prerelease key in the version notation.
	 * 
	 * <p>The prerelease key is indicated by a {@code -} before a {@code +} in
	 * the version notation.</p>
	 * 
	 * @return the optional prerelease key
	 */
	Optional<String> getPrereleaseKey();

	/**
	 * Returns the build key in the version notation.
	 * 
	 * <p>The build key is indicated by a {@code +} in the version notation.</p>
	 * 
	 * @return the optional build key
	 */
	Optional<String> getBuildKey();

	/**
	 * Returns if a wildcard notation is present in this version.
	 *
	 * <p>A wildcard notation is a {@code x}, {@code X}, or {@code *} in the version string,
	 * such as {@code 2.5.*}.</p>
	 *
	 * @return whether this version has a wildcard notation
	 */
	boolean hasWildcard();

	/**
	 * {@inheritDoc}
	 * 
	 * <p>Comparison of semantic versions is by the version components, from high to low;
	 * then it falls back to comparing the prerelease notations.</p>
	 *
	 * @param o the other version
	 * @return the result of comparison
	 */
	@Override
	default int compareTo(SemanticVersion o) {
		for (int i = 0; i < Math.max(getVersionComponentCount(), o.getVersionComponentCount()); i++) {
			int first = getVersionComponent(i);
			int second = o.getVersionComponent(i);
			if (first == COMPONENT_WILDCARD || second == COMPONENT_WILDCARD) {
				continue;
			}

			int compare = Integer.compare(first, second);
			if (compare != 0) {
				return compare;
			}
		}

		Optional<String> prereleaseA = getPrereleaseKey();
		Optional<String> prereleaseB = o.getPrereleaseKey();

		if (prereleaseA.isPresent() || prereleaseB.isPresent()) {
			if (prereleaseA.isPresent() && prereleaseB.isPresent()) {
				return prereleaseA.get().compareTo(prereleaseB.get());
			} else if (prereleaseA.isPresent()) {
				return o.hasWildcard() ? 0 : -1;
			} else { // prereleaseB.isPresent()
				return hasWildcard() ? 0 : 1;
			}
		} else {
			return 0;
		}
	}

	/**
	 * Parses a semantic version from a string notation.
	 *
	 * @param s the string notation of the version
	 * @return the parsed version
	 * @throws VersionParsingException if a problem arises during version parsing
	 */
	static SemanticVersion parse(String s) throws VersionParsingException {
		return VersionDeserializer.deserializeSemantic(s);
	}
}
