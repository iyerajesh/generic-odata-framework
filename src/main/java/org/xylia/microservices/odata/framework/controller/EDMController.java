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
package org.xylia.microservices.odata.framework.controller;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.olingo.commons.api.http.HttpHeader;
import org.apache.olingo.commons.api.http.HttpMethod;
import org.apache.olingo.server.api.OData;
import org.apache.olingo.server.api.ODataRequest;
import org.apache.olingo.server.api.ODataResponse;
import org.apache.olingo.server.api.ODataTranslatedException;
import org.apache.olingo.server.api.ServiceMetadata;
import org.apache.olingo.server.api.edm.provider.EdmProvider;
import org.apache.olingo.server.api.edmx.EdmxReference;
import org.apache.olingo.server.api.processor.EntityCollectionProcessor;
import org.apache.olingo.server.api.processor.EntityProcessor;
import org.apache.olingo.server.api.processor.PrimitiveProcessor;
import org.apache.olingo.server.api.serializer.SerializerException;
import org.apache.olingo.server.core.ODataHandler;
import org.apache.olingo.server.core.ODataHandlerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.xylia.microservices.odata.framework.exception.EdmException;

// TODO: Auto-generated Javadoc
/**
 * The Class EDMController.
 *
 * @author Rajesh Iyer
 */
@RestController
@RequestMapping("odata")
public class EDMController {

	private static String URI = "odata/";

	/** The split. */
	private int split = 0;
	/** The ctx. */
	@Autowired
	private ApplicationContext ctx;

	/** The edm provider. */
	@Autowired
	private EdmProvider edmProvider;

	/** The entity collection processor. */
	@Autowired
	private EntityCollectionProcessor enityCollectionProcessor;

	/** The entity processor. */
	@Autowired
	private EntityProcessor entityProcessor;
	
	/** The entity processor. */
	@Autowired
	private PrimitiveProcessor primitiveProcessor;
	

	/**
	 * Process.
	 *
	 * @param req
	 *            the req
	 * @return the response entity
	 */
	@RequestMapping(value = "/**")
	public ResponseEntity<String> process(HttpServletRequest req) {

		try {

			OData odata = OData.newInstance();
			ServiceMetadata edm = odata.createServiceMetadata(edmProvider, new ArrayList<EdmxReference>());

			ODataHandler handler = new ODataHandler(odata, edm);
			handler.register(enityCollectionProcessor);
			handler.register(entityProcessor);
			handler.register(primitiveProcessor);

			ODataResponse response = handler.process(createODataRequest(req, split));
			String responseStr = StreamUtils.copyToString(response.getContent(), Charset.defaultCharset());
			MultiValueMap<String, String> headers = new HttpHeaders();
			for (String key : response.getHeaders().keySet()) {
				headers.add(key, response.getHeaders().get(key).toString());
			}
			return new ResponseEntity<String>(responseStr, headers, HttpStatus.valueOf(response.getStatusCode()));
		} catch (Exception ex) {
			throw new EdmException();
		}

	}

	/**
	 * Creates the o data request.
	 *
	 * @param httpRequest
	 *            the http request
	 * @param split
	 *            the split
	 * @return the o data request
	 * @throws ODataTranslatedException
	 *             the o data translated exception
	 */
	private ODataRequest createODataRequest(final HttpServletRequest httpRequest, final int split)
			throws ODataTranslatedException {
		try {
			ODataRequest odRequest = new ODataRequest();

			odRequest.setBody(httpRequest.getInputStream());
			extractHeaders(odRequest, httpRequest);
			extractMethod(odRequest, httpRequest);
			extractUri(odRequest, httpRequest, split);

			return odRequest;
		} catch (final IOException e) {
			throw new SerializerException("An I/O exception occurred.", e,
					SerializerException.MessageKeys.IO_EXCEPTION);
		}
	}

