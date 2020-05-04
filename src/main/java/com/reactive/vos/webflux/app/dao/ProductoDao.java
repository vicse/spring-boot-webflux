package com.reactive.vos.webflux.app.dao;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

import com.reactive.vos.webflux.app.documents.Producto;

public interface ProductoDao extends ReactiveMongoRepository<Producto, String>{
	
	

}
