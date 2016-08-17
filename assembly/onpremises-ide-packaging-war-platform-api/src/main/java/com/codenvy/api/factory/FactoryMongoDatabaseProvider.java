/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.api.factory;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoDatabase;

import org.bson.codecs.BinaryCodec;
import org.bson.codecs.configuration.CodecProvider;
import org.bson.codecs.configuration.CodecRegistries;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import java.util.ArrayList;
import java.util.Set;

import static com.mongodb.MongoCredential.createCredential;
import static java.util.Collections.singletonList;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

/**
 * Database provider for factory storage
 * @author Max Shaposhnik
 *
 *
 */
public class FactoryMongoDatabaseProvider { //implements Provider<MongoDatabase> {
//    private final MongoDatabase database;

    @Inject
    public FactoryMongoDatabaseProvider(//@Named("factory.storage.db.url") String dbUrl,
//                                        @Named("factory.storage.db.name") String dbName,
//                                        @Named("factory.storage.db.username") String username,
//                                        @Named("factory.storage.db.password") String password,
//                                        Set<CodecProvider> codecProviders
    ) {
//        final MongoCredential credential = createCredential(username, dbName, password.toCharArray());
//        final MongoClient mongoClient = new MongoClient(new ServerAddress(dbUrl), singletonList(credential));
//        database = mongoClient.getDatabase(dbName)
//                              .withCodecRegistry(fromRegistries(CodecRegistries.fromProviders(new ArrayList<>(codecProviders)),
//                                                                MongoClient.getDefaultCodecRegistry()));
    }

//    @Override
//    public MongoDatabase get() {

//        return database;
//    }
}