	/**
	 * Extract method.
	 *
	 * @param odRequest
	 *            the od request
	 * @param httpRequest
	 *            the http request
	 * @throws ODataTranslatedException
	 *             the o data translated exception
	 */
	private void extractMethod(final ODataRequest odRequest, final HttpServletRequest httpRequest)
			throws ODataTranslatedException {
		try {
			HttpMethod httpRequestMethod = HttpMethod.valueOf(httpRequest.getMethod());

			if (httpRequestMethod == HttpMethod.POST) {
				String xHttpMethod = httpRequest.getHeader(HttpHeader.X_HTTP_METHOD);
				String xHttpMethodOverride = httpRequest.getHeader(HttpHeader.X_HTTP_METHOD_OVERRIDE);

				if (xHttpMethod == null && xHttpMethodOverride == null) {
					odRequest.setMethod(httpRequestMethod);
				} else if (xHttpMethod == null) {
					odRequest.setMethod(HttpMethod.valueOf(xHttpMethodOverride));
				} else if (xHttpMethodOverride == null) {
					odRequest.setMethod(HttpMethod.valueOf(xHttpMethod));
				} else {
					if (!xHttpMethod.equalsIgnoreCase(xHttpMethodOverride)) {
						throw new ODataHandlerException("Ambiguous X-HTTP-Methods",
								ODataHandlerException.MessageKeys.AMBIGUOUS_XHTTP_METHOD, xHttpMethod,
								xHttpMethodOverride);
					}
					odRequest.setMethod(HttpMethod.valueOf(xHttpMethod));
				}
			} else {
				odRequest.setMethod(httpRequestMethod);
			}
		} catch (IllegalArgumentException e) {
			throw new ODataHandlerException("Invalid HTTP method" + httpRequest.getMethod(),
					ODataHandlerException.MessageKeys.INVALID_HTTP_METHOD, httpRequest.getMethod());
		}
	}

	/**
	 * Extract uri.
	 *
	 * @param odRequest
	 *            the od request
	 * @param httpRequest
	 *            the http request
	 * @param split
	 *            the split
	 */
	private void extractUri(final ODataRequest odRequest, final HttpServletRequest httpRequest, final int split) {
		String rawRequestUri = httpRequest.getRequestURL().toString();

		String rawODataPath;
		if (!"".equals(httpRequest.getServletPath())) {
			int beginIndex;
			beginIndex = rawRequestUri.indexOf(URI);
			beginIndex += URI.length();
			rawODataPath = rawRequestUri.substring(beginIndex);
		} else if (!"".equals(httpRequest.getContextPath())) {
			int beginIndex;
			beginIndex = rawRequestUri.indexOf(httpRequest.getContextPath());
			beginIndex += httpRequest.getContextPath().length();
			rawODataPath = rawRequestUri.substring(beginIndex);
		} else {
			rawODataPath = httpRequest.getRequestURI();
		}

		String rawServiceResolutionUri;
		if (split > 0) {
			rawServiceResolutionUri = rawODataPath;
			for (int i = 0; i < split; i++) {
				int e = rawODataPath.indexOf("/", 1);
				if (-1 == e) {
					rawODataPath = "";
				} else {
					rawODataPath = rawODataPath.substring(e);
				}
			}
			int end = rawServiceResolutionUri.length() - rawODataPath.length();
			rawServiceResolutionUri = rawServiceResolutionUri.substring(0, end);
		} else {
			rawServiceResolutionUri = null;
		}

		String rawBaseUri = rawRequestUri.substring(0, rawRequestUri.length() - rawODataPath.length());

		odRequest.setRawQueryPath(httpRequest.getQueryString());
		odRequest.setRawRequestUri(
				rawRequestUri + (httpRequest.getQueryString() == null ? "" : "?" + httpRequest.getQueryString()));

		odRequest.setRawODataPath(rawODataPath);
		odRequest.setRawBaseUri(rawBaseUri);
		odRequest.setRawServiceResolutionUri(rawServiceResolutionUri);
	}

	/**
	 * Extract headers.
	 *
	 * @param odRequest
	 *            the od request
	 * @param req
	 *            the req
	 */
	private void extractHeaders(final ODataRequest odRequest, final HttpServletRequest req) {
		for (Enumeration<?> headerNames = req.getHeaderNames(); headerNames.hasMoreElements();) {
			String headerName = (String) headerNames.nextElement();
			List<String> headerValues = new ArrayList<String>();
			for (Enumeration<?> headers = req.getHeaders(headerName); headers.hasMoreElements();) {
				String value = (String) headers.nextElement();
				headerValues.add(value);
			}
			odRequest.addHeader(headerName, headerValues);
		}
	}

}
