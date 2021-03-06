/*
 * Copyright 2015 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.fabric8.apiman;

import io.apiman.manager.api.core.IApiKeyGenerator;
import io.apiman.manager.api.core.IIdmStorage;
import io.apiman.manager.api.core.IStorage;
import io.apiman.manager.api.core.IStorageQuery;
import io.apiman.manager.api.core.UuidApiKeyGenerator;
import io.apiman.manager.api.core.logging.ApimanLogger;
import io.apiman.manager.api.core.logging.IApimanLogger;
import io.apiman.manager.api.core.logging.JsonLoggerImpl;
import io.apiman.manager.api.es.EsStorage;
import io.apiman.manager.api.jpa.JpaStorage;
import io.apiman.manager.api.jpa.roles.JpaIdmStorage;
import io.apiman.manager.api.security.ISecurityContext;
import io.apiman.manager.api.security.impl.DefaultSecurityContext;
import io.searchbox.client.JestClient;
import io.searchbox.client.JestClientFactory;
import io.searchbox.client.config.HttpClientConfig;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.New;
import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.InjectionPoint;

/**
 * Attempt to create producer methods for CDI beans.
 *
 * @author eric.wittmann@redhat.com
 */
@ApplicationScoped
public class ManagerApiMicroServiceCdiFactory {

    private static JestClient sESClient;
    private static EsStorage sESStorage;
    
    @Produces @ApimanLogger
    public static IApimanLogger provideLogger(ManagerApiMicroServiceConfig config, InjectionPoint injectionPoint) {
        ApimanLogger logger = injectionPoint.getAnnotated().getAnnotation(ApimanLogger.class);
        Class<?> requestorKlazz = logger.value();
        return new JsonLoggerImpl().createLogger(requestorKlazz);
    }

    @Produces @ApplicationScoped
    public static IStorage provideStorage(ManagerApiMicroServiceConfig config, @New JpaStorage jpaStorage, @New EsStorage esStorage) {
        if ("jpa".equals(config.getStorageType())) { //$NON-NLS-1$
            return jpaStorage;
        } else if ("es".equals(config.getStorageType())) { //$NON-NLS-1$
            return initES(config, esStorage);
        } else {
            throw new RuntimeException("Unknown storage type: " + config.getStorageType()); //$NON-NLS-1$
        }
    }

    @Produces @ApplicationScoped
    public static IStorageQuery provideStorageQuery(ManagerApiMicroServiceConfig config, @New JpaStorage jpaStorage, @New EsStorage esStorage) {
        if ("jpa".equals(config.getStorageType())) { //$NON-NLS-1$
            return jpaStorage;
        } else if ("es".equals(config.getStorageType())) { //$NON-NLS-1$
            return initES(config, esStorage);
        } else {
            throw new RuntimeException("Unknown storage type: " + config.getStorageType()); //$NON-NLS-1$
        }
    }
    
    @Produces @ApplicationScoped
    public static ISecurityContext provideSecurityContext(@New DefaultSecurityContext defaultSC) {
        return defaultSC;
    }

    @Produces @ApplicationScoped
    public static IApiKeyGenerator provideApiKeyGenerator(@New UuidApiKeyGenerator uuidApiKeyGen) {
        return uuidApiKeyGen;
    }

    @Produces @ApplicationScoped
    public static IIdmStorage provideIdmStorage(ManagerApiMicroServiceConfig config, @New JpaIdmStorage jpaIdmStorage, @New EsStorage esStorage) {
        if ("jpa".equals(config.getStorageType())) { //$NON-NLS-1$
            return jpaIdmStorage;
        } else if ("es".equals(config.getStorageType())) { //$NON-NLS-1$
            return initES(config, esStorage);
        } else {
            throw new RuntimeException("Unknown storage type: " + config.getStorageType()); //$NON-NLS-1$
        }
    }

    @Produces @ApplicationScoped
    public static JestClient provideTransportClient(ManagerApiMicroServiceConfig config) {
        if ("es".equals(config.getStorageType())) { //$NON-NLS-1$
            if (sESClient == null) {
                sESClient = createJestClient(config);
            }
        }
        return sESClient;
    }

    /**
     * @param config
     * @return create a new test ES jest client
     */
    private static JestClient createJestClient(ManagerApiMicroServiceConfig config) {
    	String connectionUrl = config.getESProtocol() + "://" + config.getESHost() + ":" + config.getESPort();
        JestClientFactory factory = new JestClientFactory();
        factory.setHttpClientConfig(new HttpClientConfig
               .Builder(connectionUrl)

               .multiThreaded(true)
               .build());
        return factory.getObject();
    }
    /**
     * Initializes the ES storage (if required).
     * @param config
     * @param esStorage
     */
    private static EsStorage initES(ManagerApiMicroServiceConfig config, EsStorage esStorage) {
        if (sESStorage == null) {
            sESStorage = esStorage;
            if (config.isInitializeES()) {
                sESStorage.initialize();
            }
        }
        return sESStorage;
    }

}
