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
package org.xylia.microservices.odata.framework.edm;

import java.io.InputStream;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.Entity;
import org.apache.olingo.commons.api.data.EntitySet;
import org.apache.olingo.commons.api.data.Property;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.edm.EdmPrimitiveType;
import org.apache.olingo.commons.api.edm.EdmProperty;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.format.ODataFormat;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.deserializer.DeserializerException;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.PrimitiveSerializerOptions;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriParameter;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.UriResourceProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.xylia.microservices.odata.framework.edm.providers.EntityProvider;

/**
 * @author Rajesh Iyer 
 * Generic PrimitiveProcessor implementation class
 */
@Component
public class GenericPrimitiveProcessor implements PrimitiveProcessor {

	@Autowired
	private ApplicationContext ctx;

	private OData odata;

	private ServiceMetadata serviceMetadata;

	private static final Logger logger = LoggerFactory.getLogger(GenericPrimitiveProcessor.class);

	// our processor is initialized with the OData context object
	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.olingo.server.api.processor.Processor#init(org.apache.olingo
	 * .server.api.OData, org.apache.olingo.server.api.ServiceMetadata)
	 */
	public void init(OData odata, ServiceMetadata serviceMetadata) {
		this.odata = odata;
		this.serviceMetadata = serviceMetadata;
	}

	@Override
	public void readPrimitive(ODataRequest request, ODataResponse response, UriInfo uriInfo, ContentType responseFormat)
			throws ODataApplicationException, SerializerException {

		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		UriResourceProperty uriProperty = (UriResourceProperty) resourcePaths.get(resourcePaths.size() - 1);
		EdmProperty edmProperty = uriProperty.getProperty();
		EdmPrimitiveType edmPropertyType = (EdmPrimitiveType) edmProperty.getType();
		String edmPropertyName = edmProperty.getName();

		logger.debug("The property name being requested:" + edmPropertyName);

		EntitySet entitySet = getEntity(uriInfo);
		Entity entity = entitySet.getEntities().get(0);

		// 2.2. retrieve the property data from the entity
		Property property = entity.getProperty(edmPropertyName);

		ODataFormat format = ODataFormat.fromContentType(responseFormat);
		ODataSerializer serializer = odata.createSerializer(format);

		// 4th: Now serialize the content: transform from the EntitySet object
		// to InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).navOrPropertyPath(edmPropertyName).build();
		PrimitiveSerializerOptions options = PrimitiveSerializerOptions.with().contextURL(contextUrl).build();
		// 3.2. serialize

		InputStream serializedContent = serializer.primitive(edmPropertyType, property, options);

		// 4. configure the response object
		response.setContent(serializedContent);

		// Finally: configure the response object: set the body, headers and
		// status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
	}

	private EntitySet getEntity(UriInfo uriInfo) {

		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0);

		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		EntitySet entitySet = null;

		Map<String, EntityProvider> entityProviders = ctx.getBeansOfType(EntityProvider.class);

		for (String entity : entityProviders.keySet()) {

			EntityProvider entityProvider = entityProviders.get(entity);
			if (entityProvider.getEntityType().getName()

					.equals(edmEntitySet.getEntityType().getName())) {
				try {
					entitySet = entityProvider.getEntity(uriInfo);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				break;
			}
		}
		return entitySet;
	}

	@Override
	public void deletePrimitive(ODataRequest arg0, ODataResponse arg1, UriInfo arg2) throws ODataApplicationException {

		throw new ODataApplicationException("Primitive property delete is not supported yet.",
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

	@Override
	public void updatePrimitive(ODataRequest arg0, ODataResponse arg1, UriInfo arg2, ContentType arg3, ContentType arg4)
			throws ODataApplicationException, DeserializerException, SerializerException {

		throw new ODataApplicationException("Primitive property update is not supported yet.",
				HttpStatusCode.NOT_IMPLEMENTED.getStatusCode(), Locale.ENGLISH);
	}

}
