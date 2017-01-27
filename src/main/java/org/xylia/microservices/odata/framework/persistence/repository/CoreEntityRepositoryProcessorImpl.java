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

package org.xylia.microservices.odata.framework.persistence.repository;

import java.util.List;


import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.google.common.collect.Lists;

/*
 * @author Rajesh Iyer
 * Base class for building out custom queries against the entities.
 */

@Component
public class CoreEntityRepositoryProcessorImpl {

	@PersistenceContext
	private EntityManager em;

	private static final Logger logger = LoggerFactory.getLogger(CoreEntityRepositoryProcessorImpl.class);

	/*
	 * Find all objects for the given entity
	 */
	@SuppressWarnings("unchecked")
	public List<?> findAll(String fullQualifiedEntityName) {

		try {

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<?> cq = cb.createQuery(Class.forName(fullQualifiedEntityName));

			@SuppressWarnings("rawtypes")
			Root entityRoot = cq.from(Class.forName(fullQualifiedEntityName));
			CriteriaQuery<?> all = cq.select(entityRoot);
			TypedQuery<?> allQuery = em.createQuery(all);
			return allQuery.getResultList();

		} catch (ClassNotFoundException ce) {
			logger.debug(ce.getLocalizedMessage());
		}
		return null;
	}

	public List<?> findById(String fullQualifiedEntityName, int id) {

		try {
			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<?> criteria = cb.createQuery(Class.forName(fullQualifiedEntityName));

			Root entityRoot;
			entityRoot = criteria.from(Class.forName(fullQualifiedEntityName));
			criteria.select(entityRoot);
			criteria.where(cb.equal(entityRoot.get("id"), id));

			return em.createQuery(criteria).getResultList();

		} catch (ClassNotFoundException ce) {
			logger.debug(ce.getLocalizedMessage());
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public List<Object> findWithSelection(String fullQualifiedEntityName, String selection) {

		try {

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Object> query = cb.createQuery(Object.class);

			@SuppressWarnings("rawtypes")
			Root notesRoot = query.from(Class.forName(fullQualifiedEntityName));

			query.select(notesRoot.get(selection));
			return em.createQuery(query).getResultList();

		} catch (ClassNotFoundException ce) {
			logger.debug(ce.getLocalizedMessage());
		}
		return null;
	}

	public List<Object[]> findWithSelections(String fullQualifiedEntityName, String[] selections) {

		try {

			CriteriaBuilder cb = em.getCriteriaBuilder();
			CriteriaQuery<Object[]> criteria = cb.createQuery(Object[].class);

			@SuppressWarnings("rawtypes")
			Root notesRoot = criteria.from(Class.forName(fullQualifiedEntityName));

			List<Path<?>> predicates = Lists.newArrayList();

			for (int i = 0; i < selections.length; i++) {

				logger.debug("Selection made:" + selections[i]);
				Path<Object> predicate = notesRoot.get(selections[i]);
				predicates.add(predicate);
			}

			criteria.select(cb.array(predicates.toArray(new Path[predicates.size()])));
			return em.createQuery(criteria).getResultList();

		} catch (ClassNotFoundException ce) {
			logger.debug(ce.getLocalizedMessage());
		}
		return null;
	}

}