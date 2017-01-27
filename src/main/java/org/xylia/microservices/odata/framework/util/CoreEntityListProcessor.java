/*
 * Licensed to the Apache Software Foundation (ASF) under one

 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.xylia.microservices.odata.framework.util;

import java.lang.reflect.Field;
import java.util.List;

import javax.persistence.Column;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.ValueType;
import org.apache.olingo.commons.core.data.EntityImpl;
import org.apache.olingo.commons.core.data.PropertyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xylia.microservices.odata.edm.framework.providers.entity.BaseEntityProviderProcessorImpl;

/*
 * @author Rajesh Iyer
 */

public class CoreEntityListProcessor {

	private static final Logger logger = LoggerFactory.getLogger(CoreEntityListProcessor.class);

	public static void buildEntities(List<Entity> entityList, List<Object> entities)
			throws IllegalArgumentException, IllegalAccessException {

		for (Object note : entities) {

			EntityImpl entityRowImpl = new EntityImpl();

			for (Field field : note.getClass().getDeclaredFields()) {

				field.setAccessible(true); // Additional line
				Column column = field.getAnnotation(Column.class);

				if (column != null) {

					entityRowImpl
							.addProperty(new PropertyImpl(null, field.getName(), ValueType.PRIMITIVE, field.get(note)));
				}
			}
			entityList.add(entityRowImpl);
		}
	}

	public static void buildPrimitive(List<Entity> entityList, List<Object[]> entities, String[] selections)
			throws IllegalArgumentException, IllegalAccessException {

		for (Object[] entityValues : entities) {

			EntityImpl entityRowImpl = new EntityImpl();

			for (int i = 0; i < entityValues.length; i++) {

				entityRowImpl.addProperty(new PropertyImpl(null, selections[i], ValueType.PRIMITIVE, entityValues[i]));
			}
			entityList.add(entityRowImpl);
		}
	}

	public static void buildSinglePrimitive(List<Entity> entityList, List<Object> entities, String selection)
			throws IllegalArgumentException, IllegalAccessException {

		for (Object entityValue : entities) {

			EntityImpl entityRowImpl = new EntityImpl();
			entityRowImpl.addProperty(new PropertyImpl(null, selection, ValueType.PRIMITIVE, entityValue));
			entityList.add(entityRowImpl);
		}
	}

}
