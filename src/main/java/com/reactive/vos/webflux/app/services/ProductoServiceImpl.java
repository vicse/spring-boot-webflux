package com.reactive.vos.webflux.app.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.reactive.vos.webflux.app.dao.ProductoDao;
import com.reactive.vos.webflux.app.documents.Producto;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class ProductoServiceImpl implements ProductoService {
	
	@Autowired
	private ProductoDao dao;

	@Override
	public Flux<Producto> findAll() {
		return dao.findAll();
	}

	@Override
	public Mono<Producto> findById(String id) {
		return dao.findById(id);
	}

	@Override
	public Mono<Producto> save(Producto producto) {
		return dao.save(producto);
	}

	@Override
	public Mono<Void> delete(Producto producto) {
		return dao.delete(producto);
	}

	@Override
	public Flux<Producto> findAllWithNameUpperCase() {
		return dao.findAll().map(producto -> {
			producto.setNombre(producto.getNombre().toUpperCase());
			return producto;
		});
	}

	@Override
	public Flux<Producto> findAllWithNameUpperCaseRepeat() {		
		return findAllWithNameUpperCase().repeat(5000);
	}

}
