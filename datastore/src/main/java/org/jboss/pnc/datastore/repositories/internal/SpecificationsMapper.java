/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.datastore.repositories.internal;

import org.jboss.pnc.model.GenericEntity;
import org.jboss.pnc.spi.datastore.repositories.api.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SpecificationsMapper {

    public static <T extends GenericEntity<? extends Serializable>> Specification<T> map(Predicate<T>... predicates) {
        return (root, query, cb) -> {
            List<javax.persistence.criteria.Predicate> jpaPredicates = Stream.of(predicates)
                    .map(predicate -> predicate.apply(root, query, cb)).collect(Collectors.toList());
            return cb.and(jpaPredicates.toArray(new javax.persistence.criteria.Predicate[0]));
        };
    }

}
