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
import java.util.Map;

import org.apache.olingo.commons.api.data.ContextURL;
import org.apache.olingo.commons.api.data.EntitySet;
import org.apache.olingo.commons.api.edm.EdmEntitySet;
import org.apache.olingo.commons.api.edm.EdmEntityType;
import org.apache.olingo.commons.api.format.ContentType;
import org.apache.olingo.commons.api.format.ODataFormat;
import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpStatusCode;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataApplicationException;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.serializer.EntityCollectionSerializerOptions;
import org.apache.olingo.server.api.serializer.ODataSerializer;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.api.uri.UriInfo;
import org.apache.olingo.server.api.uri.UriResource;
import org.apache.olingo.server.api.uri.UriResourceEntitySet;
import org.apache.olingo.server.api.uri.queryoption.SelectOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.xylia.microservices.odata.framework.edm.providers.EntityProvider;

// TODO: Auto-generated Javadoc
/**
 * The Class ProductEntityCollectionProcessor.
 * @author Rajesh Iyer
 */
@Component
public class GenericEntityCollectionProcessor implements EntityCollectionProcessor {

	@Autowired
	private ApplicationContext ctx;

	/** The odata. */
	private OData odata;

	private static final Logger logger = LoggerFactory.getLogger(GenericEntityCollectionProcessor.class);

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
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.olingo.server.api.processor.EntityCollectionProcessor#
	 * readEntityCollection(org.apache.olingo.server.api.ODataRequest,
	 * org.apache.olingo.server.api.ODataResponse,
	 * org.apache.olingo.server.api.uri.UriInfo,
	 * org.apache.olingo.commons.api.format.ContentType)
	 */
	public void readEntityCollection(ODataRequest request, ODataResponse response, UriInfo uriInfo,
			ContentType responseFormat) throws ODataApplicationException, SerializerException {

		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); 
		SelectOption selectOption = uriInfo.getSelectOption();

		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		EntitySet entitySet = null;

		try {
			entitySet = getData(uriInfo);
		} catch (IllegalArgumentException | IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// 3rd: create a serializer based on the requested format (json)
		ODataFormat format = ODataFormat.fromContentType(responseFormat);
		ODataSerializer serializer = odata.createSerializer(format);

		// 4th: Now serialize the content: transform from the EntitySet object
		// to InputStream
		EdmEntityType edmEntityType = edmEntitySet.getEntityType();
		ContextURL contextUrl = ContextURL.with().entitySet(edmEntitySet).build();

		EntityCollectionSerializerOptions opts = EntityCollectionSerializerOptions.with().contextURL(contextUrl)
				.select(selectOption).build();
		InputStream serializedContent = serializer.entityCollection(edmEntityType, entitySet, opts);

		// Finally: configure the response object: set the body, headers and
		// status code
		response.setContent(serializedContent);
		response.setStatusCode(HttpStatusCode.OK.getStatusCode());
		response.setHeader(HttpHeader.CONTENT_TYPE, responseFormat.toContentTypeString());
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
	private EntitySet getData(UriInfo uriInfo) throws IllegalArgumentException, IllegalAccessException {

		List<UriResource> resourcePaths = uriInfo.getUriResourceParts();
		UriResourceEntitySet uriResourceEntitySet = (UriResourceEntitySet) resourcePaths.get(0); 
		EdmEntitySet edmEntitySet = uriResourceEntitySet.getEntitySet();

		EntitySet entitySet = null;

		/** Get the client EntityProvider beans to process the entities - and process the getEntityType and getEntitySet methods **/
		Map<String, EntityProvider> entityProviders = ctx.getBeansOfType(EntityProvider.class);

		for (String entity : entityProviders.keySet()) {

			EntityProvider entityProvider = entityProviders.get(entity);

			if (entityProvider.getEntityType().getName()

					.equals(edmEntitySet.getEntityType().getName())) {
				entitySet = entityProvider.getEntitySet(uriInfo);
				break;
			}
		}
		return entitySet;
	}

}
