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

package org.xylia.microservices.odata.edm.framework.providers.entity;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Id;

import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntitySet;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveTypeKind;
import org.apache.olingo.commons.api.edm.FullQualifiedName;
import org.apache.olingo.commons.core.data.EntitySetImpl;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.edm.provider.EntityType;
import org.apache.olingo.server.api.edm.provider.Property;
import org.apache.olingo.server.api.edm.provider.PropertyRef;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.CountOption;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.apache.olingo.server.api.uri.queryoption.TopOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.xylia.microservices.odata.framework.persistence.repository.CoreEntityRepositoryProcessorImpl;
import org.xylia.microservices.odata.framework.util.CoreEntityListProcessor;

/**
 * @author Rajesh Iyer
 *
 */
@Component
public abstract class BaseEntityProviderProcessorImpl {

	private static final Logger logger = LoggerFactory.getLogger(BaseEntityProviderProcessorImpl.class);

	@Autowired
	private CoreEntityRepositoryProcessorImpl entityRepository;

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.unum.microservices.odata.edm.providers.EntityProvider#getEntityType()
	 */
	public EntityType getEntityType() {

		/* create the entity types */
		List<Property> properties = new ArrayList<Property>();

		/* create PropertyRef for Key element */
		PropertyRef propertyRef = new PropertyRef();

		String className = getEntityNameSpace() + "." + getProducerEntitySetName();

		try {
			for (Field field : Class.forName(className).getDeclaredFields()) {

				Id idColumn = field.getAnnotation(javax.persistence.Id.class);

				if (idColumn != null)
					propertyRef.setPropertyName(field.getName());

				Column column = field.getAnnotation(Column.class);
				Property property = null;

				// logger.debug("Field name:" + field.getName() + "is of type:"
				// + field.getType());

				if (column != null) {

					if (field.getType().getName().equals("java.lang.String"))

						property = new Property().setName(field.getName())
								.setType(EdmPrimitiveTypeKind.String.getFullQualifiedName());
					else if (field.getType().getName().equals("java.sql.Date"))

						property = new Property().setName(field.getName())
								.setType(EdmPrimitiveTypeKind.Date.getFullQualifiedName());
					else
						property = new Property().setName(field.getName())
								.setType(EdmPrimitiveTypeKind.Double.getFullQualifiedName());

					properties.add(property);
				}
			}
		} catch (SecurityException | ClassNotFoundException e) {
			logger.debug(e.getLocalizedMessage());
		}

		// configure EntityType
		EntityType entityType = new EntityType();
		entityType.setName(getProducerEntityName());
		entityType.setProperties(properties);
		entityType.setKey(Arrays.asList(propertyRef));

		return entityType;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.unum.microservices.odata.edm.providers.EntityProvider#getEntitySet
	 * (org.apache.olingo.server.api.uri.UriInfo)
	 */
	public EntitySet getEntitySet(UriInfo uriInfo) throws IllegalArgumentException, IllegalAccessException {

		int topValue = 0;
		boolean count = false;
		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();

		// get the first entity set
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);

		SelectOption selectOption = uriInfo.getSelectOption();
		logger.debug("$select option:" + selectOption);

		TopOption topOption = uriInfo.getTopOption();
		if (topOption != null)
			topValue = topOption.getValue();

		CountOption countOption = uriInfo.getCountOption();
		if (countOption != null)
			count = countOption.getValue();

		if (selectOption != null) {

			EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
			EdmEntityType edmEntityType = edmEntitySet.getEntityType();

			OData odata = OData.newInstance();
			String selectList = null;

			try {
				selectList = odata.createUriHelper().buildContextURLSelectList(edmEntityType, null, selectOption);

				logger.debug("$select list:" + selectList);

			} catch (SerializerException e) {
				logger.debug(e.getLocalizedMessage());
			}

			if (selectList.contains(",")) {

				/** Multiple selections **/

				String[] selections = selectList.split(",");
				return getData(edmEntitySet, selections, topValue, count);
			} else {
				/** Single selection **/
				return getDataSingleValue(edmEntitySet, selectList, topValue, count);

			}

		} else {

			EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();
			return getData(edmEntitySet, null, topValue, count);
		}
	}

	/**
	 * Helper method for providing some sample data.
	 *
	 * @param edmEntitySet
	 *            for which the data is requested
	 * @return data of requested entity set
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	private EntitySet getData(EdmEntitySet edmEntitySet, String[] selections, int topValue, boolean count)
			throws IllegalArgumentException, IllegalAccessException {

		EntitySet entitySet = new EntitySetImpl();

		List<Entity> entityList = entitySet.getEntities();

		if (selections == null) {

			@SuppressWarnings("unchecked")
			List<Object> entities = (List<Object>) (Object) entityRepository.findAll(getFullQualifiedEntityName());
			CoreEntityListProcessor.buildEntities(entityList, entities);
		} else {
			@SuppressWarnings("unchecked")
			List<Object[]> comments = entityRepository.findWithSelections(getFullQualifiedEntityName(), selections);
			CoreEntityListProcessor.buildPrimitive(entityList, comments, selections);
		}

		return entitySet;
	}

	/**
	 * Helper method for providing some sample data.
	 *
	 * @param edmEntitySet
	 *            for which the data is requested
	 * @return data of requested entity set
	 * @throws IllegalAccessException
	 * @throws IllegalArgumentException
	 */
	private EntitySet getDataSingleValue(EdmEntitySet edmEntitySet, String selections, int topValue, boolean count)
			throws IllegalArgumentException, IllegalAccessException {

		EntitySet entitySet = new EntitySetImpl();

		List<Entity> entityList = entitySet.getEntities();
		logger.debug("Size of the entities:" + entityList.size());

		if (selections == null) {

			@SuppressWarnings("unchecked")
			List<Object> entities = (List<Object>) (Object) entityRepository.findAll(getFullQualifiedEntityName());
			CoreEntityListProcessor.buildEntities(entityList, entities);

		} else {

			@SuppressWarnings("unchecked")
			List<Object> valueString = entityRepository.findWithSelection(getFullQualifiedEntityName(), selections);
			CoreEntityListProcessor.buildSinglePrimitive(entityList, valueString, selections);
		}

		return entitySet;
	}

	public EntitySet getEntity(UriInfo uriInfo) throws IllegalArgumentException, IllegalAccessException {

		logger.debug("Get Entity method getting called");

		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();

		// get the first entity set
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		// get the key being requested from the below call
		List<UriParameter> keyPredicates = uriResourceEntitySet.getKeyPredicates();

		String idKey = keyPredicates.get(0).getText();
		int id = new Integer(idKey).intValue();

		EntitySet entitySet = new EntitySetImpl();

		List<Entity> entityList = entitySet.getEntities();

		/* go the database to retrieve by ID */
		@SuppressWarnings("unchecked")
		List<Object> entities = (List<Object>) (Object) entityRepository.findById(getFullQualifiedEntityName(), id);
		CoreEntityListProcessor.buildEntities(entityList, entities);

		return entitySet;
	}

	public String getEntitySetName() {
		return getProducerEntitySetName();
	}

	public FullQualifiedName getFullyQualifiedName() {
		return getFullyQualifiedEntityName();
	}

	public abstract String getEntityNameSpace();

	public abstract FullQualifiedName getFullyQualifiedEntityName();

	public abstract FullQualifiedName getContainer();

	public abstract String getProducerEntitySetName();

	public abstract String getProducerEntityName();

	public abstract String getFullQualifiedEntityName();

}
