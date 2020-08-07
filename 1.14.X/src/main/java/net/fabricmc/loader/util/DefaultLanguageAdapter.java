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

package net.fabricmc.loader.util;

import net.fabricmc.loader.api.LanguageAdapter;
import net.fabricmc.loader.api.LanguageAdapterException;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.launch.common.FabricLauncherBase;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;

public final class DefaultLanguageAdapter implements LanguageAdapter {
	public static final DefaultLanguageAdapter INSTANCE = new DefaultLanguageAdapter();

	private DefaultLanguageAdapter() {

	}

	@Override
	public <T> T create(ModContainer mod, String value, Class<T> type) throws LanguageAdapterException {
		String[] methodSplit = value.split("::");
		if (methodSplit.length >= 3) {
			throw new LanguageAdapterException("Invalid handle format: " + value);
		}

		Class<?> c;
		try {
			c = Class.forName(methodSplit[0], true, FabricLauncherBase.getLauncher().getTargetClassLoader());
		} catch (ClassNotFoundException e) {
			throw new LanguageAdapterException(e);
		}

		if (methodSplit.length == 1) {
			if (type.isAssignableFrom(c)) {
				try {
					//noinspection unchecked
					return (T) c.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					throw new LanguageAdapterException(e);
				}
			} else {
				throw new LanguageAdapterException("Class " + c.getName() + " cannot be cast to " + type.getName() + "!");
			}
		} else /* length == 2 */ {
			List<Method> methodList = new ArrayList<>();

			for (Method m : c.getDeclaredMethods()) {
				if (!(m.getName().equals(methodSplit[1]))) {
					continue;
				}

				methodList.add(m);
			}

			try {
				Field field = c.getDeclaredField(methodSplit[1]);
				Class<?> fType = field.getType();
				if ((field.getModifiers() & Modifier.STATIC) == 0) {
					throw new LanguageAdapterException("Field " + value + " must be static!");
				}

				if (!methodList.isEmpty()) {
					throw new LanguageAdapterException("Ambiguous " + value + " - refers to both field and method!");
				}

				if (!type.isAssignableFrom(fType)) {
					throw new LanguageAdapterException("Field " + value + " cannot be cast to " + type.getName() + "!");
				}

				//noinspection unchecked
				return (T) field.get(null);
			} catch (NoSuchFieldException e) {
				// ignore
			} catch (IllegalAccessException e) {
				throw new LanguageAdapterException("Field " + value + " cannot be accessed!", e);
			}

			if (!type.isInterface()) {
				throw new LanguageAdapterException("Cannot proxy method " + value + " to non-interface type " + type.getName() + "!");
			}

			if (methodList.isEmpty()) {
				throw new LanguageAdapterException("Could not find " + value + "!");
			} else if (methodList.size() >= 2) {
				throw new LanguageAdapterException("Found multiple method entries of name " + value + "!");
			}

			final Method targetMethod = methodList.get(0);
			Object object = null;

			if ((targetMethod.getModifiers() & Modifier.STATIC) == 0) {
				try {
					object = c.getDeclaredConstructor().newInstance();
				} catch (Exception e) {
					throw new LanguageAdapterException(e);
				}
			}

			final Object targetObject = object;

			//noinspection unchecked
			return (T) Proxy.newProxyInstance(FabricLauncherBase.getLauncher().getTargetClassLoader(), new Class[] { type }, new InvocationHandler() {
				@Override
				public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
					return targetMethod.invoke(targetObject, args);
				}
			});
		}
	}
}