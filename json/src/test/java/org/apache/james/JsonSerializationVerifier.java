/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.james.json.DTO;
import org.apache.james.json.DTOModule;
import org.apache.james.json.JsonGenericSerializer;

import com.github.fge.lambdas.Throwing;
import com.google.common.collect.ImmutableList;

public class JsonSerializationVerifier<T, U extends DTO> {

    @FunctionalInterface
    public interface RequireJson<T, U extends DTO> {
        JsonSerializationVerifier<T, U> json(String json);
    }

    public static <T, U extends DTO> JsonSerializationVerifier<T, U> dtoModule(DTOModule<T, U> dtoModule) {
        return new JsonSerializationVerifier<>(JsonGenericSerializer
            .forModules(dtoModule)
            .withoutNestedType(),
            ImmutableList.of());
    }

    public static <T, U extends DTO> JsonSerializationVerifier<T, U> serializer(JsonGenericSerializer<T, U> serializer) {
        return new JsonSerializationVerifier<>(serializer, ImmutableList.of());
    }

    private final List<Pair<String, T>> testValues;
    private final JsonGenericSerializer<T, U> serializer;

    private JsonSerializationVerifier( JsonGenericSerializer<T, U> serializer, List<Pair<String, T>> testValues) {
        this.testValues = testValues;
        this.serializer = serializer;
    }

    public RequireJson<T, U> bean(T bean) {
        return json -> new JsonSerializationVerifier<>(
            serializer,
            ImmutableList.<Pair<String, T>>builder()
                .addAll(testValues)
                .add(Pair.of(json, bean))
                .build());
    }

    public JsonSerializationVerifier<T, U> testCase(T bean, String json) {
        return bean(bean).json(json);
    }

    public void verify() throws IOException {
        testValues.forEach(Throwing.<Pair<String, T>>consumer(this::verify).sneakyThrow());
    }

    private void verify(Pair<String, T> testValue) throws IOException {
        assertThatJson(serializer.serialize(testValue.getRight()))
            .describedAs("Serialization test [" + testValue.getRight() + "]")
            .isEqualTo(testValue.getLeft());

        assertThat(serializer.deserialize(testValue.getLeft()))
            .describedAs("Deserialization test [" + testValue.getRight() + "]")
            .isEqualToComparingFieldByFieldRecursively(testValue.getRight());
    }
}