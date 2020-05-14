package com.reactive.vos.webflux.app.dao;

import com.reactive.vos.webflux.app.documents.Categoria;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface CategoriaDao extends ReactiveMongoRepository<Categoria, String> {

}
